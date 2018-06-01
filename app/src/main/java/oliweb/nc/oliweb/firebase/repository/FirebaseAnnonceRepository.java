package oliweb.nc.oliweb.firebase.repository;

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

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_ANNONCE_REF;

public class FirebaseAnnonceRepository {

    private static final String TAG = FirebaseAnnonceRepository.class.getName();
    private DatabaseReference annonceRef = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_ANNONCE_REF);

    private static FirebaseAnnonceRepository instance;

    private AnnonceRepository annonceRepository;
    private FirebasePhotoStorage firebasePhotoStorage;
    private static final GenericTypeIndicator<HashMap<String, AnnonceDto>> genericClass = new GenericTypeIndicator<HashMap<String, AnnonceDto>>() {
    };

    private FirebaseAnnonceRepository() {
    }

    public static FirebaseAnnonceRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseAnnonceRepository();
        }
        instance.annonceRepository = AnnonceRepository.getInstance(context);
        instance.firebasePhotoStorage = FirebasePhotoStorage.getInstance(context);
        return instance;
    }

    private Query queryByUidUser(String uidUser) {
        Log.d(TAG, "queryByUidUser called with uidUser = " + uidUser);
        return annonceRef.orderByChild("utilisateur/uuid").equalTo(uidUser);
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
        Log.d(TAG, "Starting checkFirebaseRepository called with uidUtilisateur = " + uidUtilisateur);
        observeAllAnnonceByUidUser(uidUtilisateur)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(throwable -> Log.e(TAG, throwable.getMessage()))
                .switchMapSingle(annonceDto -> annonceRepository.countByUidUtilisateurAndUidAnnonce(uidUtilisateur, annonceDto.getUuid()))
                .doOnNext(integer -> {
                    if (integer != null && integer == 0) {
                        shouldAskQuestion.postValue(new AtomicBoolean(true));
                    }
                })
                .subscribe();
    }

    /**
     * Go read all the annonces for a specified uid user, then record all those ones to the local DB.
     *
     * @param context
     * @param uidUser
     */
    public void saveAnnoncesByUidUser(Context context, String uidUser) {
        queryByUidUser(uidUser).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    HashMap<String, AnnonceDto> mapAnnonceSearchDto = dataSnapshot.getValue(genericClass);
                    if (mapAnnonceSearchDto != null && !mapAnnonceSearchDto.isEmpty()) {
                        for (Map.Entry<String, AnnonceDto> entry : mapAnnonceSearchDto.entrySet()) {
                            saveAnnonceDtoToLocalDb(context, entry.getValue());
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

    /**
     * Will save an AnnonceDto to the Local DB.
     * This method will also take care to download all the photo
     * <p>
     * Count in local DB all the record with the same Uid User and Uid Annonce.
     * If no one exist, convert AnnonceDto to AnnonceEntity
     * Try to save this new AnnonceEntity
     * If AnnonceDto contained photos, try to save all the photos
     *
     * @param context
     * @param annonceDto
     */
    private void saveAnnonceDtoToLocalDb(Context context, AnnonceDto annonceDto) {
        Log.d(TAG, "Starting saveAnnonceDtoToLocalDb called with annonceDto = " + annonceDto.toString());
        annonceRepository.countByUidUtilisateurAndUidAnnonce(annonceDto.getUtilisateur().getUuid(), annonceDto.getUuid())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(throwable -> Log.e(TAG, "countByUidUtilisateurAndUidAnnonce.doOnError " + throwable.getMessage()))
                .filter(integer -> integer == null || integer.equals(0))
                .map(integer -> {
                    AnnonceEntity annonceEntity = AnnonceConverter.convertDtoToEntity(annonceDto);
                    String uidUtilisateur = annonceDto.getUtilisateur().getUuid();
                    annonceEntity.setUidUser(uidUtilisateur);
                    return annonceEntity;
                })
                .flatMapSingle(annonceEntity -> annonceRepository.singleSave(annonceEntity))
                .filter(annonceEntity -> annonceDto.getPhotos() != null && !annonceDto.getPhotos().isEmpty())
                .doOnSuccess(annonceEntity1 -> firebasePhotoStorage.savePhotoToLocalByListUrl(context, annonceEntity1.getIdAnnonce(), annonceDto.getPhotos()))
                .subscribe();
    }

    /**
     * Will emits all the AnnonceDto from Firebase for a given uid User
     *
     * @param uidUser uid of the user, we're looking for
     * @return Observable<AnnonceDto> wich will emit AnnonceDto
     */
    private Observable<AnnonceDto> observeAllAnnonceByUidUser(String uidUser) {
        Log.d(TAG, "Starting observeAllAnnonceByUidUser uidUtilisateur : " + uidUser);
        return Observable.create(emitter -> queryByUidUser(uidUser).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot == null || dataSnapshot.getValue() == null) {
                    emitter.onComplete();
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

    /**
     * Retrieve an annonce on Firebase Database based on its uidAnnonce
     *
     * @param uidAnnonce to retrieve
     * @return AnnonceDto
     */
    public Maybe<AnnonceDto> maybeFindByUidAnnonce(String uidAnnonce) {
        Log.d(TAG, "Starting maybeFindByUidAnnonce uidAnnonce : " + uidAnnonce);
        return Maybe.create(e ->
                annonceRef.child(uidAnnonce)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                AnnonceDto annonceDto = dataSnapshot.getValue(AnnonceDto.class);
                                if (annonceDto != null) {
                                    e.onSuccess(annonceDto);
                                } else {
                                    e.onComplete();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                e.onError(new RuntimeException(databaseError.getMessage()));
                            }
                        })
        );
    }

    /**
     * Va récupérer un uid et le timestamp du serveur pour une annonceDto
     *
     * @param annonceEntity
     * @return
     */
    public Single<AnnonceEntity> getUidAndTimestampFromFirebase(AnnonceEntity annonceEntity) {
        Log.d(TAG, "Starting getUidAndTimestampFromFirebase annonceEntity : " + annonceEntity);
        return Single.create(emitter ->
                FirebaseUtility.getServerTimestamp()
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(timestamp -> {
                            DatabaseReference dbRef;
                            if (annonceEntity.getUid() == null || annonceEntity.getUid().isEmpty()) {
                                dbRef = annonceRef.push();
                                annonceEntity.setUid(dbRef.getKey());
                            } else {
                                dbRef = annonceRef.child(annonceEntity.getUid());
                            }

                            annonceEntity.setDatePublication(timestamp);
                            annonceEntity.setUid(dbRef.getKey());
                            emitter.onSuccess(annonceEntity);
                        })
                        .subscribe()
        );
    }

    /**
     * Insert or Update an annonce into the Firebase Database
     *
     * @param annonceDto
     * @return UID de l'annonce DTO enregistré
     */
    public Single<String> saveAnnonceDtoToFirebase(AnnonceDto annonceDto) {
        Log.d(TAG, "Starting saveAnnonceDtoToFirebase annonceDto : " + annonceDto);
        return Single.create(emitter -> {
            if (annonceDto == null) {
                emitter.onError((new RuntimeException("Can't save null annonceDto object to Firebase")));
            } else if (annonceDto.getUuid() == null) {
                emitter.onError((new RuntimeException("UID is mandatory to save in Firebase")));
            } else {
                annonceRef.child(annonceDto.getUuid()).setValue(annonceDto)
                        .addOnFailureListener(emitter::onError)
                        .addOnSuccessListener(o ->
                                maybeFindByUidAnnonce(annonceDto.getUuid())
                                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                        .doOnError(emitter::onError)
                                        .doOnSuccess(annonceDto1 -> emitter.onSuccess(annonceDto1.getUuid()))
                                        .doOnComplete(() -> emitter.onError(new RuntimeException("No annonceDto in Firebase with Uid : " + annonceDto.getUuid())))
                                        .subscribe()
                        );
            }
        });
    }


    /**
     * Suppression d'une annonce sur Firebase Database
     * La suppression est basée sur l'UID de l'annonce.
     *
     * @param uidAnnonce à supprimer de Firebase Database
     */
    public Single<AtomicBoolean> delete(String uidAnnonce) {
        Log.d(TAG, "Starting delete " + uidAnnonce);
        return Single.create(emitter -> {
            if (uidAnnonce == null) {
                emitter.onSuccess(new AtomicBoolean(true));
            } else {
                annonceRef.child(uidAnnonce)
                        .removeValue()
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Fail to delete annonce on Firebase Database : " + uidAnnonce + " exception : " + e.getLocalizedMessage());
                            emitter.onError(e);
                        })
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Successful delete annonce on Firebase Database : " + uidAnnonce);
                            emitter.onSuccess(new AtomicBoolean(true));
                        });
            }
        });
    }
}