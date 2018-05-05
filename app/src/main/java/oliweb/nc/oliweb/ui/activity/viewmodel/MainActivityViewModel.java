package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.service.sync.SyncService;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MainActivityViewModel extends AndroidViewModel {

    private static final String TAG = MainActivityViewModel.class.getName();

    private UtilisateurRepository utilisateurRepository;
    private AnnonceWithPhotosRepository annonceWithPhotosRepository;
    private FirebaseAnnonceRepository firebaseAnnonceRespository;
    private AnnonceRepository annonceRepository;
    private MutableLiveData<AtomicBoolean> shouldAskQuestion;
    private MutableLiveData<Integer> sorting;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        utilisateurRepository = UtilisateurRepository.getInstance(application.getApplicationContext());
        annonceWithPhotosRepository = AnnonceWithPhotosRepository.getInstance(application.getApplicationContext());
        annonceRepository = AnnonceRepository.getInstance(application.getApplicationContext());
        firebaseAnnonceRespository = FirebaseAnnonceRepository.getInstance(application.getApplicationContext());
    }

    public LiveData<List<AnnoncePhotos>> getFavoritesByUidUser(String uidUtilisateur) {
        return annonceWithPhotosRepository.findFavoritesByUidUser(uidUtilisateur);
    }

    public LiveData<AtomicBoolean> shouldIAskQuestionToRetreiveData(@Nullable String uidUtilisateur) {
        Log.d(TAG, "Starting shouldIAskQuestionToRetreiveData uidUtilisateur : " + uidUtilisateur);
        if (shouldAskQuestion == null) {
            shouldAskQuestion = new MutableLiveData<>();
        }
        shouldAskQuestion.setValue(new AtomicBoolean(false));

        if (uidUtilisateur != null) {
            firebaseAnnonceRespository.checkFirebaseRepository(uidUtilisateur, shouldAskQuestion);
        }

        return shouldAskQuestion;
    }

    public LiveData<Integer> sortingUpdated() {
        if (sorting == null) {
            sorting = new MutableLiveData<>();
        }
        return sorting;
    }

    public void updateSort(int sort) {
        if (sorting == null) {
            sorting = new MutableLiveData<>();
        }
        sorting.postValue(sort);
    }

    public void saveUser(FirebaseUser firebaseUser) {
        Log.d(TAG, "Starting saveUser firebaseUser : " + firebaseUser);
        utilisateurRepository.registerUser(firebaseUser)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(utilisateurEntity -> {
                    if (utilisateurEntity.getStatut().equals(StatusRemote.TO_SEND)) {
                        SyncService.launchSynchroForUser(getApplication());
                    }
                })
                .subscribe();
    }

    public LiveData<Integer> countAllAnnoncesByUser(String uid, List<String> statusToAvoid) {
        return this.annonceRepository.countAllAnnoncesByUser(uid, statusToAvoid);
    }

    public LiveData<Integer> countAllFavoritesByUser(String uid) {
        return this.annonceRepository.countAllFavoritesByUser(uid);
    }
}
