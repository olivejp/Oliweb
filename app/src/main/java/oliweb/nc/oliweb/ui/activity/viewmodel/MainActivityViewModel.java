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

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.firebase.FirebaseRetrieverService;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;
import oliweb.nc.oliweb.utility.CustomLiveData;
import oliweb.nc.oliweb.utility.LiveDataOnce;
import oliweb.nc.oliweb.utility.MediaUtility;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MainActivityViewModel extends AndroidViewModel {

    private static final String TAG = MainActivityViewModel.class.getName();

    private UserRepository userRepository;
    private AnnonceWithPhotosRepository annonceWithPhotosRepository;
    private FirebaseAnnonceRepository firebaseAnnonceRespository;
    private FirebaseRetrieverService firebaseRetrieverService;
    private AnnonceRepository annonceRepository;
    private ChatRepository chatRepository;
    private PhotoRepository photoRepository;
    private MutableLiveData<AtomicBoolean> shouldAskQuestion;
    private MutableLiveData<Integer> sorting;
    private MutableLiveData<FirebaseUser> liveDataFirebaseUser;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);

        ContextModule contextModule = new ContextModule(application);
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().contextModule(contextModule).build();
        FirebaseRepositoriesComponent componentFb = DaggerFirebaseRepositoriesComponent.builder().contextModule(contextModule).build();
        FirebaseServicesComponent componentFbServices = DaggerFirebaseServicesComponent.builder().contextModule(contextModule).build();

        userRepository = component.getUserRepository();
        annonceWithPhotosRepository = component.getAnnonceWithPhotosRepository();
        annonceRepository = component.getAnnonceRepository();
        photoRepository = component.getPhotoRepository();
        chatRepository = component.getChatRepository();
        firebaseAnnonceRespository = componentFb.getFirebaseAnnonceRepository();
        firebaseRetrieverService = componentFbServices.getFirebaseRetrieverService();
    }

    public LiveData<List<AnnoncePhotos>> getFavoritesByUidUser(String uidUtilisateur) {
        return annonceWithPhotosRepository.findFavoritesByUidUser(uidUtilisateur);
    }

    public LiveData<AtomicBoolean> shouldIAskQuestionToRetrieveData(@Nullable String uidUser) {
        Log.d(TAG, "Starting shouldIAskQuestionToRetrieveData uidUser : " + uidUser);
        if (shouldAskQuestion == null) {
            shouldAskQuestion = new MutableLiveData<>();
        }
        shouldAskQuestion.setValue(new AtomicBoolean(false));
        if (uidUser != null) {
            firebaseRetrieverService.checkFirebaseRepository(uidUser, shouldAskQuestion);
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
    public LiveDataOnce<AtomicBoolean> saveUser(FirebaseUser firebaseUser) {
        CustomLiveData<AtomicBoolean> customLiveData = new CustomLiveData<>();
        userRepository.saveUserFromFirebase(firebaseUser)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(customLiveData::postValue)
                .subscribe();
        return customLiveData;
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

    public LiveDataOnce<AtomicBoolean> removeFromFavoriteLive(String uidUser, AnnoncePhotos annoncePhotos) {
        CustomLiveData<AtomicBoolean> liveDataRemoveFavorite = new CustomLiveData<>();
        removeFromFavorite(uidUser, annoncePhotos)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(liveDataRemoveFavorite::postValue)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
        return liveDataRemoveFavorite;
    }

    private Single<AtomicBoolean> removeFromFavorite(String uidUser, AnnoncePhotos annoncePhotos) {
        return Single.create(emitter ->
                annonceWithPhotosRepository.findFavoriteAnnonceByUidAnnonce(uidUser, annoncePhotos.getAnnonceEntity().getUid())
                        .doOnError(emitter::onError)
                        .doOnSuccess(annoncePhotos1 -> {
                                    if (annoncePhotos.getPhotos() != null && !annoncePhotos.getPhotos().isEmpty()) {
                                        for (PhotoEntity photo : annoncePhotos1.getPhotos()) {
                                            MediaUtility.deletePhotoFromDevice(getApplication().getContentResolver(), photo);
                                            photoRepository.delete(photo);
                                        }
                                    }
                                    annonceRepository.removeFromFavorite(uidUser, annoncePhotos1);
                                    emitter.onSuccess(new AtomicBoolean(true));
                                }
                        )
                        .subscribe()
        );
    }

    /**
     * Vérification que l'annonce n'existe pas deja dans la DB
     * avec le statut Favorite et que je ne suis pas l'auteur de cette annonce.
     *
     * @param uidUser
     * @param annoncePhotos
     * @return
     */
    public LiveDataOnce<AnnonceEntity> saveToFavorite(String uidUser, AnnoncePhotos annoncePhotos) {
        Log.d(TAG, "Starting saveToFavorite called with annoncePhotos = " + annoncePhotos.toString());
        CustomLiveData<AnnonceEntity> liveDataSaveFavorite = new CustomLiveData<>();
        annonceRepository.saveToFavorite(getApplication().getApplicationContext(), uidUser, annoncePhotos)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(liveDataSaveFavorite::postValue)
                .subscribe();
        return liveDataSaveFavorite;
    }

    public LiveDataOnce<AnnoncePhotos> getLiveFromFirebaseByUidAnnonce(String uidAnnonce) {
        CustomLiveData<AnnoncePhotos> customLiveData = new CustomLiveData<>();
        firebaseAnnonceRespository.findMaybeByUidAnnonce(uidAnnonce)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .map(AnnonceConverter::convertDtoToAnnoncePhotos)
                .doOnSuccess(customLiveData::postValue)
                .doOnComplete(() -> customLiveData.postValue(null))
                .subscribe();
        return customLiveData;
    }

    public LiveData<FirebaseUser> getLiveDataFirebaseUser() {
        if (liveDataFirebaseUser == null) {
            liveDataFirebaseUser = new MutableLiveData<>();
        }
        return liveDataFirebaseUser;
    }

    public void setFirebaseUser(FirebaseUser firebaseUser) {
        liveDataFirebaseUser.postValue(firebaseUser);
    }
}
