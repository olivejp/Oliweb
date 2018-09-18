package oliweb.nc.oliweb.ui.activity.business;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.reactivex.Scheduler;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.utility.CustomLiveData;
import oliweb.nc.oliweb.utility.LiveDataOnce;

/**
 * Created by orlanth23 on 18/08/2018.
 */
@Singleton
public class MyChatsActivityBusiness {

    private static final String TAG = MyChatsActivityBusiness.class.getCanonicalName();

    private FirebaseAnnonceRepository firebaseAnnonceRepository;
    private ChatRepository chatRepository;
    private Scheduler processScheduler;
    private Scheduler androidScheduler;

    @Inject
    public MyChatsActivityBusiness(FirebaseAnnonceRepository firebaseAnnonceRepository,
                                   ChatRepository chatRepository,
                                   @Named("processScheduler")
                                   Scheduler processScheduler,
                                   @Named("androidScheduler")
                                   Scheduler androidScheduler) {
        this.firebaseAnnonceRepository = firebaseAnnonceRepository;
        this.chatRepository = chatRepository;
        this.processScheduler = processScheduler;
        this.androidScheduler = androidScheduler;
    }

    public LiveDataOnce<AnnonceFirebase> findLiveFirebaseByUidAnnonce(String uidAnnonce) {
        CustomLiveData<AnnonceFirebase> customLiveData = new CustomLiveData<>();
        firebaseAnnonceRepository.findMaybeByUidAnnonce(uidAnnonce)
                .subscribeOn(processScheduler).observeOn(androidScheduler)
                .doOnSuccess(customLiveData::postValue)
                .doOnComplete(() -> customLiveData.postValue(null))
                .doOnError(throwable -> Log.e(TAG, throwable.getLocalizedMessage(), throwable))
                .subscribe();
        return customLiveData;
    }

    public LiveDataOnce<ChatEntity> findLiveChatByUidUserAndUidAnnonce(String uidUser, String uidAnonce) {
        CustomLiveData<ChatEntity> chatEntityCustomLiveData = new CustomLiveData<>();
        chatRepository.findByUidUserAndUidAnnonce(uidUser, uidAnonce)
                .subscribeOn(processScheduler).observeOn(androidScheduler)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(chatEntityCustomLiveData::postValue)
                .doOnComplete(() -> chatEntityCustomLiveData.postValue(null))
                .subscribe();
        return chatEntityCustomLiveData;
    }

    private ChatEntity initializeNewChatEntity(String uidUser, AnnonceEntity annonce) {
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setStatusRemote(StatusRemote.TO_SEND);
        chatEntity.setUidBuyer(uidUser);
        chatEntity.setUidAnnonce(annonce.getUid());
        chatEntity.setUidSeller(annonce.getUidUser());
        chatEntity.setTitreAnnonce(annonce.getTitre());
        return chatEntity;
    }

    /**
     * Search in the local DB if ChatEntity for this uidUser and this uidAnnonce exist otherwise create a new one
     *
     * @return
     */
    public Single<ChatEntity> findOrCreateLiveNewChat(String uidUser, AnnonceEntity annonce) {
        return Single.create(emitter ->
                chatRepository.findByUidUserAndUidAnnonce(uidUser, annonce.getUid())
                        .subscribeOn(processScheduler).observeOn(processScheduler)
                        .doOnSuccess(emitter::onSuccess)
                        .doOnComplete(() ->
                                chatRepository.singleSave(initializeNewChatEntity(uidUser, annonce))
                                        .subscribeOn(processScheduler).observeOn(processScheduler)
                                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                        .doOnSuccess(emitter::onSuccess)
                                        .subscribe()
                        )
                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                        .subscribe()
        );
    }

}
