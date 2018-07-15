package oliweb.nc.oliweb.service.sync;

import android.content.Context;
import android.util.Log;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by orlanth23 on 18/12/2017.
 * <p>
 * This class contains the series of network calls to make to sync local db with firebase
 */
public class CoreSync {
    private static final String TAG = CoreSync.class.getName();

    private static CoreSync instance;

    private FirebaseUserRepository firebaseUserRepository;
    private UtilisateurRepository utilisateurRepository;

    private CoreSync() {
    }

    public static CoreSync getInstance(Context context) {
        if (instance == null) {
            instance = new CoreSync();
            instance.utilisateurRepository = UtilisateurRepository.getInstance(context);
            instance.firebaseUserRepository = FirebaseUserRepository.getInstance();

        }
        return instance;
    }

    void synchronize() {
        Log.d(TAG, "Launch synchronyse");
        Log.d(TAG, "Starting startSendingUser");
        utilisateurRepository
                .getAllUtilisateursByStatus(Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .doOnComplete(() -> Log.d(TAG, "All users to send has been send"))
                .doOnNext(utilisateur ->
                        firebaseUserRepository.insertUserIntoFirebase(utilisateur)
                                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                                .doOnSuccess(success -> {
                                    if (success.get()) {
                                        Log.d(TAG, "insertUserIntoFirebase successfully send user : " + utilisateur);
                                        utilisateur.setStatut(StatusRemote.SEND);
                                        utilisateurRepository.singleSave(utilisateur)
                                                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                                                .subscribe();
                                    }
                                })
                                .subscribe()
                )
                .subscribe();
    }
}
