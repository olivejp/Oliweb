package oliweb.nc.oliweb.repository.firebase;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

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

    public Single<UserEntity> insertUserIntoFirebase(UserEntity userEntity) {
        Log.d(TAG, "Try to insert this user in firebase " + userEntity);
        return Single.create(emitter -> userRef.child(userEntity.getUid()).setValue(userEntity)
                .addOnSuccessListener(aVoid -> emitter.onSuccess(userEntity))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Fail to send this user " + userEntity);
                    emitter.onError(error);
                })
        );
    }

    public Single<UserEntity> getUtilisateurByUid(String uidUser) {
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

    public Single<String> getToken() {
        Log.d(TAG, "Starting getToken");
        return Single.create(emitter -> FirebaseInstanceId.getInstance().getInstanceId()
                .addOnSuccessListener(instanceIdResult -> emitter.onSuccess(instanceIdResult.getToken()))
                .addOnFailureListener(emitter::onError)
        );
    }
}
