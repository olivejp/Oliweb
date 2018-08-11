package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.ChatDao;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;

/**
 * Created by 2761oli on 29/01/2018.
 */
public class ChatRepository extends AbstractRepository<ChatEntity, Long> {
    private static final String TAG = ChatRepository.class.getName();
    private static ChatRepository instance;
    private ChatDao chatDao;

    private ChatRepository(Context context) {
        super(context);
        this.dao = this.db.getChatDao();
        this.chatDao = (ChatDao) this.dao;
    }

    public static synchronized ChatRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ChatRepository(context);
        }
        return instance;
    }

    public Maybe<ChatEntity> findByUid(String uidChat) {
        return chatDao.findByUid(uidChat);
    }

    public LiveData<List<ChatEntity>> findByUidAnnonceAndStatusNotIn(String uidAnnonce, List<String> status) {
        return this.chatDao.findByUidAnnonce(uidAnnonce, status);
    }

    public LiveData<List<ChatEntity>> findByUidUserAndStatusNotIn(String uidBuyerOrSeller, List<String> status) {
        return this.chatDao.findByUidUserAndStatusNotIn(uidBuyerOrSeller, status);
    }

    public Flowable<List<ChatEntity>> findFlowableByUidUserAndStatusNotIn(String uidBuyerOrSeller, List<String> status) {
        return this.chatDao.findFlowableByUidUserAndStatusNotIn(uidBuyerOrSeller, status);
    }

    public Flowable<ChatEntity> findFlowableByUidUserAndStatusIn(String uidBuyerOrSeller, List<String> status) {
        return this.chatDao.findFlowableByUidUserAndStatusIn(uidBuyerOrSeller, status);
    }

    public Maybe<ChatEntity> findByUidUserAndUidAnnonce(String uidUser, String uidAnnonce) {
        return this.chatDao.findByUidUserAndUidAnnonce(uidUser, uidAnnonce);
    }

    public Observable<ChatEntity> findByStatusIn(List<String> status) {
        return this.chatDao.getAllChatByStatus(status).flattenAsObservable(chatEntities -> chatEntities);
    }

    public LiveData<Integer> countAllChatsByUser(String uidUser, List<String> status) {
        return this.chatDao.countAllFavoritesByUser(uidUser, status);
    }

    public Single<AtomicBoolean> deleteByUid(String uid) {
        return Single.create(emitter ->
                this.chatDao.findByUid(uid)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnSuccess(chatEntity ->
                                delete(dataReturn -> {
                                    if (dataReturn.isSuccessful()) {
                                        emitter.onSuccess(new AtomicBoolean(true));
                                    } else {
                                        emitter.onSuccess(new AtomicBoolean(false));
                                    }
                                }, chatEntity)
                        )
                        .doOnComplete(() -> emitter.onSuccess(new AtomicBoolean(true)))
                        .subscribe()
        );
    }

    public Maybe<ChatEntity> insertIfNotExist(ChatEntity chatEntity) {
        return Maybe.create(emitter ->
                findByUid(chatEntity.getUidChat())
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(chatRead -> emitter.onComplete())
                        .doOnComplete(() -> singleInsert(chatEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .subscribe())
                        .subscribe()
        );
    }

    public Observable<ChatEntity> markToDeleteByUidAnnonceAndUidUser(String uidAnnonce, String uidUser) {
        return chatDao.findByUidUserAndUidAnnonce(uidUser, uidAnnonce)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .toObservable()
                .flatMap(chatEntity -> {
                    chatEntity.setStatusRemote(StatusRemote.TO_DELETE);
                    return this.singleSave(chatEntity)
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .toObservable();
                });
    }

}
