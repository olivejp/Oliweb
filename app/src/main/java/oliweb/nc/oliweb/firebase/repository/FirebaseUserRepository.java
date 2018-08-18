package oliweb.nc.oliweb.firebase.repository;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.UserEntity;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_USER_REF;

@Singleton
public class FirebaseUserRepository {

    private static final String TAG = FirebaseUserRepository.class.getName();
    private DatabaseReference userRef;

    @Inject
    public FirebaseUserRepository() {
        userRef = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_USER_REF);
    }

    public Single<AtomicBoolean> insertUserIntoFirebase(UserEntity userEntity) {
        Log.d(TAG, "Starting insertUserIntoFirebase");
        return Single.create(emitter ->
                userRef.child(userEntity.getUid()).setValue(userEntity)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Utilisateur correctement créé dans Firebase " + userEntity.toString());
                            emitter.onSuccess(new AtomicBoolean(true));
                        })
                        .addOnFailureListener(exception -> {
                            Log.d(TAG, "FAIL : L'utilisateur n'a pas pu être créé dans Firebase " + userEntity.toString());
                            emitter.onError(exception);
                        })
        );
    }

    public Single<UserEntity> getUtilisateurByUid(String uidUser) {
        Log.d(TAG, "Starting getUtilisateurByUid");
        return Single.create(emitter ->
                userRef.child(uidUser).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        try {
                            UserEntity user = dataSnapshot.getValue(UserEntity.class);
                            if (user != null) {
                                emitter.onSuccess(user);
                            } else {
                                throw new FirebaseRepositoryException("Return a null value");
                            }
                        } catch (FirebaseRepositoryException e) {
                            emitter.onError(e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        emitter.onError(new RuntimeException(databaseError.getMessage()));
                    }
                })
        );
    }

    public LiveData<UserEntity> getLiveUtilisateurByUid(String uidUser) {
        return new LiveData<UserEntity>() {
            @Override
            public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<UserEntity> observer) {
                super.observe(owner, observer);
                userRef.child(uidUser).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        UserEntity user = dataSnapshot.getValue(UserEntity.class);
                        observer.onChanged(user);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, databaseError.getMessage());
                        observer.onChanged(null);
                    }
                });
            }
        };
    }


    public Single<String> getToken() {
        Log.d(TAG, "Starting getToken");
        return Single.create(emitter ->
                FirebaseInstanceId.getInstance().getInstanceId()
                        .addOnSuccessListener(instanceIdResult -> emitter.onSuccess(instanceIdResult.getToken()))
                        .addOnFailureListener(emitter::onError)
        );
    }
}
