package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MainActivityViewModel extends AndroidViewModel {

    private static final String TAG = MainActivityViewModel.class.getName();

    private UtilisateurRepository utilisateurRepository;
    private AnnonceWithPhotosRepository annonceWithPhotosRepository;
    private FirebaseAnnonceRepository firebaseAnnonceRespository;
    private FirebasePhotoStorage firebasePhotoStorage;
    private AnnonceRepository annonceRepository;
    private ChatRepository chatRepository;
    private MutableLiveData<AtomicBoolean> shouldAskQuestion;
    private MutableLiveData<Integer> sorting;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        utilisateurRepository = UtilisateurRepository.getInstance(application.getApplicationContext());
        annonceWithPhotosRepository = AnnonceWithPhotosRepository.getInstance(application.getApplicationContext());
        annonceRepository = AnnonceRepository.getInstance(application.getApplicationContext());
        chatRepository = ChatRepository.getInstance(application.getApplicationContext());
        firebaseAnnonceRespository = FirebaseAnnonceRepository.getInstance(application.getApplicationContext());
        firebasePhotoStorage = FirebasePhotoStorage.getInstance(application.getApplicationContext());
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
     * Retourne un Single<AtomicBoolean>
     * onSuccess sera appelé avec la valeur true, si l'utilisateur vient d'être créé
     * onSuccess sera appelé avec la valeur false, si l'utilisateur existait déjà et à correctement été mis à jour
     * onError sera appelé si une erreur s'est produite dans la mise à jour ou à la création
     *
     * @param firebaseUser
     * @return
     */
    public Single<AtomicBoolean> saveUser(FirebaseUser firebaseUser) {
        Log.d(TAG, "Starting saveUser firebaseUser : " + firebaseUser);
        return Single.create(emitter ->
                utilisateurRepository.saveUserFromFirebase(firebaseUser)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(emitter::onSuccess)
                        .subscribe()
        );
    }

    public LiveData<Integer> countAllAnnoncesByUser(String uid, List<String> statusToAvoid) {
        return this.annonceRepository.countAllAnnoncesByUser(uid, statusToAvoid);
    }

    public LiveData<Integer> countAllFavoritesByUser(String uid) {
        return this.annonceRepository.countAllFavoritesByUser(uid);
    }

    public LiveData<Integer> countAllChatsByUser(String uidUser, List<String> status) {
        return this.chatRepository.countAllChatsByUser(uidUser, status);
    }

    /**
     * Vérification que l'annonce n'existe pas deja dans la DB
     * avec le statut Favorite et que je ne suis pas l'auteur de cette annonce.
     *
     * @param uidUser
     * @param annoncePhotos
     * @return
     */
    public Maybe<AnnonceEntity> saveToFavorite(String uidUser, AnnoncePhotos annoncePhotos) {
        Log.d(TAG, "Starting saveAnnonceDtoToLocalDb called with annoncePhotos = " + annoncePhotos.toString());
        return annonceRepository.getAnnonceFavoriteByUidUser(annoncePhotos.getAnnonceEntity().getUidUser(), annoncePhotos.getAnnonceEntity().getUid())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(throwable -> Log.e(TAG, "getAnnonceFavoriteByUidUser.doOnError " + throwable.getMessage()))
                .switchIfEmpty(saveFavoriteAnnonceFromFbToLocalDb(uidUser, getApplication().getApplicationContext(), annoncePhotos).toMaybe());
    }

    /**
     * @param uidUser
     * @param context
     * @param annoncePhotos
     * @return
     */
    private Single<AnnonceEntity> saveFavoriteAnnonceFromFbToLocalDb(String uidUser, Context context, final AnnoncePhotos annoncePhotos) {
        Log.d(TAG, "Starting saveFavoriteAnnonceFromFbToLocalDb called with AnnoncePhotos = " + annoncePhotos.toString());

        AnnonceEntity annonceEntity = annoncePhotos.getAnnonceEntity();
        annonceEntity.setFavorite(1);
        annonceEntity.setUidUserFavorite(uidUser);

        return annonceRepository.singleSave(annonceEntity)
                .doOnError(throwable -> Log.e(TAG, "Annonce has not been stored correctly UidAnnonce : " + annonceEntity.getUid()))
                .doOnSuccess(annonceSaved -> firebasePhotoStorage.saveFromRemoteToLocal(context, annonceSaved.getId(), annoncePhotos.getPhotos()));
    }
}
