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
import oliweb.nc.oliweb.database.converter.UtilisateurConverter;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.database.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;

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
        return Single.create(emitter -> {
            UtilisateurEntity entity = UtilisateurConverter.convertFbToEntity(firebaseUser);
            entity.setTokenDevice(FirebaseInstanceId.getInstance().getToken());
            utilisateurRepository.saveWithSingle(entity)
                    .doOnSuccess(saveSuccessful -> {
                        Log.d(TAG, "Utilisateur créé dans la base de données");
                        firebaseUserRespository.insertUserIntoFirebase(entity)
                                .doOnSuccess(insertedIntoFirebase -> {
                                    if (insertedIntoFirebase.get()) {
                                        Log.d(TAG, "Utilisateur créé dans la base Firebase");
                                        emitter.onSuccess(new AtomicBoolean(true));
                                    } else {
                                        Log.d(TAG, "Echec de la création de l'utilisateur dans Firebase");
                                        emitter.onSuccess(new AtomicBoolean(false));
                                    }
                                })
                                .doOnError(emitter::onError).subscribe();
                    })
                    .doOnError(throwable -> Log.e(TAG, throwable.getLocalizedMessage()))
                    .subscribe();
        });
    }

    public LiveData<Integer> countAllAnnoncesByUser(String uid) {
        return this.annonceRepository.countAllAnnoncesByUser(uid);
    }

    public LiveData<Integer> countAllFavoritesByUser(String uid) {
        return this.annonceRepository.countAllFavoritesByUser(uid);
    }
}
