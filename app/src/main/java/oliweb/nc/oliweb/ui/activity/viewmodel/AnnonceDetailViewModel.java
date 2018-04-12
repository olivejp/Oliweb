package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.firebase.FirebaseQueryLiveData;

public class AnnonceDetailViewModel extends AndroidViewModel {

    private FirebaseQueryLiveData firebaseLiveData;

    public AnnonceDetailViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<DataSnapshot> getFirebaseAnnonceDetailByUid(String uidAnnonce) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_ANNONCE_REF).child(uidAnnonce);

        if (firebaseLiveData == null || firebaseLiveData.getQuery() != ref) {
            firebaseLiveData = new FirebaseQueryLiveData(ref);
        }

        return firebaseLiveData;
    }
}
