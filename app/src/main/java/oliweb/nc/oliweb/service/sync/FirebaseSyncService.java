package oliweb.nc.oliweb.service.sync;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import oliweb.nc.oliweb.database.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;

/**
 * Created by orlanth23 on 03/03/2018.
 */

public class FirebaseSyncService {

    private static final String TAG = FirebaseSyncService.class.getName();

    private static FirebaseSyncService instance;

    private FirebaseAnnonceRepository firebaseAnnonceRepository;

    private static final GenericTypeIndicator<HashMap<String, AnnonceDto>> genericClass = new GenericTypeIndicator<HashMap<String, AnnonceDto>>() {
    };

    private FirebaseSyncService() {
    }

    public static FirebaseSyncService getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseSyncService();
        }
        instance.firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(context);
        return instance;
    }

    public void synchronize(Context context, String uidUser) {
        Log.d(TAG, "synchronize");
        firebaseAnnonceRepository.queryByUidUser(uidUser).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    HashMap<String, AnnonceDto> mapAnnonceSearchDto = dataSnapshot.getValue(genericClass);
                    if (mapAnnonceSearchDto != null && !mapAnnonceSearchDto.isEmpty()) {
                        for (Map.Entry<String, AnnonceDto> entry : mapAnnonceSearchDto.entrySet()) {
                            firebaseAnnonceRepository.checkAnnonceExistInLocalOrSaveIt(context, entry.getValue());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled");
            }
        });
    }
}
