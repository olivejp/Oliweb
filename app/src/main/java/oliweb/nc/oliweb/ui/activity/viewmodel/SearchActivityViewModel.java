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
import java.util.concurrent.atomic.AtomicBoolean;

import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
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
    private MutableLiveData<List<AnnoncePhotos>> listAnnonce;
    private DatabaseReference newRequestRef;
    private MutableLiveData<AtomicBoolean> loading;

    public SearchActivityViewModel(@NonNull Application application) {
        super(application);
        genericClass = new GenericTypeIndicator<List<ResultElasticSearchDto<AnnonceSearchDto>>>() {
        };
    }

    public LiveData<List<AnnoncePhotos>> getListAnnonce() {
        if (listAnnonce == null) {
            listAnnonce = new MutableLiveData<>();
            listAnnonce.setValue(new ArrayList<>());
        }
        return listAnnonce;
    }

    public LiveData<AtomicBoolean> getLoading() {
        if (loading == null) {
            loading = new MutableLiveData<>();
            loading.setValue(new AtomicBoolean(false));
        }
        return loading;
    }

    private ValueEventListener listener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            List<AnnoncePhotos> listAnnonceWithPhoto = new ArrayList<>();
            if (dataSnapshot.child("no_results").exists()) {
                listAnnonce.postValue(listAnnonceWithPhoto);
                newRequestRef.removeEventListener(this);
                newRequestRef.removeValue();
                updateLoadingStatus(false);
            } else {
                if (dataSnapshot.child("results").exists()) {
                    List<ResultElasticSearchDto<AnnonceSearchDto>> list = dataSnapshot.child("results").getValue(genericClass);
                    if (list != null && !list.isEmpty()) {
                        for (ResultElasticSearchDto<AnnonceSearchDto> resultSearchSnapshot : list) {
                            listAnnonceWithPhoto.add(AnnonceConverter.convertDtoToEntity(resultSearchSnapshot.get_source()));
                        }
                        listAnnonce.postValue(listAnnonceWithPhoto);
                    }
                    newRequestRef.removeEventListener(this);
                    newRequestRef.removeValue();
                    updateLoadingStatus(false);
                }
            }

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            updateLoadingStatus(false);
        }
    };

    private void updateLoadingStatus(boolean status) {
        if (loading == null) {
            loading = new MutableLiveData<>();
        }
        loading.postValue(new AtomicBoolean(status));
    }

    /**
     * Launch a search with the Query
     *
     * @param query
     * @return true if the search has been launched, false otherwise
     */
    public boolean makeASearch(String query) {
        if (NetworkReceiver.checkConnection(getApplication().getApplicationContext())) {
            ElasticsearchRequest request = new ElasticsearchRequest(1, PER_PAGE_REQUEST, query);

            // Création d'une nouvelle request dans la table request
            newRequestRef = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_REQUEST_REF).push();
            newRequestRef.setValue(request);

            // Ensuite on va écouter les changements pour cette nouvelle requête
            newRequestRef.addValueEventListener(listener);
            updateLoadingStatus(true);
            return true;
        } else {
            return false;
        }
    }
}
