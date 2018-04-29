package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.UtilisateurConverter;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MainActivityViewModel extends AndroidViewModel {

    private static final String TAG = MainActivityViewModel.class.getName();

    private UtilisateurRepository utilisateurRepository;
    private AnnonceWithPhotosRepository annonceWithPhotosRepository;
    private FirebaseAnnonceRepository firebaseAnnonceRespository;
    private FirebaseUserRepository firebaseUserRespository;
    private AnnonceRepository annonceRepository;
    private MutableLiveData<AtomicBoolean> shouldAskQuestion;
    private MutableLiveData<Integer> sorting;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        utilisateurRepository = UtilisateurRepository.getInstance(application.getApplicationContext());
        annonceWithPhotosRepository = AnnonceWithPhotosRepository.getInstance(application.getApplicationContext());
        annonceRepository = AnnonceRepository.getInstance(application.getApplicationContext());
        firebaseAnnonceRespository = FirebaseAnnonceRepository.getInstance(application.getApplicationContext());
        firebaseUserRespository = FirebaseUserRepository.getInstance();
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

    /**
     * Will try to insert UtilisateurEntity into the local DB and then try to insert into Firebase
     *
     * @param firebaseUser
     * @return
     */
    public Single<AtomicBoolean> registerUser(FirebaseUser firebaseUser) {
        Log.d(TAG, "Starting registerUser firebaseUser : " + firebaseUser);
        return Single.create(emitter ->
                utilisateurRepository.existByUid(firebaseUser.getUid())
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnSuccess(exist -> {
                            Log.d(TAG, "existByUid.doOnSuccess exist : " + exist);
                            if (!exist.get()) {
                                UtilisateurEntity entity = UtilisateurConverter.convertFbToEntity(firebaseUser);
                                entity.setTokenDevice(FirebaseInstanceId.getInstance().getToken());
                                utilisateurRepository.saveWithSingle(entity)
                                        .doOnSuccess(user -> {
                                            Log.d(TAG, "Utilisateur créé dans la base de données");
                                            firebaseUserRespository.insertUserIntoFirebase(entity)
                                                    .doOnSuccess(result -> {
                                                        Log.d(TAG, "insertUserIntoFirebase.doOnSuccess result : " + result);
                                                        if (result.get()) {
                                                            emitter.onSuccess(new AtomicBoolean(true));
                                                        } else {
                                                            emitter.onSuccess(new AtomicBoolean(false));
                                                        }
                                                    })
                                                    .doOnError(e1 -> {
                                                        Log.d(TAG, "insertUserIntoFirebase.doOnError e1 : " + e1.getLocalizedMessage(), e1);
                                                        emitter.onError(e1);
                                                    })
                                                    .subscribe();
                                        })
                                        .doOnError(e -> {
                                            Log.d(TAG, "saveWithSingle.doOnError e : " + e.getLocalizedMessage(), e);
                                            emitter.onError(e);
                                        })
                                        .subscribe();
                            }
                        })
                        .doOnError(emitter::onError)
                        .subscribe()
        );
    }

    public LiveData<Integer> countAllAnnoncesByUser(String uid, List<String> statusToAvoid) {
        return this.annonceRepository.countAllAnnoncesByUser(uid, statusToAvoid);
    }

    public LiveData<Integer> countAllFavoritesByUser(String uid) {
        return this.annonceRepository.countAllFavoritesByUser(uid);
    }
}
