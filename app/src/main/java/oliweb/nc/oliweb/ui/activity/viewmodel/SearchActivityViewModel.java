package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import oliweb.nc.oliweb.Utility;
import oliweb.nc.oliweb.database.entity.AnnonceWithPhotos;
import oliweb.nc.oliweb.network.NetworkReceiver;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceSearchDto;
import oliweb.nc.oliweb.network.elasticsearchDto.ElasticsearchRequest;
import oliweb.nc.oliweb.network.elasticsearchDto.ResultElasticSearchDto;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_REQUEST_REF;
import static oliweb.nc.oliweb.Constants.PER_PAGE_REQUEST;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class SearchActivityViewModel extends AndroidViewModel {

    private GenericTypeIndicator<List<ResultElasticSearchDto<AnnonceSearchDto>>> genericClass;
    private MutableLiveData<List<AnnonceWithPhotos>> listAnnonce;
    private DatabaseReference newRequestRef;

    public SearchActivityViewModel(@NonNull Application application) {
        super(application);
        genericClass = new GenericTypeIndicator<List<ResultElasticSearchDto<AnnonceSearchDto>>>() {
        };
    }

    public LiveData<List<AnnonceWithPhotos>> getListAnnonce() {
        if (listAnnonce == null) {
            listAnnonce = new MutableLiveData<>();
            listAnnonce.setValue(new ArrayList<>());
        }
        return listAnnonce;
    }

    private ValueEventListener listener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot.child("results").exists()) {
                List<ResultElasticSearchDto<AnnonceSearchDto>> list = dataSnapshot.child("results").getValue(genericClass);
                if (list != null && !list.isEmpty()) {
                    List<AnnonceWithPhotos> listAnnonceWithPhoto = new ArrayList<>();
                    for (ResultElasticSearchDto<AnnonceSearchDto> resultSearchSnapshot : list) {
                        listAnnonceWithPhoto.add(Utility.convertDtoToEntity(resultSearchSnapshot.get_source()));
                    }
                    listAnnonce.postValue(listAnnonceWithPhoto);
                }
                // After the data has been received we delete the request.
                newRequestRef.removeValue();
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Do nothing here
        }
    };

    public boolean makeASearch(String query) {
        if (NetworkReceiver.checkConnection(getApplication().getApplicationContext())) {
            ElasticsearchRequest request = new ElasticsearchRequest(1, PER_PAGE_REQUEST, query);

            // Création d'une nouvelle request dans la table request
            newRequestRef = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_REQUEST_REF).push();
            newRequestRef.setValue(request);

            // Ensuite on va écouter les changements pour cette nouvelle requête
            newRequestRef.addValueEventListener(listener);
            return true;
        } else {
            return false;
        }
    }
}
