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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.UtilisateurConverter;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.UtilisateurRepository;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.service.sync.FirebaseSync;
import oliweb.nc.oliweb.ui.DialogInfos;

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
    private DatabaseReference USER_REF = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_USER_REF);

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

    // TODO refacto de cette méthode pour renvoyer un Single<AtomicBoolean> dans le cas où on trouve une annonce qui existe dans Fb et pas dans la db locale.
    public void retrieveAnnoncesFromFirebase(final String uidUtilisateur) {
        FirebaseSync firebaseSync = FirebaseSync.getInstance(getApplication().getApplicationContext());
        firebaseSync.getAllAnnonceFromFirebaseByUidUser(uidUtilisateur).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    HashMap<String, AnnonceDto> mapAnnonceSearchDto = dataSnapshot.getValue(FirebaseSync.genericClass);
                    if (mapAnnonceSearchDto != null && !mapAnnonceSearchDto.isEmpty()) {

                        AtomicBoolean questionAsked = new AtomicBoolean(false);
                        for (Map.Entry<String, AnnonceDto> entry : mapAnnonceSearchDto.entrySet()) {
                            if (questionAsked.get()) {
                                break;
                            }
                            firebaseSync.existInLocalByUidUserAndUidAnnonce(uidUtilisateur, entry.getValue().getUuid()).subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                    .doOnSuccess(integer -> {
                                        if ((integer == null || integer.equals(0)) && !questionAsked.get()) {
                                            questionAsked.set(true);
                                            String message = "Des annonces vous appartenant ont été trouvées sur le réseau, voulez vous les récupérer sur votre appareil ?";
                                            postNewNotification(message, R.drawable.ic_announcement_white_48dp, TYPE_BOUTON_YESNO, DIALOG_FIREBASE_RETRIEVE, null);
                                        }
                                    })
                                    .subscribe();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled");
            }
        });
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

    public Single<AtomicBoolean> tryToInsertUserIntoLocalDbAndFirebase(FirebaseUser firebaseUser) {
        return Single.create(emitter -> existByUid(firebaseUser.getUid())
                .doOnSuccess(userExist -> {
                    if (!userExist.get()) {
                        UtilisateurEntity entity = UtilisateurConverter.convertFbToEntity(firebaseUser);
                        utilisateurRepository.save(entity)
                                .doOnSuccess(saveSuccessful -> {
                                    if (saveSuccessful.get()) {
                                        insertUserIntoFirebase(firebaseUser)
                                                .doOnSuccess(insertedIntoFirebase -> {
                                                    if (insertedIntoFirebase.get()) {
                                                        emitter.onSuccess(new AtomicBoolean(true));
                                                    } else {
                                                        emitter.onSuccess(new AtomicBoolean(false));
                                                    }
                                                })
                                                .doOnError(emitter::onError).subscribe();
                                    }
                                })
                                .doOnError(throwable -> Log.e(TAG, throwable.getLocalizedMessage()))
                                .subscribe();
                    }
                })
                .doOnError(throwable -> Log.e(TAG, throwable.getLocalizedMessage()))
                .subscribe());
    }

    private Single<AtomicBoolean> existByUid(String uidUser) {
        return utilisateurRepository.existByUid(uidUser);
    }

    private Single<AtomicBoolean> insertUserIntoFirebase(FirebaseUser firebaseUser) {
        return Single.create(emitter -> USER_REF.child(firebaseUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null && dataSnapshot.getValue(UtilisateurFirebase.class) == null) {
                            String token = FirebaseInstanceId.getInstance().getToken();
                            UtilisateurFirebase utilisateurFirebase = UtilisateurConverter.convertFbUserToUtilisateurFirebase(firebaseUser, token);
                            USER_REF.child(firebaseUser.getUid()).setValue(utilisateurFirebase)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Utilisateur correctement créé dans Firebase");
                                        emitter.onSuccess(new AtomicBoolean(true));
                                    })
                                    .addOnFailureListener(exception -> {
                                        Log.d(TAG, "FAIL : L'utilisateur n'a pas pu être créé dans Firebase");
                                        emitter.onError(exception);
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("MainActivityViewModel", "onCancelled");
                        emitter.onError(new RuntimeException(databaseError.getMessage()));
                    }
                }));
    }

    public LiveData<Integer> countAllAnnoncesByUser(String uid) {
        return this.annonceRepository.countAllAnnoncesByUser(uid);
    }

    public LiveData<Integer> countAllFavoritesByUser(String uid) {
        return this.annonceRepository.countAllFavoritesByUser(uid);
    }
}
