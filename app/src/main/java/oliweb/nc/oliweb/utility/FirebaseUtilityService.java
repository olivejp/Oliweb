package oliweb.nc.oliweb.utility;

import androidx.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_TIME_REF;

@Singleton
public class FirebaseUtilityService {

    private static final String TAG = FirebaseUtilityService.class.getName();

    @Inject
    public FirebaseUtilityService() {
        // Do nothing
    }

    public Single<Long> getServerTimestamp() {
        Log.d(TAG, "Starting getServerTimestamp");
        DatabaseReference timeRef = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_TIME_REF);
        return Single.create(emitter ->
                timeRef.child("now").setValue(ServerValue.TIMESTAMP)
                        .addOnFailureListener(exception -> {
                            Log.d(TAG, "getServerTimestamp setValue.onFailure exception : " + exception.getLocalizedMessage(), exception);
                            emitter.onError(exception);
                        })
                        .addOnSuccessListener(aVoid ->
                                {
                                    Log.d(TAG, "getServerTimestamp setValue.onSuccess");
                                    timeRef.child("now").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            Long timestamp = dataSnapshot.getValue(Long.class);
                                            Log.d(TAG, "getServerTimestamp getValue.onSuccess datasnapshot.getValue() : " + timestamp);
                                            emitter.onSuccess(timestamp);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            Log.d(TAG, "getServerTimestamp getValue.onCancelled databaseError : " + databaseError.getMessage());
                                            emitter.onError(new RuntimeException(databaseError.getMessage()));
                                        }
                                    });
                                }
                        )
        );
    }
}