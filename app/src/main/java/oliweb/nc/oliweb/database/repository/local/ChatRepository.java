package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Observable;
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

    public LiveData<List<ChatEntity>> findByUidAnnonce(String uidAnnonce) {
        Log.d(TAG, "Starting findByUidAnnonce uidAnnonce : " + uidAnnonce);
        return this.chatDao.findByUidAnnonce(uidAnnonce);
    }

    public LiveData<List<ChatEntity>> findByUidUser(String uidSeller) {
        Log.d(TAG, "Starting findByUidUser uidSeller : " + uidSeller);
        return this.chatDao.findByUidUser(uidSeller);
    }

    public Maybe<ChatEntity> findByUidUserAndUidAnnonce(String uidUser, String uidAnnonce) {
        Log.d(TAG, "Starting findByUidUserAndUidAnnonce uidUser : " + uidUser + " uidAnnonce : " + uidAnnonce);
        return this.chatDao.findByUidUserAndUidAnnonce(uidUser, uidAnnonce);
    }

    public Observable<ChatEntity> getAllChatByStatus(List<String> status) {
        Log.d(TAG, "Starting getAllChatByStatus " + status);
        return Observable.create(emitter ->
                this.chatDao.getAllChatByStatus(status)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(chatEntities -> {
                            for (ChatEntity chat : chatEntities) {
                                emitter.onNext(chat);
                            }
                            emitter.onComplete();
                        })
                        .subscribe()
        );
    }

    public LiveData<Integer> countAllChatsByUser(String uidUser) {
        Log.d(TAG, "Starting countAllChatsByUser uidUser : " + uidUser);
        return this.chatDao.countAllFavoritesByUser(uidUser);
    }
}
