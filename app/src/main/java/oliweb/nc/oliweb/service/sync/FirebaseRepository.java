package oliweb.nc.oliweb.service.sync;

import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.UtilisateurConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.PhotoRepository;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.utility.MediaUtility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_ANNONCE_REF;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_USER_REF;

/**
 * Created by orlanth23 on 03/03/2018.
 */

public class FirebaseRepository {

    private static final String TAG = FirebaseRepository.class.getName();

    private static FirebaseRepository instance;

    private PhotoRepository photoRepository;
    private AnnonceRepository annonceRepository;
    private DatabaseReference USER_REF = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_USER_REF);
    private static final GenericTypeIndicator<HashMap<String, AnnonceDto>> genericClass = new GenericTypeIndicator<HashMap<String, AnnonceDto>>() {
    };

    private FirebaseRepository() {
    }

    public static FirebaseRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseRepository();
        }
        instance.photoRepository = PhotoRepository.getInstance(context);
        instance.annonceRepository = AnnonceRepository.getInstance(context);
        return instance;
    }

    void synchronize(Context context, String uidUser) {
        Log.d(TAG, "synchronize");
        getAllAnnonceFromFirebaseByUidUser(uidUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    HashMap<String, AnnonceDto> mapAnnonceSearchDto = dataSnapshot.getValue(genericClass);
                    if (mapAnnonceSearchDto != null && !mapAnnonceSearchDto.isEmpty()) {
                        for (Map.Entry<String, AnnonceDto> entry : mapAnnonceSearchDto.entrySet()) {
                            checkAnnonceExistInLocalOrSaveIt(context, entry.getValue());
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

    public Single<AtomicBoolean> insertUserIntoFirebase(FirebaseUser firebaseUser) {
        return Single.create(emitter -> USER_REF.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
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

    private void checkAnnonceExistInLocalOrSaveIt(Context context, AnnonceDto annonceDto) {
        annonceRepository.countByUidUtilisateurAndUidAnnonce(annonceDto.getUtilisateur().getUuid(), annonceDto.getUuid())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(integer -> {
                    if (integer == null || integer.equals(0)) {
                        saveAnnonceFromFirebaseToLocalDb(context, annonceDto);
                    }
                })
                .subscribe();
    }

    private Query getAllAnnonceFromFirebaseByUidUser(String uidUser) {
        Log.d(TAG, "getAllAnnonceFromFirebaseByUidUser called with uidUser = " + uidUser);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_ANNONCE_REF);
        return ref.orderByChild("utilisateur/uuid").equalTo(uidUser);
    }

    private void saveAnnonceFromFirebaseToLocalDb(Context context, final AnnonceDto annonceDto) {
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annonceEntity.setUUID(annonceDto.getUuid());
        annonceEntity.setStatut(StatusRemote.SEND);
        annonceEntity.setTitre(annonceDto.getTitre());
        annonceEntity.setDescription(annonceDto.getDescription());
        annonceEntity.setDatePublication(annonceDto.getDatePublication());
        annonceEntity.setPrix(annonceDto.getPrix());
        annonceEntity.setFavorite(0);
        annonceEntity.setIdCategorie(annonceDto.getCategorie().getId());
        String uidUtilisateur = annonceDto.getUtilisateur().getUuid();
        annonceEntity.setUuidUtilisateur(uidUtilisateur);
        annonceRepository.save(annonceEntity, dataReturn -> {
            // Now we can save Photos, if any
            if (dataReturn.isSuccessful()) {
                Log.d(TAG, "Annonce has been stored successfully : " + annonceDto.getTitre());
                long idAnnonce = dataReturn.getIds()[0];
                if (annonceDto.getPhotos() != null && !annonceDto.getPhotos().isEmpty()) {
                    for (String photoUrl : annonceDto.getPhotos()) {
                        savePhotoFromFirebaseStorageToLocal(context, idAnnonce, photoUrl, uidUtilisateur);
                    }
                }
            } else {
                Log.e(TAG, "Annonce has not been stored correctly UidAnnonce : " + annonceEntity.getUUID() + ", UidUtilisateur : " + uidUtilisateur);
            }
        });
    }

    private void savePhotoFromFirebaseStorageToLocal(Context context, final long idAnnonce, final String urlPhoto, String uidUser) {
        StorageReference httpsReference = FirebaseStorage.getInstance().getReferenceFromUrl(urlPhoto);
        boolean externalStorage = SharedPreferencesHelper.getInstance(context).getUseExternalStorage();
        Pair<Uri, File> pairUriFile = MediaUtility.createNewMediaFileUri(context, externalStorage, MediaUtility.MediaType.IMAGE, uidUser);
        if (pairUriFile != null && pairUriFile.second != null && pairUriFile.first != null) {
            httpsReference.getFile(pairUriFile.second).addOnSuccessListener(taskSnapshot -> {
                // Local temp file has been created
                Log.d(TAG, "Download successful for image : " + urlPhoto + " to URI : " + pairUriFile.first);

                // Save photo to DB now
                PhotoEntity photoEntity = new PhotoEntity();
                photoEntity.setStatut(StatusRemote.SEND);
                photoEntity.setFirebasePath(urlPhoto);
                photoEntity.setUriLocal(pairUriFile.first.toString());
                photoEntity.setIdAnnonce(idAnnonce);

                photoRepository.save(photoEntity, dataReturn -> {
                    if (dataReturn.isSuccessful()) {
                        Log.d(TAG, "Insert into DB successful");
                    } else {
                        Log.d(TAG, "Insert into DB fail");
                    }
                });

            }).addOnFailureListener(exception -> {
                // Handle any errors
                Log.d(TAG, "Download failed for image : " + urlPhoto);
            });
        }
    }

    public void checkFirebaseRepository(final String uidUtilisateur, @NonNull MutableLiveData<AtomicBoolean> shouldAskQuestion) {
        Log.d(TAG, "checkFirebaseRepository called with uidUtilisateur = " + uidUtilisateur);
        getAllAnnonceFromFbByUidUser(uidUtilisateur)
                .doOnNext(annonceDto -> checkAnnonceLocalRepository(uidUtilisateur, annonceDto, shouldAskQuestion))
                .doOnError(throwable -> Log.e(TAG, throwable.getMessage()))
                .subscribe();
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

    public Observable<AnnonceDto> getAllAnnonceFromFbByUidUser(String uidUtilisateur) {
        return Observable.create(emitter -> getAllAnnonceFromFirebaseByUidUser(uidUtilisateur).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot == null || dataSnapshot.getValue() == null) {
                    emitter.onError(new RuntimeException("Datasnapshot is empty"));
                    return;
                }

                HashMap<String, AnnonceDto> mapAnnonceSearchDto = dataSnapshot.getValue(FirebaseRepository.genericClass);
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
