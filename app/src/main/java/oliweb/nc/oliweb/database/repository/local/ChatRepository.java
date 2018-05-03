package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.ChatDao;
import oliweb.nc.oliweb.database.entity.ChatEntity;

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

    public Maybe<ChatEntity> findById(Long idChat) {
        return this.chatDao.findById(idChat);
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
