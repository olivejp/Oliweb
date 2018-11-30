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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import androidx.annotation.NonNull;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.Scheduler;
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
    private Gson gson;
    private DatabaseReference newRequestRef;
    private DatabaseReference requestReference;
    private GenericTypeIndicator<ElasticsearchHitsResult> genericClassDetail;

    @Inject
    public SearchEngine(FirebaseUtilityService utilityService, @Named("processScheduler") Scheduler processScheduler) {
        this.utilityService = utilityService;
        this.processScheduler = processScheduler;
        this.gson = new Gson();
        this.genericClassDetail = new GenericTypeIndicator<ElasticsearchHitsResult>() {
        };
        this.requestReference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_REQUEST_REF);
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

    public Maybe<ElasticsearchHitsResult> searchMaybe(List<String> libellesCategorie,
                                                      boolean withPhotoOnly,
                                                      int lowestPrice,
                                                      int highestPrice,
                                                      String query,
                                                      int pagingSize,
                                                      int from,
                                                      int tri,
                                                      int direction) {
        return Maybe.create(emitter -> {
            ElasticsearchQueryBuilder builder = getElasticsearchQueryBuilder(libellesCategorie, withPhotoOnly, lowestPrice, highestPrice, query, pagingSize, from, tri, direction);

            String requestJson = gson.toJson(builder.build());
            utilityService.getServerTimestamp()
                    .subscribeOn(processScheduler).observeOn(processScheduler)
                    .doOnError(throwable -> {
                        if (!emitter.isDisposed()) emitter.onError(throwable);
                    })
                    .doOnSuccess(timestamp -> {
                        if (!emitter.isDisposed())
                            doOnSuccessGetServerTimestamp(emitter, requestJson, timestamp);
                    })
                    .subscribe();
        });
    }

    private void doOnSuccessGetServerTimestamp(MaybeEmitter<ElasticsearchHitsResult> emitter, String requestJson, Long timestamp) {
        newRequestRef = requestReference.push();
        newRequestRef.setValue(new ElasticsearchRequest(timestamp, requestJson, 2));
        newRequestRef.addValueEventListener(getValueEventListenerMaybe(emitter));
    }

    private ValueEventListener getValueEventListenerMaybe(MaybeEmitter<ElasticsearchHitsResult> emitter) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ElasticsearchHitsResult elasticsearchResult = null;
                if (dataSnapshot.child(NO_RESULTS).exists()) {
                    newRequestRef.removeEventListener(this);
                    newRequestRef.removeValue();
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(new ElasticsearchHitsResult());
                    }
                } else if (dataSnapshot.child(RESULTS).exists()) {
                    DataSnapshot snapshotResults = dataSnapshot.child(RESULTS);
                    try {
                        elasticsearchResult = snapshotResults.getValue(genericClassDetail);
                    } catch (DatabaseException exception) {
                        Crashlytics.log(1, TAG, exception.getLocalizedMessage());
                        Log.e(TAG, exception.getLocalizedMessage(), exception);
                    } finally {
                        newRequestRef.removeEventListener(this);
                        newRequestRef.removeValue();
                        if (!emitter.isDisposed()) {
                            emitter.onSuccess(elasticsearchResult);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                newRequestRef.removeValue();
                if (!emitter.isDisposed()) {
                    emitter.onError(new RuntimeException(databaseError.getMessage()));
                }
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
}
