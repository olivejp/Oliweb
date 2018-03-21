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
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.DialogInfos;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.UtilisateurRepository;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.service.FirebaseSync;
import oliweb.nc.oliweb.utility.Utility;

import static oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment.TYPE_BOUTON_YESNO;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MainActivityViewModel extends AndroidViewModel {

    private static final String TAG = MainActivityViewModel.class.getName();

    public static final String DIALOG_FIREBASE_RETRIEVE = "DIALOG_FIREBASE_RETRIEVE";

    private UtilisateurRepository utilisateurRepository;
    private AnnonceWithPhotosRepository annonceWithPhotosRepository;

    private MutableLiveData<DialogInfos> liveNotification;

    private MutableLiveData<Integer> sorting;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        utilisateurRepository = UtilisateurRepository.getInstance(application.getApplicationContext());
        annonceWithPhotosRepository = AnnonceWithPhotosRepository.getInstance(application.getApplicationContext());

        liveNotification = new MutableLiveData<>();
        liveNotification.setValue(null);
    }

    public LiveData<DialogInfos> getNotification() {
        return liveNotification;
    }

    public LiveData<List<AnnoncePhotos>> getFavoritesByUidUser(String uidUtilisateur){
        return annonceWithPhotosRepository.findFavoritesByUidUser(uidUtilisateur);
    }

    private void postNewNotification(String message, @DrawableRes int idDrawable, int buttonType, String tag, @Nullable Bundle bundle) {
        DialogInfos dialogInfos = new DialogInfos();
        dialogInfos
                .setMessage(message)
                .setButtonType(buttonType)
                .setIdDrawable(idDrawable)
                .setTag(tag)
                .setBundlePar(bundle);
        liveNotification.postValue(dialogInfos);
    }

    public void createUtilisateur(FirebaseUser user, AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        UtilisateurEntity utilisateurEntity = new UtilisateurEntity();
        utilisateurEntity.setDateCreation(Utility.getNowInEntityFormat());
        utilisateurEntity.setEmail(user.getEmail());
        utilisateurEntity.setUuidUtilisateur(user.getUid());
        utilisateurEntity.setTelephone(user.getPhoneNumber());
        utilisateurRepository.save(utilisateurEntity, onRespositoryPostExecute);
    }

    public void retrieveAnnoncesFromFirebase(final String uidUtilisateur) {
        FirebaseSync firebaseSync = FirebaseSync.getInstance(getApplication().getApplicationContext());
        firebaseSync.getAllAnnonceFromFirebaseByUidUser(uidUtilisateur)
                .addValueEventListener(new ValueEventListener() {
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

    public void updateSort(int sort) {
        if (sorting == null) {
            sorting = new MutableLiveData<>();
        }
        sorting.postValue(sort);
    }

    public LiveData<Integer> sortingUpdated() {
        if (sorting == null) {
            sorting = new MutableLiveData<>();
        }
        return sorting;
    }
}
