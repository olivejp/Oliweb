package oliweb.nc.oliweb.firebase.repository;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import io.reactivex.Single;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_TIME_REF;

public class FirebaseUtility {

    private static final String TAG = FirebaseUtility.class.getName();

    public static Single<Long> getServerTimestamp() {
        Log.d(TAG, "Starting getServerTimestamp");
        DatabaseReference TIME_REF = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_TIME_REF);
        return Single.create(emitter ->
                TIME_REF.child("now").setValue(ServerValue.TIMESTAMP)
                        .addOnFailureListener(exception -> {
                            Log.d(TAG, "getServerTimestamp setValue.onFailure exception : " + exception.getLocalizedMessage(), exception);
                            emitter.onError(exception);
                        })
                        .addOnSuccessListener(aVoid ->
                                {
                                    Log.d(TAG, "getServerTimestamp setValue.onSuccess");
                                    TIME_REF.child("now").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Long timestamp = dataSnapshot.getValue(Long.class);
                                            Log.d(TAG, "getServerTimestamp getValue.onSuccess datasnapshot.getValue() : " + timestamp);
                                            emitter.onSuccess(timestamp);
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            Log.d(TAG, "getServerTimestamp getValue.onCancelled databaseError : " + databaseError.getMessage());
                                            emitter.onError(new RuntimeException(databaseError.getMessage()));
                                        }
                                    });
                                }
                        )
        );
    }
}