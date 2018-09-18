package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.App;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.AnnonceService;
import oliweb.nc.oliweb.service.UserService;
import oliweb.nc.oliweb.service.firebase.FirebaseRetrieverService;
import oliweb.nc.oliweb.service.firebase.FirebaseSyncListenerService;
import oliweb.nc.oliweb.service.sync.DatabaseSyncListenerService;
import oliweb.nc.oliweb.system.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.utility.CustomLiveData;
import oliweb.nc.oliweb.utility.LiveDataOnce;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MainActivityViewModel extends AndroidViewModel {

    private static final String TAG = MainActivityViewModel.class.getName();

    @Inject
    AnnonceFullRepository annonceFullRepository;

    @Inject
    FirebaseAnnonceRepository firebaseAnnonceRepository;

    @Inject
    FirebaseRetrieverService firebaseRetrieverService;

    @Inject
    AnnonceRepository annonceRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ChatRepository chatRepository;

    @Inject
    AnnonceService annonceService;

    @Inject
    UserService userService;

    private UserEntity userConnected;
    private boolean isNetworkAvailable;
    private MutableLiveData<Integer> sorting;
    private MutableLiveData<UserEntity> liveUserConnected;
    private MutableLiveData<AtomicBoolean> liveNetworkAvailable;

    private Intent intentLocalDbService;
    private Intent intentFirebaseDbService;

    private Application application;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);

        Log.d(TAG, "Création d'une nouvelle instance de MainActivityViewModel");

        this.application = application;

        intentLocalDbService = new Intent(application, DatabaseSyncListenerService.class);
        intentFirebaseDbService = new Intent(application, FirebaseSyncListenerService.class);

        ((App) application).getDatabaseRepositoriesComponent().inject(this);
        ((App) application).getFirebaseRepositoriesComponent().inject(this);
        ((App) application).getServicesComponent().inject(this);
        ((App) application).getFirebaseServicesComponent().inject(this);
    }

    public LiveData<List<AnnonceFull>> getFavoritesByUidUser(String uidUtilisateur) {
        return annonceFullRepository.findFavoritesByUidUser(uidUtilisateur);
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

    public LiveData<Integer> countAllAnnoncesByUser(String uid, List<String> statusToAvoid) {
        return annonceRepository.countAllAnnoncesByUser(uid, statusToAvoid);
    }

    public LiveData<Integer> countAllFavoritesByUser(String uid) {
        return annonceRepository.countAllFavoritesByUser(uid);
    }

    public LiveData<Integer> countAllChatsByUser(String uidUser, List<String> status) {
        return chatRepository.countAllChatsByUser(uidUser, status);
    }

    public LiveDataOnce<SearchActivityViewModel.AddRemoveFromFavorite> addOrRemoveFromFavorite(String uidUser, AnnonceFull annonceFull) {
        return annonceService.addOrRemoveFromFavorite(uidUser, annonceFull);
    }

    public LiveDataOnce<AnnonceFull> getLiveFromFirebaseByUidAnnonce(String uidAnnonce) {
        CustomLiveData<AnnonceFull> customLiveData = new CustomLiveData<>();
        firebaseAnnonceRepository.findMaybeByUidAnnonce(uidAnnonce)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .map(AnnonceConverter::convertDtoToAnnonceFull)
                .doOnSuccess(customLiveData::postValue)
                .doOnComplete(() -> customLiveData.postValue(null))
                .subscribe();
        return customLiveData;
    }

    public LiveData<UserEntity> getLiveUserConnected() {
        if (liveUserConnected == null) {
            liveUserConnected = new MutableLiveData<>();
        }
        return liveUserConnected;
    }

    private void setUserConnected(UserEntity user) {
        userConnected = user;
        liveUserConnected.postValue(userConnected);
    }

    public UserEntity getUserConnected() {
        return userConnected;
    }

    /**
     * Return true if this is a disconnection
     *
     * @param firebaseUser
     * @return
     */
    public LiveDataOnce<AuthEventType> listenAuthentication(FirebaseUser firebaseUser) {
        return new CustomLiveData<AuthEventType>() {
            @Override
            public void observeOnce(Observer<AuthEventType> observer) {
                super.observeOnce(observer);

                AuthEventType authEventType;
                if (userConnected == null) {
                    authEventType = (firebaseUser == null) ? AuthEventType.NOTHING : AuthEventType.NEW_CONNECTION;
                } else {
                    if (firebaseUser == null) {
                        authEventType = AuthEventType.DISCONNECT;
                    } else {
                        authEventType = (userConnected.getUid().equals(firebaseUser.getUid())) ? AuthEventType.SAME_CONNECTION : AuthEventType.NEW_CONNECTION;
                    }
                }

                switch (authEventType) {
                    case DISCONNECT:
                        setUserConnected(null);
                        stopAllServices();
                        SharedPreferencesHelper.getInstance(application).setUidFirebaseUser(null);
                        break;
                    case NEW_CONNECTION:
                        SharedPreferencesHelper.getInstance(application).setRetrievePreviousAnnonces(true);
                        userService.saveSingleUserFromFirebase(firebaseUser)
                                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                .doOnError(e -> Log.e(TAG, "Saving user failed", e))
                                .doOnSuccess(userSaved -> {
                                    setUserConnected(userSaved);
                                    stopAllServices();
                                    startAllServices(userSaved.getUid());
                                    SharedPreferencesHelper.getInstance(application).setUidFirebaseUser(userSaved.getUid());
                                })
                                .subscribe();
                        break;
                    case SAME_CONNECTION:
                    case NOTHING:
                }

                observer.onChanged(authEventType);
            }
        };
    }

    public void startAllServices(String uidUser) {
        if (NetworkReceiver.checkConnection(application)) {
            intentLocalDbService.putExtra(DatabaseSyncListenerService.CHAT_SYNC_UID_USER, uidUser);
            application.startService(intentLocalDbService);

            intentFirebaseDbService.putExtra(FirebaseSyncListenerService.CHAT_SYNC_UID_USER, uidUser);
            application.startService(intentFirebaseDbService);
        }
    }

    public void stopAllServices() {
        application.stopService(intentLocalDbService);
        application.stopService(intentFirebaseDbService);
    }

    public void setIsNetworkAvailable(boolean available) {
        isNetworkAvailable = available;
        if (liveNetworkAvailable != null) {
            liveNetworkAvailable.postValue(new AtomicBoolean(isNetworkAvailable));
        }
    }

    public LiveData<AtomicBoolean> getIsNetworkAvailable() {
        if (liveNetworkAvailable == null) {
            liveNetworkAvailable = new MutableLiveData<>();
        }
        liveNetworkAvailable.setValue(new AtomicBoolean(isNetworkAvailable));
        return liveNetworkAvailable;
    }

    public enum AuthEventType {
        NOTHING,
        DISCONNECT,
        NEW_CONNECTION,
        SAME_CONNECTION
    }
}
