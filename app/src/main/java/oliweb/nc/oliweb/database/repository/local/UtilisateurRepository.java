package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.UtilisateurConverter;
import oliweb.nc.oliweb.database.dao.UtilisateurDao;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class UtilisateurRepository extends AbstractRepository<UtilisateurEntity, Long> {
    private static final String TAG = UtilisateurRepository.class.getName();
    private static UtilisateurRepository instance;
    private UtilisateurDao utilisateurDao;

    private UtilisateurRepository(Context context) {
        super(context);
        this.utilisateurDao = this.db.getUtilisateurDao();
        this.dao = utilisateurDao;
    }

    public static synchronized UtilisateurRepository getInstance(Context context) {
        if (instance == null) {
            instance = new UtilisateurRepository(context);
        }
        return instance;
    }

    public LiveData<UtilisateurEntity> findByUid(String uuidUtilisateur) {
        return this.utilisateurDao.findByUuid(uuidUtilisateur);
    }

    public Maybe<UtilisateurEntity> findSingleByUid(String uuidUtilisateur) {
        return this.utilisateurDao.findSingleByUuid(uuidUtilisateur);
    }

    public Single<AtomicBoolean> existByUid(String uidUser) {
        return Single.create(e -> utilisateurDao.countByUid(uidUser)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(count -> e.onSuccess(new AtomicBoolean(count != null && count == 1)))
                .doOnError(e::onError)
                .subscribe());
    }

    public Single<AtomicBoolean> hasBeenSync(String uidUser) {
        return Single.create(e -> findSingleByUid(uidUser)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(utilisateurEntity -> e.onSuccess(new AtomicBoolean(utilisateurEntity.getStatut().equals(StatusRemote.SEND))))
                .doOnComplete(() -> e.onSuccess(new AtomicBoolean(false)))
                .doOnError(e::onError)
                .subscribe());
    }

    private Maybe<List<UtilisateurEntity>> getAllUtilisateursByStatus(List<String> status) {
        return utilisateurDao.getAllUtilisateursByStatus(status);
    }

    public Observable<UtilisateurEntity> observeAllUtilisateursByStatus(List<String> status) {
        return Observable.create(e ->
                getAllUtilisateursByStatus(status)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(e::onError)
                        .doOnSuccess(utilisateurEntities -> {
                            for (UtilisateurEntity utilisateurEntity : utilisateurEntities) {
                                e.onNext(utilisateurEntity);
                            }
                            e.onComplete();
                        })
                        .subscribe()
        );
    }

    public Single<UtilisateurEntity> registerUser(FirebaseUser firebaseUser) {
        Log.d(TAG, "Starting registerUser firebaseUser : " + firebaseUser);
        return Single.create(emitter ->
                findSingleByUid(firebaseUser.getUid())
                        .doOnError(emitter::onError)
                        .doOnSuccess(utilisateurEntity -> {
                            // Mise à jour de la date de dernière connexion
                            utilisateurEntity.setDateLastConnexion(Utility.getNowInEntityFormat());
                            saveWithSingle(utilisateurEntity)
                                    .doOnError(emitter::onError)
                                    .doOnSuccess(emitter::onSuccess)
                                    .subscribe();
                        })
                        .doOnComplete(() -> {
                            // Création de l'utilisateur
                            UtilisateurEntity utilisateurEntity = UtilisateurConverter.convertFbToEntity(firebaseUser);
                            utilisateurEntity.setTokenDevice(FirebaseInstanceId.getInstance().getToken());
                            utilisateurEntity.setDateCreation(Utility.getNowInEntityFormat());
                            utilisateurEntity.setStatut(StatusRemote.TO_SEND);
                            saveWithSingle(utilisateurEntity)
                                    .doOnError(emitter::onError)
                                    .doOnSuccess(emitter::onSuccess)
                                    .subscribe();
                        })
                        .subscribe()
        );
    }

}
