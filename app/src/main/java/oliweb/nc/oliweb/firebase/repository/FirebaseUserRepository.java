package oliweb.nc.oliweb.firebase.repository;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_USER_REF;

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
        return Single.create(emitter ->
                USER_REF.child(utilisateurEntity.getUid()).setValue(utilisateurEntity)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Utilisateur correctement créé dans Firebase " + utilisateurEntity.toString());
                            emitter.onSuccess(new AtomicBoolean(true));
                        })
                        .addOnFailureListener(exception -> {
                            Log.d(TAG, "FAIL : L'utilisateur n'a pas pu être créé dans Firebase " + utilisateurEntity.toString());
                            emitter.onError(exception);
                        })
        );
    }

    public Single<UtilisateurEntity> getUtilisateurByUid(String uidUser) {
        Log.d(TAG, "Starting getUtilisateurByUid");
        return Single.create(emitter ->
                USER_REF.child(uidUser).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        UtilisateurEntity user = dataSnapshot.getValue(UtilisateurEntity.class);
                        emitter.onSuccess(user);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        emitter.onError(new RuntimeException(databaseError.getMessage()));
                    }
                })
        );
    }
}
