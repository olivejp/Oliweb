package oliweb.nc.oliweb.firebase.repository;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_USER_REF;

// TODO Faire des tests sur ce repository
public class FirebaseUserRepository {

    private static final String TAG = FirebaseUserRepository.class.getName();
    private DatabaseReference USER_REF = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_USER_REF);

    private static FirebaseUserRepository instance;

    private FirebaseUserRepository() {
    }

    public static FirebaseUserRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseUserRepository();
        }
        return instance;
    }

    public Single<AtomicBoolean> insertUserIntoFirebase(UtilisateurEntity utilisateurEntity) {
        Log.d(TAG, "Starting insertUserIntoFirebase");
        return Single.create(emitter -> USER_REF.child(utilisateurEntity.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue(UtilisateurFirebase.class) == null) {
                    USER_REF.child(utilisateurEntity.getUid()).setValue(utilisateurEntity)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Utilisateur correctement créé dans Firebase " + utilisateurEntity.toString());
                                emitter.onSuccess(new AtomicBoolean(true));
                            })
                            .addOnFailureListener(exception -> {
                                Log.d(TAG, "FAIL : L'utilisateur n'a pas pu être créé dans Firebase " + utilisateurEntity.toString());
                                emitter.onError(exception);
                            });
                } else {
                    Log.d(TAG, "Utilisateur's already in the Firebase Database");
                    emitter.onSuccess(new AtomicBoolean(false));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("MainActivityViewModel", "onCancelled");
                emitter.onError(new RuntimeException(databaseError.getMessage()));
            }
        }));
    }
}
