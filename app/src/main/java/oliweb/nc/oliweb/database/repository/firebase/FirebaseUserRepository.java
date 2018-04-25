package oliweb.nc.oliweb.database.repository.firebase;

import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.converter.UtilisateurConverter;
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
}
