package oliweb.nc.oliweb.repository.firebase;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
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

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.dto.elasticsearch.AnnonceDto;
import oliweb.nc.oliweb.utility.FirebaseUtility;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_ANNONCE_REF;

@Singleton
public class FirebaseAnnonceRepository {

    private static final String TAG = FirebaseAnnonceRepository.class.getName();

    private DatabaseReference annonceRef;

    private GenericTypeIndicator<HashMap<String, AnnonceDto>> genericClass;

    @Inject
    public FirebaseAnnonceRepository() {
        annonceRef = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_ANNONCE_REF);
        genericClass = new GenericTypeIndicator<HashMap<String, AnnonceDto>>() {
        };
    }

    private Query queryByUidUser(String uidUser) {
        Log.d(TAG, "queryByUidUser called with uidUser = " + uidUser);
        return annonceRef.orderByChild("utilisateur/uuid").equalTo(uidUser);
    }

    public LiveData<Long> getCountAnnonceByUidUser(String uidUser) {
        return new LiveData<Long>() {
            @Override
            public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<Long> observer) {
                super.observe(owner, observer);
                annonceRef.orderByChild("utilisateur/uuid").equalTo(uidUser).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        long count = dataSnapshot.getChildrenCount();
                        observer.onChanged(count);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        observer.onChanged(0L);
                    }
                });
            }
        };
    }

    /**
     * Will emits all the AnnonceDto from Firebase for a given uid User
     *
     * @param uidUser uid of the user, we're looking for
     * @return Observable<AnnonceDto> wich will emit AnnonceDto
     */
    public Observable<AnnonceDto> observeAllAnnonceByUidUser(String uidUser) {
        Log.d(TAG, "Starting observeAllAnnonceByUidUser uidUser : " + uidUser);
        return Observable.create(emitter -> queryByUidUser(uidUser).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
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
            public void onCancelled(@NonNull DatabaseError databaseError) {
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
    public Maybe<AnnonceDto> findMaybeByUidAnnonce(String uidAnnonce) {
        Log.d(TAG, "Starting findMaybeByUidAnnonce uidAnnonce : " + uidAnnonce);
        return Maybe.create(e -> annonceRef.child(uidAnnonce).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        AnnonceDto annonceDto = dataSnapshot.getValue(AnnonceDto.class);
                        if (annonceDto != null) {
                            e.onSuccess(annonceDto);
                        } else {
                            e.onComplete();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        e.onError(new RuntimeException(databaseError.getMessage()));
                    }
                })
        );
    }

    /**
     * Va récupérer un uid et le timestamp du serveur pour une annonceDto
     *
     * @param annonceEntity qui nécessite un timestamp et un uid
     * @return annonceEntity avec les attributs datePublication et uid renseigné avec des valeurs issues de Firebase.
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
    public Single<String> saveAnnonceToFirebase(AnnonceDto annonceDto) {
        Log.d(TAG, "Starting saveAnnonceToFirebase annonceDto : " + annonceDto);
        return Single.create(emitter -> {
            if (annonceDto == null) {
                emitter.onError((new RuntimeException("Can't save null annonceDto object to Firebase")));
            } else if (annonceDto.getUuid() == null || annonceDto.getUuid().isEmpty()) {
                emitter.onError((new RuntimeException("UID is mandatory to save in Firebase")));
            } else {
                annonceRef.child(annonceDto.getUuid()).setValue(annonceDto)
                        .addOnSuccessListener(o -> emitter.onSuccess(annonceDto.getUuid()))
                        .addOnFailureListener(emitter::onError);
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