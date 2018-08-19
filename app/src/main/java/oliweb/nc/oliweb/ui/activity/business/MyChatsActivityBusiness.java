package oliweb.nc.oliweb.ui.activity.business;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.dto.elasticsearch.AnnonceDto;
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

    @Inject
    public MyChatsActivityBusiness(FirebaseAnnonceRepository firebaseAnnonceRepository,
                                   ChatRepository chatRepository) {
        this.firebaseAnnonceRepository = firebaseAnnonceRepository;
        this.chatRepository = chatRepository;
    }

    public LiveDataOnce<AnnonceDto> findLiveFirebaseByUidAnnonce(String uidAnnonce) {
        CustomLiveData<AnnonceDto> customLiveData = new CustomLiveData<>();
        this.firebaseAnnonceRepository.findMaybeByUidAnnonce(uidAnnonce)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> Log.e(TAG, throwable.getLocalizedMessage(), throwable))
                .doOnSuccess(customLiveData::postValue)
                .doOnComplete(() -> customLiveData.postValue(null))
                .subscribe();
        return customLiveData;
    }

    public LiveDataOnce<ChatEntity> findLiveChatByUidUserAndUidAnnonce(String uidUser, String uidAnonce) {
        CustomLiveData<ChatEntity> chatEntityCustomLiveData = new CustomLiveData<>();
        chatRepository.findByUidUserAndUidAnnonce(uidUser, uidAnonce)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
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
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                        .doOnSuccess(emitter::onSuccess)
                        .doOnComplete(() ->
                                chatRepository.singleSave(initializeNewChatEntity(uidUser, annonce))
                                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                        .doOnSuccess(emitter::onSuccess)
                                        .subscribe()
                        )
                        .subscribe()
        );
    }

}
