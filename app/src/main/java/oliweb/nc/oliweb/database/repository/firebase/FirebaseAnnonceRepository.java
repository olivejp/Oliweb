package oliweb.nc.oliweb.database.repository.firebase;

import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_ANNONCE_REF;

// TODO Faire des tests sur ce repository
public class FirebaseAnnonceRepository {

    private static final String TAG = FirebaseAnnonceRepository.class.getName();
    private DatabaseReference ANNONCE_REF = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_ANNONCE_REF);

    private static FirebaseAnnonceRepository instance;

    private AnnonceRepository annonceRepository;
    private FirebasePhotoRepository firebasePhotoRepository;
    private static final GenericTypeIndicator<HashMap<String, AnnonceDto>> genericClass = new GenericTypeIndicator<HashMap<String, AnnonceDto>>() {
    };

    private FirebaseAnnonceRepository() {
    }

    public static FirebaseAnnonceRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseAnnonceRepository();
        }
        instance.annonceRepository = AnnonceRepository.getInstance(context);
        instance.firebasePhotoRepository = FirebasePhotoRepository.getInstance(context);
        return instance;
    }

    public Query queryByUidUser(String uidUser) {
        Log.d(TAG, "queryByUidUser called with uidUser = " + uidUser);
        return ANNONCE_REF.orderByChild("utilisateur/uuid").equalTo(uidUser);
    }

    /**
     * Retreive all the annonces on the Fb database for the specified User.
     * Then we try to find them in the local DB.
     * If not present the MutableLiveData shouldAskQuestion will receive True.
     *
     * @param uidUtilisateur
     * @param shouldAskQuestion
     */
    public void checkFirebaseRepository(final String uidUtilisateur, @NonNull MutableLiveData<AtomicBoolean> shouldAskQuestion) {
        Log.d(TAG, "checkFirebaseRepository called with uidUtilisateur = " + uidUtilisateur);
        getAllAnnonceFromFbByUidUser(uidUtilisateur)
                .doOnNext(annonceDto -> checkAnnonceLocalRepository(uidUtilisateur, annonceDto, shouldAskQuestion))
                .doOnError(throwable -> Log.e(TAG, throwable.getMessage()))
                .subscribe();
    }


    public void checkAnnonceExistInLocalOrSaveIt(Context context, AnnonceDto annonceDto) {
        annonceRepository.countByUidUtilisateurAndUidAnnonce(annonceDto.getUtilisateur().getUuid(), annonceDto.getUuid())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(integer -> {
                    if (integer == null || integer.equals(0)) {
                        getAnnonceFromFirebaseToLocalDb(context, annonceDto);
                    }
                })
                .subscribe();
    }

    private void getAnnonceFromFirebaseToLocalDb(Context context, final AnnonceDto annonceDto) {
        AnnonceEntity annonceEntity = AnnonceConverter.convertDtoToEntity(annonceDto);
        String uidUtilisateur = annonceDto.getUtilisateur().getUuid();
        annonceEntity.setUuidUtilisateur(uidUtilisateur);
        annonceRepository.save(annonceEntity, dataReturn -> {
            // Now we can save Photos, if any
            if (dataReturn.isSuccessful()) {
                Log.d(TAG, "Annonce has been stored successfully : " + annonceDto.getTitre());
                long idAnnonce = dataReturn.getIds()[0];
                if (annonceDto.getPhotos() != null && !annonceDto.getPhotos().isEmpty()) {
                    for (String photoUrl : annonceDto.getPhotos()) {
                        firebasePhotoRepository.savePhotoFromFirebaseStorageToLocal(context, idAnnonce, photoUrl, uidUtilisateur);
                    }
                }
            } else {
                Log.e(TAG, "Annonce has not been stored correctly UidAnnonce : " + annonceEntity.getUUID() + ", UidUtilisateur : " + uidUtilisateur);
            }
        });
    }

    private void checkAnnonceLocalRepository(String uidUser, AnnonceDto annonceDto, @NonNull MutableLiveData<AtomicBoolean> shouldAskQuestion) {
        Log.d(TAG, "checkAnnonceLocalRepository called with uidUser = " + uidUser);
        annonceRepository.countByUidUtilisateurAndUidAnnonce(uidUser, annonceDto.getUuid())
                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                .doOnSuccess(integer -> {
                    if (integer != null && integer == 0) {
                        shouldAskQuestion.postValue(new AtomicBoolean(true));
                    }
                })
                .doOnError(throwable -> Log.e(TAG, throwable.getMessage()))
                .subscribe();
    }

    private Observable<AnnonceDto> getAllAnnonceFromFbByUidUser(String uidUtilisateur) {
        return Observable.create(emitter -> queryByUidUser(uidUtilisateur).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot == null || dataSnapshot.getValue() == null) {
                    emitter.onError(new RuntimeException("Datasnapshot is empty"));
                    return;
                }

                HashMap<String, AnnonceDto> mapAnnonceSearchDto = dataSnapshot.getValue(genericClass);
                if (mapAnnonceSearchDto == null || mapAnnonceSearchDto.isEmpty()) {
                    emitter.onError(new RuntimeException("MapAnnonceSearchDto is empty"));
                    return;
                }

                for (Map.Entry<String, AnnonceDto> entry : mapAnnonceSearchDto.entrySet()) {
                    emitter.onNext(entry.getValue());
                }
                emitter.onComplete();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled DatabaseError : " + databaseError.getMessage());
                emitter.onError(new RuntimeException(databaseError.getMessage()));
            }
        }));
    }

}
