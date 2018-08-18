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

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.AnnonceService;
import oliweb.nc.oliweb.service.UserService;
import oliweb.nc.oliweb.service.firebase.FirebaseRetrieverService;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.ServicesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;
import oliweb.nc.oliweb.utility.CustomLiveData;
import oliweb.nc.oliweb.utility.LiveDataOnce;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MainActivityViewModel extends AndroidViewModel {

    private static final String TAG = MainActivityViewModel.class.getName();

    private AnnonceWithPhotosRepository annonceWithPhotosRepository;
    private FirebaseAnnonceRepository firebaseAnnonceRespository;
    private FirebaseRetrieverService firebaseRetrieverService;
    private AnnonceRepository annonceRepository;
    private UserRepository userRepository;
    private AnnonceService annonceService;
    private UserService userService;
    private FirebaseUser mFirebaseUser;

    private ChatRepository chatRepository;
    private MutableLiveData<Integer> sorting;
    private MutableLiveData<FirebaseUser> liveDataFirebaseUser;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);

        ContextModule contextModule = new ContextModule(application);
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().contextModule(contextModule).build();
        ServicesComponent componentServices = DaggerServicesComponent.builder().contextModule(contextModule).build();
        FirebaseServicesComponent componentFbServices = DaggerFirebaseServicesComponent.builder().contextModule(contextModule).build();
        FirebaseRepositoriesComponent componentFb = DaggerFirebaseRepositoriesComponent.builder().build();

        annonceWithPhotosRepository = component.getAnnonceWithPhotosRepository();
        annonceRepository = component.getAnnonceRepository();
        userRepository = component.getUserRepository();
        chatRepository = component.getChatRepository();
        firebaseAnnonceRespository = componentFb.getFirebaseAnnonceRepository();
        firebaseRetrieverService = componentFbServices.getFirebaseRetrieverService();
        annonceService = componentServices.getAnnonceService();
        userService = componentServices.getUserService();
    }

    public LiveData<List<AnnoncePhotos>> getFavoritesByUidUser(String uidUtilisateur) {
        return annonceWithPhotosRepository.findFavoritesByUidUser(uidUtilisateur);
    }

    public LiveDataOnce<AtomicBoolean> shouldIAskQuestionToRetrieveData(@Nullable String uidUser) {
        if (uidUser != null) {
            return firebaseRetrieverService.checkFirebaseRepository(uidUser);
        }
        return observer -> observer.onChanged(new AtomicBoolean(false));
    }

    public LiveData<Integer> sortingUpdated() {
        if (sorting == null) {
            sorting = new MutableLiveData<>();
        }
        return sorting;
    }

    public LiveData<UserEntity> findByUid(String uidUser) {
        return userRepository.findByUid(uidUser);
    }

    public void updateSort(int sort) {
        if (sorting == null) {
            sorting = new MutableLiveData<>();
        }
        sorting.postValue(sort);
    }

    public LiveDataOnce<AtomicBoolean> saveUser(FirebaseUser firebaseUser) {
        return userService.saveUserFromFirebase(firebaseUser);
    }

    public LiveData<Integer> countAllAnnoncesByUser(String uid, List<String> statusToAvoid) {
        return annonceRepository.countAllAnnoncesByUser(uid, statusToAvoid);
    }

    public LiveData<Integer> countAllFavoritesByUser(String uid) {
        return annonceRepository.countAllFavoritesByUser(uid);
    }

    public LiveData<Integer> countAllChatsByUser(String uidUser, List<String> status) {
        return chatRepository.countAllChatsByUser(uidUser, status);
    }

    public LiveDataOnce<SearchActivityViewModel.AddRemoveFromFavorite> addOrRemoveFromFavorite(String uidUser, AnnoncePhotos annoncePhotos) {
        return annonceService.addOrRemoveFromFavorite(uidUser, annoncePhotos);
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
        mFirebaseUser = firebaseUser;
        liveDataFirebaseUser.postValue(mFirebaseUser);
    }

    public FirebaseUser getFirebaseUser() {
        return mFirebaseUser;
    }
}
