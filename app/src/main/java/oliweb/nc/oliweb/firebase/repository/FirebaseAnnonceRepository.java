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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_ANNONCE_REF;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_TIME_REF;

// TODO Faire des tests sur ce repository
public class FirebaseAnnonceRepository {

    private static final String TAG = FirebaseAnnonceRepository.class.getName();
    private DatabaseReference ANNONCE_REF = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_ANNONCE_REF);
    private DatabaseReference TIME_REF = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_TIME_REF);

    private static FirebaseAnnonceRepository instance;

    private AnnonceRepository annonceRepository;
    private AnnonceFullRepository annonceFullRepository;
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
        instance.annonceFullRepository = AnnonceFullRepository.getInstance(context);
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
        Log.d(TAG, "checkAnnonceExistInLocalOrSaveIt called with annonceDto = " + annonceDto.toString());
        annonceRepository.countByUidUtilisateurAndUidAnnonce(annonceDto.getUtilisateur().getUuid(), annonceDto.getUuid())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(integer -> {
                    if (integer == null || integer.equals(0)) {
                        getAnnonceFromFirebaseToLocalDb(context, annonceDto);
                    }
                })
                .doOnError(throwable -> Log.e(TAG, throwable.getLocalizedMessage(), throwable))
                .subscribe();
    }

    private void getAnnonceFromFirebaseToLocalDb(Context context, final AnnonceDto annonceFromFirebase) {
        Log.d(TAG, "getAnnonceFromFirebaseToLocalDb called with annonceDto = " + annonceFromFirebase.toString());
        try {
            AnnonceEntity annonceEntity = AnnonceConverter.convertDtoToEntity(annonceFromFirebase);
            String uidUtilisateur = annonceFromFirebase.getUtilisateur().getUuid();
            annonceEntity.setUuidUtilisateur(uidUtilisateur);
            annonceRepository.saveWithSingle(annonceEntity)
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnSuccess(annonceEntity1 -> {
                        Log.d(TAG, "Annonce has been stored successfully : " + annonceEntity1.getTitre());
                        if (annonceFromFirebase.getPhotos() != null && !annonceFromFirebase.getPhotos().isEmpty()) {
                            for (String photoUrl : annonceFromFirebase.getPhotos()) {
                                Log.d(TAG, "Try to save : " + photoUrl);
                                firebasePhotoRepository.savePhotoFromFirebaseStorageToLocal(context, annonceEntity1.getIdAnnonce(), photoUrl);
                            }
                        }
                    })
                    .doOnError(throwable -> Log.e(TAG, "Annonce has not been stored correctly UidAnnonce : " + annonceEntity.getUUID() + ", UidUtilisateur : " + uidUtilisateur))
                    .subscribe();
        } catch (Exception exception) {
            Log.e(TAG, exception.getLocalizedMessage(), exception);
        }
    }

    private void checkAnnonceLocalRepository(String uidUser, AnnonceDto annonceDto, @NonNull MutableLiveData<AtomicBoolean> shouldAskQuestion) {
        Log.d(TAG, "checkAnnonceLocalRepository called with uidUser : " + uidUser + " annonceDto : " + annonceDto);
        annonceRepository.countByUidUtilisateurAndUidAnnonce(uidUser, annonceDto.getUuid())
                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                .doOnSuccess(integer -> {
                    Log.d(TAG, "countByUidUtilisateurAndUidAnnonce.doOnSuccess integer : " + integer);
                    if (integer != null && integer == 0) {
                        shouldAskQuestion.postValue(new AtomicBoolean(true));
                    }
                })
                .doOnError(throwable -> Log.e(TAG, "countByUidUtilisateurAndUidAnnonce.doOnError " + throwable.getMessage()))
                .subscribe();
    }

    private Observable<AnnonceDto> getAllAnnonceFromFbByUidUser(String uidUtilisateur) {
        Log.d(TAG, "getAllAnnonceFromFbByUidUser uidUtilisateur : " + uidUtilisateur);
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

    public Single<AnnonceDto> findByUidAnnonce(String uidAnnonce) {
        Log.d(TAG, "findByUidAnnonce uidAnnonce : " + uidAnnonce);
        return Single.create(e ->
                ANNONCE_REF.child(uidAnnonce)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                AnnonceDto annonceDto = dataSnapshot.getValue(AnnonceDto.class);
                                if (annonceDto != null) {
                                    e.onSuccess(annonceDto);
                                } else {
                                    e.onSuccess(null);
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
     * Insert or Update an annonce into the Firebase Database
     *
     * @param annonceDto
     * @return
     */
    public Single<AnnonceDto> saveAnnonceToFirebase(AnnonceDto annonceDto) {
        Log.d(TAG, "saveAnnonceToFirebase annonceDto : " + annonceDto);
        DatabaseReference dbRef;
        if (annonceDto.getUuid() == null || annonceDto.getUuid().isEmpty()) {
            dbRef = ANNONCE_REF.push();
            annonceDto.setUuid(dbRef.getKey());
        } else {
            dbRef = ANNONCE_REF.child(annonceDto.getUuid());
        }

        return Single.create(emitter -> dbRef.setValue(annonceDto)
                .addOnFailureListener(emitter::onError)
                .addOnSuccessListener(o ->
                        setDatePublication(annonceDto)
                                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                .doOnError(emitter::onError)
                                .doOnSuccess(datePublication ->
                                        findByUidAnnonce(annonceDto.getUuid())
                                                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                                .doOnError(emitter::onError)
                                                .doOnSuccess(emitter::onSuccess)
                                                .subscribe()
                                )
                                .subscribe()
                ));
    }

    public Single<AnnonceDto> saveAnnonceToFirebase(Long idAnnonce) {
        Log.d(TAG, "saveAnnonceToFirebase idAnnonce : " + idAnnonce);
        return Single.create(emitter ->
                annonceFullRepository.findAnnoncesByIdAnnonce(idAnnonce)
                        .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(annonceFullEntity -> {
                            AnnonceDto annonceDto = AnnonceConverter.convertEntityToDto(annonceFullEntity);
                            saveAnnonceToFirebase(annonceDto)
                                    .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                                    .doOnError(emitter::onError)
                                    .doOnSuccess(emitter::onSuccess)
                                    .subscribe();
                        })
                        .subscribe()
        );
    }

    private Single<Long> setDatePublication(AnnonceDto annonceDto) {
        Log.d(TAG, "setDatePublication annonceDto : " + annonceDto);
        return Single.create(e -> {
                    DatabaseReference dbRef = ANNONCE_REF.child(annonceDto.getUuid());
                    dbRef.child("datePublication").setValue(ServerValue.TIMESTAMP)
                            .addOnFailureListener(e::onError)
                            .addOnSuccessListener(aVoid ->
                                    findByUidAnnonce(annonceDto.getUuid())
                                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                            .doOnError(e::onError)
                                            .doOnSuccess(annonceDtoRead -> e.onSuccess(annonceDtoRead.getDatePublication()))
                                            .subscribe());
                }
        );
    }

    private Single<Long> getServerTimestamp() {
        return Single.create(emitter ->
                TIME_REF.child("now").setValue(ServerValue.TIMESTAMP)
                        .addOnFailureListener(emitter::onError)
                        .addOnSuccessListener(aVoid ->
                                TIME_REF.child("now").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        emitter.onSuccess(dataSnapshot.getValue(Long.class));
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        emitter.onError(new RuntimeException(databaseError.getMessage()));
                                    }
                                })
                        )
        );
    }
}