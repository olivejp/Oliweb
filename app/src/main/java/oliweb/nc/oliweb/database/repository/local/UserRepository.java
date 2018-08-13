package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.UtilisateurConverter;
import oliweb.nc.oliweb.database.dao.UtilisateurDao;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class UserRepository extends AbstractRepository<UtilisateurEntity, Long> {
    private static final String TAG = UserRepository.class.getName();
    private static UserRepository instance;
    private UtilisateurDao utilisateurDao;
    private FirebaseUserRepository firebaseUserRepository;

    private UserRepository(Context context) {
        super(context);
        this.utilisateurDao = this.db.getUtilisateurDao();
        this.dao = utilisateurDao;
        this.firebaseUserRepository = FirebaseUserRepository.getInstance();
    }

    public static synchronized UserRepository getInstance(Context context) {
        if (instance == null) {
            instance = new UserRepository(context);
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

    public Flowable<UtilisateurEntity> getAllUtilisateursByStatus(List<String> status) {
        return utilisateurDao.getAllUtilisateursByStatus(status);
    }

    /**
     * Création ou mise à jour de l'utilisateur dans Firebase
     *
     * @param firebaseUser
     * @return AtomicBoolean sera égal à true si c'est une création
     * false si c'est une mise à jour.
     * Si la mise à jour ou la création ont échoué onError sera appelé.
     */
    public Single<AtomicBoolean> saveUserFromFirebase(FirebaseUser firebaseUser) {
        Log.d(TAG, "Starting saveUserFromFirebase firebaseUser : " + firebaseUser);
        return Single.create(emitter ->
                findSingleByUid(firebaseUser.getUid())
                        .doOnError(emitter::onError)
                        .doOnSuccess(utilisateurEntity ->
                                // Mise à jour de la date de dernière connexion et du dernier token de device utilisé
                                this.firebaseUserRepository.getToken()
                                        .doOnSuccess(token -> {
                                            utilisateurEntity.setTokenDevice(token);
                                            utilisateurEntity.setDateLastConnexion(Utility.getNowInEntityFormat());
                                            utilisateurEntity.setStatut(StatusRemote.TO_SEND);
                                            singleSave(utilisateurEntity)
                                                    .doOnError(emitter::onError)
                                                    .doOnSuccess(utilisateurEntity1 -> emitter.onSuccess(new AtomicBoolean(false)))
                                                    .subscribe();
                                        })
                                        .doOnError(emitter::onError)
                                        .subscribe()
                        )
                        .doOnComplete(() -> {
                            // Création de l'utilisateur
                            UtilisateurEntity utilisateurEntity = UtilisateurConverter.convertFbToEntity(firebaseUser);
                            this.firebaseUserRepository.getToken()
                                    .doOnSuccess(token -> {
                                        utilisateurEntity.setTokenDevice(token);
                                        utilisateurEntity.setDateLastConnexion(Utility.getNowInEntityFormat());
                                        utilisateurEntity.setStatut(StatusRemote.TO_SEND);
                                        singleSave(utilisateurEntity)
                                                .doOnError(emitter::onError)
                                                .doOnSuccess(utilisateurEntity1 -> emitter.onSuccess(new AtomicBoolean(true)))
                                                .subscribe();
                                    })
                                    .doOnError(emitter::onError)
                                    .subscribe();
                        })
                        .subscribe()
        );
    }

}
