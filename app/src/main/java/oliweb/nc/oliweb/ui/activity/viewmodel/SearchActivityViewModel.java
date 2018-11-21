package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;

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
import oliweb.nc.oliweb.database.entity.AnnonceFull;
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

    public boolean isLoading() {
        return (loading != null) && loading.getValue().get();
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

    public void updateLoadingStatus(boolean status) {
        if (loading == null) {
            loading = new MutableLiveData<>();
        }
        loading.postValue(new AtomicBoolean(status));
    }

    public boolean isConnected() {
        return NetworkReceiver.checkConnection(getApplication().getApplicationContext());
    }

    public SearchEngine getSearchEngine() {
        return searchEngine;
    }

    public LiveData<List<AnnonceFull>> getFavoritesByUidUser(String uidUtilisateur) {
        return annonceFullRepository.findFavoritesByUidUser(uidUtilisateur);
    }
}
