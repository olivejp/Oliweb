package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.UtilisateurConverter;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.UtilisateurRepository;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.service.sync.FirebaseSync;
import oliweb.nc.oliweb.ui.DialogInfos;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment.TYPE_BOUTON_YESNO;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_USER_REF;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MainActivityViewModel extends AndroidViewModel {

    private static final String TAG = MainActivityViewModel.class.getName();

    public static final String DIALOG_FIREBASE_RETRIEVE = "DIALOG_FIREBASE_RETRIEVE";

    private UtilisateurRepository utilisateurRepository;
    private AnnonceWithPhotosRepository annonceWithPhotosRepository;
    private AnnonceRepository annonceRepository;

    private MutableLiveData<DialogInfos> retreiveAnnonceNotification;

    private MutableLiveData<Integer> sorting;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        utilisateurRepository = UtilisateurRepository.getInstance(application.getApplicationContext());
        annonceWithPhotosRepository = AnnonceWithPhotosRepository.getInstance(application.getApplicationContext());
        annonceRepository = AnnonceRepository.getInstance(application.getApplicationContext());
    }

    public LiveData<DialogInfos> getRetreiveAnnonceNotification() {
        if (retreiveAnnonceNotification == null) {
            retreiveAnnonceNotification = new MutableLiveData<>();
            retreiveAnnonceNotification.setValue(null);
        }
        return retreiveAnnonceNotification;
    }

    private void postNewNotification(String message, @DrawableRes int idDrawable, int buttonType, String tag, @Nullable Bundle bundle) {
        DialogInfos dialogInfos = new DialogInfos();
        dialogInfos
                .setMessage(message)
                .setButtonType(buttonType)
                .setIdDrawable(idDrawable)
                .setTag(tag)
                .setBundlePar(bundle);
        retreiveAnnonceNotification.postValue(dialogInfos);
    }

    public LiveData<List<AnnoncePhotos>> getFavoritesByUidUser(String uidUtilisateur) {
        return annonceWithPhotosRepository.findFavoritesByUidUser(uidUtilisateur);
    }

    public void retrieveAnnoncesFromFirebase(final String uidUtilisateur) {
        FirebaseSync firebaseSync = FirebaseSync.getInstance(getApplication().getApplicationContext());
        firebaseSync.getAllAnnonceFromFirebaseByUidUser(uidUtilisateur)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                            HashMap<String, AnnonceDto> mapAnnonceSearchDto = dataSnapshot.getValue(FirebaseSync.genericClass);
                            if (mapAnnonceSearchDto != null && !mapAnnonceSearchDto.isEmpty()) {
                                listenForFirebaseAnnonces(uidUtilisateur, mapAnnonceSearchDto);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG, "onCancelled");
                    }
                });
    }

    private void listenForFirebaseAnnonces(final String uidUtilisateur, HashMap<String, AnnonceDto> mapAnnonceSearchDto) {
        FirebaseSync firebaseSync = FirebaseSync.getInstance(getApplication().getApplicationContext());

        AtomicBoolean questionAsked = new AtomicBoolean(false);
        for (Map.Entry<String, AnnonceDto> entry : mapAnnonceSearchDto.entrySet()) {
            if (questionAsked.get()) {
                break;
            }
            firebaseSync.existInLocalByUidUserAndUidAnnonce(uidUtilisateur, entry.getValue().getUuid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(integer -> {
                        if ((integer == null || integer.equals(0)) && !questionAsked.get()) {
                            questionAsked.set(true);
                            String message = "Des annonces vous appartenant ont été trouvées sur le réseau, voulez vous les récupérer sur votre appareil ?";
                            postNewNotification(message, R.drawable.ic_announcement_white_48dp, TYPE_BOUTON_YESNO, DIALOG_FIREBASE_RETRIEVE, null);
                        }
                    });

        }
    }

    public LiveData<Integer> sortingUpdated() {
        if (sorting == null) {
            sorting = new MutableLiveData<>();
        }
        return sorting;
    }

    public void updateSort(int sort) {
        if (sorting == null) {
            sorting = new MutableLiveData<>();
        }
        sorting.postValue(sort);
    }

    public void insertUtilisateur(FirebaseUser firebaseUser, AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        // Sauvegarde dans les préférences, dans le cas d'une déconnexion
        SharedPreferencesHelper.getInstance(getApplication()).setUidFirebaseUser(firebaseUser.getUid());

        // Sauvegarde dans la DB
        insertUserIntoLocalDb(firebaseUser, onRespositoryPostExecute);

        // Sauvegarde dans Firebase
        insertUserIntoFirebase(firebaseUser);
    }

    private void insertUserIntoLocalDb(FirebaseUser firebaseUser, AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        Log.d(TAG, "insertUserIntoLocalDb : Try to insert firebaseUser : " + firebaseUser);
        utilisateurRepository.findSingleByUid(firebaseUser.getUid())
                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                .doOnSuccess(utilisateurEntity -> {
                    Log.d(TAG, "insertUserIntoLocalDb : Found user with uid : " + firebaseUser.getUid());
                    utilisateurRepository.insert(onRespositoryPostExecute, UtilisateurConverter.convertFbToEntity(firebaseUser));
                })
                .doOnError(throwable -> Log.d(TAG, "insertUserIntoLocalDb : Fail to find user with UID  : " + firebaseUser.getUid()))
                .subscribe();
    }

    private void insertUserIntoFirebase(FirebaseUser firebaseUser) {
        FirebaseDatabase.getInstance().getReference(FIREBASE_DB_USER_REF).child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue(UtilisateurFirebase.class) == null) {
                    // Si pas d'utilisateur enregistré dans Firebase, je vais le créer.
                    String token = FirebaseInstanceId.getInstance().getToken();
                    UtilisateurFirebase utilisateurFirebase = UtilisateurConverter.convertFbUserToUtilisateurFirebase(firebaseUser, token);
                    FirebaseDatabase.getInstance().getReference(FIREBASE_DB_USER_REF).child(firebaseUser.getUid()).setValue(utilisateurFirebase)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Utilisateur correctement créé dans Firebase"))
                            .addOnFailureListener(e -> Log.d(TAG, "FAIL : L'utilisateur n'a pas pu être créé dans Firebase"));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("MainActivityViewModel", "onCancelled");
            }
        });
    }

    public LiveData<Integer> countAllAnnoncesByUser(String uid) {
        return this.annonceRepository.countAllAnnoncesByUser(uid);
    }

    public LiveData<Integer> countAllFavoritesByUser(String uid) {
        return this.annonceRepository.countAllFavoritesByUser(uid);
    }
}
