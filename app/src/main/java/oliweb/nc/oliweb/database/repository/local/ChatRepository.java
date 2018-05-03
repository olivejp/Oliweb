package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.ChatDao;
import oliweb.nc.oliweb.database.entity.ChatEntity;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class ChatRepository extends AbstractRepository<ChatEntity> {
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

    private Maybe<ChatEntity> insertSingle(ChatEntity chatEntity) {
        Long[] ids = dao.insert(chatEntity);
        if (ids.length == 1) {
            return findById(ids[0]);
        } else {
            return Maybe.empty();
        }
    }

    private Maybe<ChatEntity> updateSingle(ChatEntity chatEntity) {
        int updatedCount = dao.update(chatEntity);
        if (updatedCount == 1) {
            return findById(chatEntity.getIdChat());
        } else {
            return Maybe.empty();
        }
    }

    public Single<ChatEntity> saveWithSingle(ChatEntity chatEntity) {
        return Single.create(emitter -> findById(chatEntity.getIdChat())
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnError(emitter::onError)
                .doOnSuccess(chatEntityRead ->
                        updateSingle(chatEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .doOnComplete(() -> Log.e(TAG, "Failed to update"))
                                .subscribe()
                )
                .doOnComplete(() ->
                        insertSingle(chatEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .doOnComplete(() -> Log.e(TAG, "Failed to insert"))
                                .subscribe()
                ).subscribe()
        );
    }

    /**
     * @return Single to be observe. This Single emit an integer which represent the number of chat correctly deleted
     */
    public Single<Integer> deleteAll() {
        return Single.create(e -> chatDao.getAll()
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnError(e::onError)
                .doOnSuccess(chatEntities -> {
                    if (chatEntities != null && !chatEntities.isEmpty()) {
                        e.onSuccess(chatDao.delete(chatEntities));
                    } else {
                        e.onSuccess(0);
                    }
                })
                .subscribe());
    }

    public Single<List<ChatEntity>> getAll() {
        return chatDao.getAll();
    }

    public Maybe<List<ChatEntity>> saveWithSingle(List<ChatEntity> chatEntities) {
        AtomicInteger countSuccess = new AtomicInteger();
        return Maybe.create(emitter -> {
            countSuccess.set(0);
            ArrayList<ChatEntity> listResult = new ArrayList<>();
            for (ChatEntity chat : chatEntities) {
                saveWithSingle(chat)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnSuccess(chatSaved -> {
                            listResult.add(chatSaved);
                            countSuccess.getAndIncrement();
                            if (countSuccess.get() == chatEntities.size()) {
                                emitter.onSuccess(listResult);
                            }
                        })
                        .doOnError(emitter::onError)
                        .subscribe();
            }
        });
    }

    public Maybe<ChatEntity> findById(Long idChat) {
        return this.chatDao.findById(idChat);
    }

    public LiveData<List<ChatEntity>> findByUidAnnonce(String uidAnnonce) {
        return this.chatDao.findByUidAnnonce(uidAnnonce);
    }

    public LiveData<List<ChatEntity>> findByUidUser(String uidSeller) {
        return this.chatDao.findByUidUser(uidSeller);
    }

    public Single<Integer> count() {
        return this.chatDao.count();
    }

    public Maybe<ChatEntity> findByUidUserAndUidAnnonce(String uidUser, String uidAnnonce) {
        return this.chatDao.findByUidUserAndUidAnnonce(uidUser, uidAnnonce);
    }
}
