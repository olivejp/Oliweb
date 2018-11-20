package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.Scheduler;
import oliweb.nc.oliweb.App;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.dto.elasticsearch.ElasticsearchHitsResult;
import oliweb.nc.oliweb.dto.elasticsearch.ElasticsearchResult;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.service.AnnonceService;
import oliweb.nc.oliweb.service.search.SearchEngine;
import oliweb.nc.oliweb.system.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.utility.FirebaseUtilityService;
import oliweb.nc.oliweb.utility.LiveDataOnce;

/**
 * Created by 2761oli on 06/02/2018.
 */
public class SearchActivityViewModel extends AndroidViewModel {

    private static final String TAG = SearchActivityViewModel.class.getName();

    public enum AddRemoveFromFavorite {
        ONE_OF_YOURS,
        REMOVE_SUCCESSFUL,
        REMOVE_FAILED,
        ADD_SUCCESSFUL,
        ADD_FAILED
    }

    @Inject
    AnnonceService annonceService;

    @Inject
    AnnonceFullRepository annonceFullRepository;

    @Inject
    FirebaseUtilityService utilityService;

    @Inject
    SearchEngine searchEngine;

    @Inject
    @Named("processScheduler")
    Scheduler processScheduler;

    @Inject
    @Named("androidScheduler")
    Scheduler androidScheduler;

    private ArrayList<AnnonceFull> listAnnonce;
    private MutableLiveData<ArrayList<AnnonceFull>> liveListAnnonce;
    private MutableLiveData<AtomicBoolean> loading;

    public SearchActivityViewModel(@NonNull Application application) {
        super(application);

        ((App) application).getFirebaseServicesComponent().inject(this);
        ((App) application).getServicesComponent().inject(this);
        ((App) application).getDatabaseRepositoriesComponent().inject(this);

        listAnnonce = new ArrayList<>();
    }

    public LiveData<ArrayList<AnnonceFull>> getLiveListAnnonce() {
        if (liveListAnnonce == null) {
            liveListAnnonce = new MutableLiveData<>();
            listAnnonce = new ArrayList<>();
            liveListAnnonce.setValue(listAnnonce);
        }
        return liveListAnnonce;
    }

    public LiveData<AtomicBoolean> getLoading() {
        if (loading == null) {
            loading = new MutableLiveData<>();
            loading.setValue(new AtomicBoolean(false));
        }
        return loading;
    }

    public LiveDataOnce<AddRemoveFromFavorite> addOrRemoveFromFavorite(String uidCurrentUser, AnnonceFull annoncePhotos) {
        return annonceService.addOrRemoveFromFavorite(uidCurrentUser, annoncePhotos);
    }

    private void updateLoadingStatus(boolean status) {
        if (loading == null) {
            loading = new MutableLiveData<>();
        }
        loading.postValue(new AtomicBoolean(status));
    }

    public boolean isConnected() {
        return NetworkReceiver.checkConnection(getApplication().getApplicationContext());
    }

    public void search(List<String> libellesCategorie, boolean withPhotoOnly, int lowestPrice, int highestPrice, String query, int pagingSize, int from, int tri, int direction) {
        updateLoadingStatus(true);
        if (from == 0) listAnnonce.clear();

        searchEngine.searchMaybe(libellesCategorie, withPhotoOnly, lowestPrice, highestPrice, query, pagingSize, from, tri, direction)
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .doOnError(throwable -> Log.e(TAG, throwable.getLocalizedMessage(), throwable))
                .doOnSuccess(this::doOnSuccess)
                .doOnComplete(() -> updateLoadingStatus(false))
                .subscribe();
    }

    private void doOnSuccess(ElasticsearchHitsResult elasticsearchHitsResult) {
        if (elasticsearchHitsResult != null && elasticsearchHitsResult.getHits() != null && !elasticsearchHitsResult.getHits().isEmpty()) {
            for (ElasticsearchResult<AnnonceFirebase> elasticsearchResult : elasticsearchHitsResult.getHits()) {
                AnnonceFull annonceFull = AnnonceConverter.convertDtoToAnnonceFull(elasticsearchResult.get_source());
                listAnnonce.add(annonceFull);
            }
            liveListAnnonce.postValue(listAnnonce);
        }
        updateLoadingStatus(false);
    }

    public LiveData<List<AnnonceFull>> getFavoritesByUidUser(String uidUtilisateur) {
        return annonceFullRepository.findFavoritesByUidUser(uidUtilisateur);
    }
}
