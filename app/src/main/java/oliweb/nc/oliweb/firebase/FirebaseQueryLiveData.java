package oliweb.nc.oliweb.firebase;

import android.arch.lifecycle.LiveData;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by orlanth23 on 31/01/2018.
 */

public class FirebaseQueryLiveData extends LiveData<DataSnapshot> {
    private static final String LOG_TAG = "FirebaseQueryLiveData";

    private final Query query;
    private final MyValueEventListener listener = new MyValueEventListener();
    private boolean stayConnected = true;

    public FirebaseQueryLiveData(Query query) {
        this.query = query;
    }

    public FirebaseQueryLiveData(Query query, boolean stayConnected) {
        this.query = query;
        this.stayConnected = stayConnected;
    }

    public FirebaseQueryLiveData(DatabaseReference ref) {
        this.query = ref;
    }

    public FirebaseQueryLiveData(DatabaseReference ref, boolean stayConnected) {
        this.query = ref;
        this.stayConnected = stayConnected;
    }

    public Query getQuery() {
        return this.query;
    }

    public boolean getStayConnected() {
        return this.stayConnected;
    }

    @Override
    protected void onActive() {
        Log.d(LOG_TAG, "onActive");
        if (stayConnected) {
            query.addValueEventListener(listener);
        } else {
            query.addListenerForSingleValueEvent(listener);
        }
    }

    @Override
    protected void onInactive() {
        Log.d(LOG_TAG, "onInactive");
        query.removeEventListener(listener);
    }

    private class MyValueEventListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            setValue(dataSnapshot);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.e(LOG_TAG, "Can't listen to query " + query, databaseError.toException());
        }
    }
}
