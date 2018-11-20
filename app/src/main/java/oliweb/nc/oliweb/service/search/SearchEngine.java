package oliweb.nc.oliweb.service.search;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import androidx.annotation.NonNull;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import oliweb.nc.oliweb.dto.elasticsearch.ElasticsearchHitsResult;
import oliweb.nc.oliweb.dto.elasticsearch.ElasticsearchRequest;
import oliweb.nc.oliweb.service.misc.ElasticsearchQueryBuilder;
import oliweb.nc.oliweb.utility.FirebaseUtilityService;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_REQUEST_REF;

@Singleton
public class SearchEngine {

    private static final String TAG = SearchEngine.class.getName();
    public static final String NO_RESULTS = "no_results";
    public static final String RESULTS = "results";
    public static final String FIELD_TITRE = "titre";
    public static final String FIELD_TITRE_SORT = "titre.keyword";
    public static final String FILED_PRIX = "prix";
    public static final String FIELD_DATE_PUBLICATION = "datePublication";
    public static final String FIELD_DESCRIPTION = "description";

    public static final int SORT_DATE = 1;
    public static final int SORT_TITLE = 2;
    public static final int SORT_PRICE = 3;

    public static final int ASC = 1;
    public static final int DESC = 2;

    private FirebaseUtilityService utilityService;
    private Scheduler processScheduler;
    private Scheduler androidScheduler;
    private Gson gson;
    private Observable<Long> obsDelay;
    private DatabaseReference newRequestRef;
    private DatabaseReference requestReference;
    private GenericTypeIndicator<ElasticsearchHitsResult> genericClassDetail;

