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
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_ANNONCE_REF;

public class FirebaseAnnonceRepository {

    private static final String TAG = FirebaseAnnonceRepository.class.getName();
    private DatabaseReference ANNONCE_REF = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_ANNONCE_REF);

    private static FirebaseAnnonceRepository instance;

    private AnnonceRepository annonceRepository;
    private AnnonceFullRepository annonceFullRepository;
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
        instance.annonceFullRepository = AnnonceFullRepository.getInstance(context);
        instance.firebasePhotoStorage = FirebasePhotoStorage.getInstance(context);
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
        Log.d(TAG, "Starting checkFirebaseRepository called with uidUtilisateur = " + uidUtilisateur);
        getAllAnnonceByUidUser(uidUtilisateur)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnNext(annonceDto -> checkAnnonceLocalRepository(uidUtilisateur, annonceDto.getUuid(), shouldAskQuestion))
                .doOnError(throwable -> Log.e(TAG, throwable.getMessage()))
                .subscribe();
    }

    public void checkAnnonceExistInLocalOrSaveIt(Context context, AnnonceDto annonceDto) {
        Log.d(TAG, "Starting checkAnnonceExistInLocalOrSaveIt called with annonceDto = " + annonceDto.toString());
        annonceRepository.countByUidUtilisateurAndUidAnnonce(annonceDto.getUtilisateur().getUuid(), annonceDto.getUuid())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(throwable -> Log.e(TAG, "countByUidUtilisateurAndUidAnnonce.doOnError " + throwable.getMessage()))
                .doOnSuccess(integer -> {
                    if (integer == null || integer.equals(0)) {
                        getAnnonceFromFbToLocalDb(context, annonceDto);
                    }
                })
                .subscribe();
    }

    private void checkAnnonceLocalRepository(String uidUser, String uidAnnonce, @NonNull MutableLiveData<AtomicBoolean> shouldAskQuestion) {
        Log.d(TAG, "Starting checkAnnonceLocalRepository called with uidUser : " + uidUser + " uidAnnonce : " + uidAnnonce);
        annonceRepository.countByUidUtilisateurAndUidAnnonce(uidUser, uidAnnonce)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(integer -> {
                    Log.d(TAG, "countByUidUtilisateurAndUidAnnonce.doOnSuccess integer : " + integer);
                    if (integer != null && integer == 0) {
                        shouldAskQuestion.postValue(new AtomicBoolean(true));
                    }
                })
                .doOnError(throwable -> Log.e(TAG, "countByUidUtilisateurAndUidAnnonce.doOnError " + throwable.getMessage()))
                .subscribe();
    }

    private void getAnnonceFromFbToLocalDb(Context context, final AnnonceDto annonceFromFirebase) {
        Log.d(TAG, "Starting getAnnonceFromFbToLocalDb called with annonceDto = " + annonceFromFirebase.toString());
        try {
            AnnonceEntity annonceEntity = AnnonceConverter.convertDtoToEntity(annonceFromFirebase);
            String uidUtilisateur = annonceFromFirebase.getUtilisateur().getUuid();
            annonceEntity.setUidUser(uidUtilisateur);
            annonceRepository.saveWithSingle(annonceEntity)
                    .doOnError(throwable -> Log.e(TAG, "Annonce has not been stored correctly UidAnnonce : " + annonceEntity.getUid() + ", uidUtilisateur : " + uidUtilisateur))
                    .doOnSuccess(annonceEntity1 -> {
                        Log.d(TAG, "Annonce has been stored successfully : " + annonceEntity1.getTitre());
                        if (annonceFromFirebase.getPhotos() != null && !annonceFromFirebase.getPhotos().isEmpty()) {
                            for (String photoUrl : annonceFromFirebase.getPhotos()) {
                                Log.d(TAG, "Try to save : " + photoUrl);
                                if (photoUrl != null && !photoUrl.isEmpty()) {
                                    firebasePhotoStorage.saveFromRemoteToLocal(context, annonceEntity1.getId(), photoUrl);
                                }
                            }
                        }
                    })
                    .subscribe();
        } catch (Exception exception) {
            Log.e(TAG, exception.getLocalizedMessage(), exception);
        }
    }

    private Observable<AnnonceDto> getAllAnnonceByUidUser(String uidUtilisateur) {
        Log.d(TAG, "Starting getAllAnnonceByUidUser uidUtilisateur : " + uidUtilisateur);
        return Observable.create(emitter -> queryByUidUser(uidUtilisateur).addListenerForSingleValueEvent(new ValueEventListener() {
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

    public Maybe<AnnonceDto> findByUidAnnonce(String uidAnnonce) {
        Log.d(TAG, "Starting findByUidAnnonceAndStatusNotIn uidAnnonce : " + uidAnnonce);
        return Maybe.create(e ->
                ANNONCE_REF.child(uidAnnonce)
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
                                dbRef = ANNONCE_REF.push();
                                annonceEntity.setUid(dbRef.getKey());
                            } else {
                                dbRef = ANNONCE_REF.child(annonceEntity.getUid());
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
     * @return
     */
    public Single<AnnonceDto> saveAnnonceToFirebase(AnnonceDto annonceDto) {
        Log.d(TAG, "Starting saveAnnonceToFirebase annonceDto : " + annonceDto);
        DatabaseReference dbRef;
        if (annonceDto.getUuid() == null || annonceDto.getUuid().isEmpty()) {
            dbRef = ANNONCE_REF.push();
            annonceDto.setUuid(dbRef.getKey());
        } else {
            dbRef = ANNONCE_REF.child(annonceDto.getUuid());
        }

        return Single.create(emitter ->
                FirebaseUtility.getServerTimestamp()
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(datePublication -> {
                            annonceDto.setDatePublication(datePublication);
                            dbRef.setValue(annonceDto)
                                    .addOnFailureListener(emitter::onError)
                                    .addOnSuccessListener(o ->
                                            findByUidAnnonce(annonceDto.getUuid())
                                                    .doOnError(emitter::onError)
                                                    .doOnSuccess(emitter::onSuccess)
                                                    .doOnComplete(() -> emitter.onError(new RuntimeException("No annonceDto in Firebase with Uid : " + annonceDto.getUuid())))
                                                    .subscribe()
                                    );
                        })
                        .subscribe()
        );
    }

    public Single<AnnonceEntity> saveAnnonceToFirebase(Long idAnnonce) {
        Log.d(TAG, "Starting saveAnnonceToFirebase idAnnonce : " + idAnnonce);
        return Single.create(emitter ->
                annonceFullRepository.findAnnoncesByIdAnnonce(idAnnonce)
                        .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                        .map(AnnonceConverter::convertFullEntityToDto)
                        .toObservable()
                        .switchMap(annonceDto -> saveAnnonceToFirebase(annonceDto).toObservable())
                        .switchMap(annonceDto -> annonceRepository.findById(idAnnonce).toObservable())
        );
    }

    /**
     * Suppression d'une annonce sur Firebase Database
     * La suppression est basée sur l'UID de l'annonce.
     *
     * @param annonce à supprimer de Firebase Database
     */
    public Single<AtomicBoolean> delete(AnnonceEntity annonce) {
        Log.d(TAG, "Starting delete " + annonce);
        return Single.create(emitter -> {
            if (annonce == null || annonce.getUid() == null) {
                emitter.onSuccess(new AtomicBoolean(true));
            } else {
                ANNONCE_REF.child(annonce.getUid())
                        .removeValue()
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Fail to delete annonce on Firebase Database : " + annonce.getUid() + " exception : " + e.getLocalizedMessage());
                            emitter.onError(e);
                        })
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Successful delete annonce on Firebase Database : " + annonce.getUid());
                            emitter.onSuccess(new AtomicBoolean(true));
                        });
            }
        });
    }
}