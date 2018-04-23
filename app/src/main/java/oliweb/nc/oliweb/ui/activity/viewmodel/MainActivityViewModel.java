package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.UtilisateurConverter;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.UtilisateurRepository;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.service.sync.FirebaseSync;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MainActivityViewModel extends AndroidViewModel {

    private static final String TAG = MainActivityViewModel.class.getName();

    public static final String DIALOG_FIREBASE_RETRIEVE = "DIALOG_FIREBASE_RETRIEVE";

    private UtilisateurRepository utilisateurRepository;
    private AnnonceWithPhotosRepository annonceWithPhotosRepository;
    private FirebaseSync utilisateurFbRespository;
    private AnnonceRepository annonceRepository;
    private MutableLiveData<AtomicBoolean> shouldAskQuestion;
    private MutableLiveData<Integer> sorting;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        utilisateurRepository = UtilisateurRepository.getInstance(application.getApplicationContext());
        annonceWithPhotosRepository = AnnonceWithPhotosRepository.getInstance(application.getApplicationContext());
        annonceRepository = AnnonceRepository.getInstance(application.getApplicationContext());
        utilisateurFbRespository = FirebaseSync.getInstance(application.getApplicationContext());
    }

    public LiveData<List<AnnoncePhotos>> getFavoritesByUidUser(String uidUtilisateur) {
        return annonceWithPhotosRepository.findFavoritesByUidUser(uidUtilisateur);
    }

    public void retrieveAnnoncesFromFirebase(final String uidUtilisateur) {
        utilisateurFbRespository.getAllAnnonceFromFbByUidUser(uidUtilisateur)
                .doOnNext(annonceDto -> checkAnnonceExistInLocalDb(uidUtilisateur, annonceDto))
                .doOnError(throwable -> Log.e(TAG, throwable.getMessage()))
                .subscribe();
    }

    private void checkAnnonceExistInLocalDb(String uidUser, AnnonceDto annonceDto) {
        annonceRepository.existByUidUtilisateurAndUidAnnonce(uidUser, annonceDto.getUuid())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(integer -> {
                    if (integer != null && integer > 0) {
                        shouldAskQuestion.postValue(new AtomicBoolean(true));
                    }
                })
                .doOnError(throwable -> Log.e(TAG, throwable.getMessage()))
                .subscribe();
    }

    public LiveData<AtomicBoolean> shouldIAskQuestionToRetreiveData() {
        if (shouldAskQuestion == null) {
            shouldAskQuestion = new MutableLiveData<>();
        }
        shouldAskQuestion.setValue(new AtomicBoolean(false));
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

    public Single<AtomicBoolean> tryToInsertUserIntoLocalDbAndFirebase(FirebaseUser firebaseUser) {
        return Single.create(emitter -> {
            UtilisateurEntity entity = UtilisateurConverter.convertFbToEntity(firebaseUser);
            utilisateurRepository.insertSingle(entity)
                    .doOnSuccess(saveSuccessful -> {
                        if (saveSuccessful.get()) {
                            Log.d(TAG, "Utilisateur créé dans la base de données");
                            utilisateurFbRespository.insertUserIntoFirebase(firebaseUser)
                                    .doOnSuccess(insertedIntoFirebase -> {
                                        if (insertedIntoFirebase.get()) {
                                            emitter.onSuccess(new AtomicBoolean(true));
                                        } else {
                                            emitter.onSuccess(new AtomicBoolean(false));
                                        }
                                    })
                                    .doOnError(emitter::onError).subscribe();
                        }
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