    @Inject
    public SearchEngine(FirebaseUtilityService utilityService,
                        @Named("processScheduler") Scheduler processScheduler,
                        @Named("androidScheduler") Scheduler androidScheduler) {
        this.utilityService = utilityService;
        this.processScheduler = processScheduler;
        this.androidScheduler = androidScheduler;
        this.gson = new Gson();
        this.obsDelay = Observable.timer(30, TimeUnit.SECONDS);
        this.genericClassDetail = new GenericTypeIndicator<ElasticsearchHitsResult>() {
        };
        this.requestReference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_REQUEST_REF);
    }


    public void search(List<String> libellesCategorie,
                       boolean withPhotoOnly,
                       int lowestPrice,
                       int highestPrice,
                       String query,
                       int pagingSize,
                       int from,
                       int tri,
                       int direction,
                       SearchListener listener) {

        // Condition de garde, si pas de listener alors inutile de faire tourner la requête
        if (listener == null) return;

        listener.onBeginSearch();

        ElasticsearchQueryBuilder builder = getElasticsearchQueryBuilder(libellesCategorie, withPhotoOnly, lowestPrice, highestPrice, query, pagingSize, from, tri, direction);

        String requestJson = gson.toJson(builder.build());
        utilityService.getServerTimestamp()
                .subscribeOn(processScheduler).observeOn(androidScheduler)
                .timeout(30, TimeUnit.SECONDS)
                .doOnError(throwable -> {
                    Log.e(TAG, throwable.getLocalizedMessage(), throwable);
                    listener.onFinishSearch(false, null, throwable.getLocalizedMessage());
                })
                .doOnSuccess(timestamp -> doOnSuccess(listener, requestJson, timestamp))
                .subscribe();
    }

    private ElasticsearchQueryBuilder getElasticsearchQueryBuilder(List<String> libellesCategorie, boolean withPhotoOnly, int lowestPrice, int highestPrice, String query, int pagingSize, int from, int tri, int direction) {
        ElasticsearchQueryBuilder builder = initQueryBuilder(pagingSize, direction, from, tri);
        if (query != null) {
            builder.setMultiMatchQuery(Arrays.asList(FIELD_TITRE, FIELD_DESCRIPTION), query);
        }
        if (libellesCategorie != null) {
            builder.setListCategories(libellesCategorie);
        }
        if (lowestPrice >= 0 && highestPrice > 0) {
            builder.setRangePrice(lowestPrice, highestPrice);
        }
        if (withPhotoOnly) {
            builder.setWithPhotoOnly();
        }
        return builder;
    }

    private void doOnSuccess(SearchListener listener, String requestJson, Long timestamp) {
        Disposable delay = obsDelay.observeOn(androidScheduler)
                .doOnNext(aLong -> {
                    newRequestRef.removeValue();
                    listener.onFinishSearch(false, null, "Délai expiré");
                })
                .subscribe();
        newRequestRef = requestReference.push();
        newRequestRef.setValue(new ElasticsearchRequest(timestamp, requestJson, 2));
        newRequestRef.addValueEventListener(getValueEventListener(listener, delay));
    }


    private ValueEventListener getValueEventListener(SearchListener listener, Disposable delay) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(NO_RESULTS).exists()) {
                    newRequestRef.removeEventListener(this);
                    newRequestRef.removeValue();
                    listener.onFinishSearch(true, null, null);
                    delay.dispose();
                } else if (dataSnapshot.child(RESULTS).exists()) {
                    ElasticsearchHitsResult elasticsearchResult = null;
                    DataSnapshot snapshotResults = dataSnapshot.child(RESULTS);
                    try {
                        elasticsearchResult = snapshotResults.getValue(genericClassDetail);
                    } catch (DatabaseException e) {
                        Crashlytics.log(1, TAG, e.getLocalizedMessage());
                        Log.e(TAG, e.getLocalizedMessage(), e);
                    } finally {
                        newRequestRef.removeEventListener(this);
                        newRequestRef.removeValue();
                        listener.onFinishSearch(true, elasticsearchResult, null);
                        delay.dispose();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                delay.dispose();
                newRequestRef.removeValue();
                listener.onFinishSearch(false, null, databaseError.getMessage());
            }
        };
    }

    public Maybe<ElasticsearchHitsResult> searchMaybe(List<String> libellesCategorie,
                                                      boolean withPhotoOnly,
                                                      int lowestPrice,
                                                      int highestPrice,
                                                      String query,
                                                      int pagingSize,
                                                      int from,
                                                      int tri,
                                                      int direction) {
        return Maybe.create(e -> {
            ElasticsearchQueryBuilder builder = getElasticsearchQueryBuilder(libellesCategorie, withPhotoOnly, lowestPrice, highestPrice, query, pagingSize, from, tri, direction);

            String requestJson = gson.toJson(builder.build());
            utilityService.getServerTimestamp()
                    .subscribeOn(processScheduler).observeOn(processScheduler)
                    .timeout(30, TimeUnit.SECONDS)
                    .doOnError(e::onError)
                    .doOnSuccess(timestamp -> doOnSuccessMaybe(e, requestJson, timestamp))
                    .subscribe();
        });
    }

    private void doOnSuccessMaybe(MaybeEmitter<ElasticsearchHitsResult> emitter, String requestJson, Long timestamp) {
        Disposable delay = obsDelay.observeOn(androidScheduler)
                .doOnNext(aLong -> {
                    newRequestRef.removeValue();
                    emitter.onError(new RuntimeException("Délai expiré"));
                })
                .subscribe();
        newRequestRef = requestReference.push();
        newRequestRef.setValue(new ElasticsearchRequest(timestamp, requestJson, 2));
        newRequestRef.addValueEventListener(getValueEventListenerMaybe(emitter, delay));
    }


    private ValueEventListener getValueEventListenerMaybe(MaybeEmitter<ElasticsearchHitsResult> emitter, Disposable delay) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(NO_RESULTS).exists()) {
                    newRequestRef.removeEventListener(this);
                    newRequestRef.removeValue();
                    delay.dispose();
                    emitter.onComplete();
                } else if (dataSnapshot.child(RESULTS).exists()) {
                    ElasticsearchHitsResult elasticsearchResult = null;
                    DataSnapshot snapshotResults = dataSnapshot.child(RESULTS);
                    try {
                        elasticsearchResult = snapshotResults.getValue(genericClassDetail);
                    } catch (DatabaseException e) {
                        Crashlytics.log(1, TAG, e.getLocalizedMessage());
                        Log.e(TAG, e.getLocalizedMessage(), e);
                    } finally {
                        newRequestRef.removeEventListener(this);
                        newRequestRef.removeValue();
                        delay.dispose();
                        emitter.onSuccess(elasticsearchResult);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                delay.dispose();
                newRequestRef.removeValue();
                emitter.onError(new RuntimeException(databaseError.getMessage()));
            }
        };
    }

    private ElasticsearchQueryBuilder initQueryBuilder(int pagingSize, int direction, int from, int tri) {
        String directionStr;
        if (direction == ASC) {
            directionStr = "asc";
        } else {
            directionStr = "desc";
        }

        String field;
        if (tri == SORT_TITLE) {
            field = FIELD_TITRE_SORT;
        } else if (tri == SORT_PRICE) {
            field = FILED_PRIX;
        } else {
            field = FIELD_DATE_PUBLICATION;
        }

        ElasticsearchQueryBuilder builder = new ElasticsearchQueryBuilder();
        builder.setSize(pagingSize)
                .setFrom(from)
                .addSortingFields(field, directionStr);

        return builder;
    }

    public interface SearchListener {
        void onBeginSearch();

        void onFinishSearch(boolean goodFinish, ElasticsearchHitsResult elasticsearchHitsResult, String messageError);
    }
}
