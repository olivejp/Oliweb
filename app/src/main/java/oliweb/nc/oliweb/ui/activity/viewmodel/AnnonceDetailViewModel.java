package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import oliweb.nc.oliweb.firebase.FirebaseQueryLiveData;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_USER_REF;

public class AnnonceDetailViewModel extends AndroidViewModel {

    private FirebaseQueryLiveData fbSellerLiveData;

    public AnnonceDetailViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<DataSnapshot> getFirebaseSeller(String uidSeller) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_USER_REF).child(uidSeller);
        if (fbSellerLiveData == null || fbSellerLiveData.getQuery() != ref) {
            fbSellerLiveData = new FirebaseQueryLiveData(ref, false);
        }
        return fbSellerLiveData;
    }
}
