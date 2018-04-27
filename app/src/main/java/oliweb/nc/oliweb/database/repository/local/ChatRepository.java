package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.ChatDao;
import oliweb.nc.oliweb.database.entity.ChatEntity;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class ChatRepository extends AbstractRepository<ChatEntity> {
    private static final String TAG = ChatRepository.class.getName();
    private static ChatRepository INSTANCE;
    private ChatDao chatDao;
    private int countSuccess;

    private ChatRepository(Context context) {
        super(context);
        this.dao = this.db.getChatDao();
        this.chatDao = (ChatDao) this.dao;
    }

    public Single<List<ChatEntity>> saveWithSingle(List<ChatEntity> chatEntities) {
        return Single.create(emitter -> {
            countSuccess = 0;
            ArrayList<ChatEntity> listResult = new ArrayList<>();
            for (ChatEntity chat : chatEntities) {
                saveWithSingle(chat)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnSuccess(chatSaved -> {
                            listResult.add(chatSaved);
                            countSuccess++;
                            if (countSuccess == chatEntities.size()) {
                                emitter.onSuccess(listResult);
                            }
                        })
                        .doOnError(emitter::onError)
                        .subscribe();
            }
        });
    }

    public static synchronized ChatRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ChatRepository(context);
        }
        return INSTANCE;
    }

    private Single<ChatEntity> insertSingle(ChatEntity chatEntity) {
        return Single.create(e -> {
            try {
                Long[] ids = dao.insert(chatEntity);
                if (ids.length == 1) {
                    findSingleById(chatEntity.getUidChat())
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .doOnSuccess(e::onSuccess)
                            .subscribe();
                } else {
                    e.onError(new RuntimeException("Failed to insert into UtilisateurRepository"));
                }
            } catch (Exception exception) {
                Log.e(TAG, exception.getMessage());
                e.onError(exception);
            }
        });
    }

    /**
     * You have to subscribe to this Single on a background thread
     * because it queries the Database which only accept background queries.
     */
    private Single<ChatEntity> updateSingle(ChatEntity chatEntity) {
        return Single.create(e -> {
            try {
                int updatedCount = dao.update(chatEntity);
                if (updatedCount == 1) {
                    findSingleById(chatEntity.getUidChat())
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .doOnSuccess(e::onSuccess)
                            .subscribe();
                } else {
                    e.onError(new RuntimeException("Failed to update into UtilisateurRepository"));
                }
            } catch (Exception exception) {
                Log.e(TAG, exception.getMessage());
                e.onError(exception);
            }
        });
    }

    public Single<ChatEntity> saveWithSingle(ChatEntity chatEntity) {
        return Single.create(emitter -> existById(chatEntity.getUidChat())
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnError(emitter::onError)
                .doOnSuccess(atomicBoolean -> {
                    if (atomicBoolean.get()) {
                        updateSingle(chatEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .subscribe();
                    } else {
                        insertSingle(chatEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .subscribe();
                    }
                })
                .subscribe());
    }


    private Single<AtomicBoolean> existById(String uidChat) {
        return Single.create(e -> chatDao.countById(uidChat)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(count -> e.onSuccess(new AtomicBoolean(count != null && count == 1)))
                .doOnError(e::onError)
                .subscribe());
    }


    public Single<ChatEntity> findSingleById(String uidChat) {
        return this.chatDao.findSingleById(uidChat);
    }


    public LiveData<ChatEntity> findById(String uidChat) {
        return this.chatDao.findById(uidChat);
    }

    public LiveData<List<ChatEntity>> findByUidAnnonce(String uidAnnonce) {
        return this.chatDao.findByUidAnnonce(uidAnnonce);
    }

    public LiveData<List<ChatEntity>> findByUidUser(String uidSeller) {
        return this.chatDao.findByUidUser(uidSeller);
    }

}
