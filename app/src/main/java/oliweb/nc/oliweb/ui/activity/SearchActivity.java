package oliweb.nc.oliweb.ui.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.SharedPreferencesHelper;
import oliweb.nc.oliweb.Utility;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceWithPhotos;
import oliweb.nc.oliweb.network.NetworkReceiver;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceSearchDto;
import oliweb.nc.oliweb.network.elasticsearchDto.ElasticsearchRequest;
import oliweb.nc.oliweb.ui.adapter.AnnonceAdapterRaw;
import oliweb.nc.oliweb.ui.adapter.AnnonceAdapterSingle;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_REQUEST_REF;
import static oliweb.nc.oliweb.Constants.PER_PAGE_REQUEST;

public class SearchActivity extends AppCompatActivity {

    @BindView(R.id.recycler_search_annonce)
    RecyclerView recyclerView;

    @BindView(R.id.empty_search_linear)
    LinearLayout linearLayout;

    private AnnonceAdapterRaw annonceAdapterRaw;
    private AnnonceAdapterSingle annonceAdapterSingle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        ButterKnife.bind(this);

        // Ouvre l'activité PostAnnonceActivity en mode Modification
        View.OnClickListener onClickListener = v -> {
            AnnonceEntity annonce = (AnnonceEntity) v.getTag();
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            intent.setClass(this, PostAnnonceActivity.class);
            bundle.putString(PostAnnonceActivity.BUNDLE_KEY_MODE, Constants.PARAM_VIS);
            bundle.putLong(PostAnnonceActivity.BUNDLE_KEY_ID_ANNONCE, annonce.getIdAnnonce());
            intent.putExtras(bundle);
            startActivity(intent);
        };

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        // Recherche du mode display actuellement dans les préférences.
        boolean displayBeautyMode = SharedPreferencesHelper.getInstance(getApplicationContext()).getDisplayBeautyMode();
        if (displayBeautyMode) {
            // En mode Raw
            RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
            annonceAdapterRaw = new AnnonceAdapterRaw(onClickListener);
            recyclerView.setAdapter(annonceAdapterRaw);
            recyclerView.addItemDecoration(itemDecoration);
        } else {
            // En mode Beauty
            annonceAdapterSingle = new AnnonceAdapterSingle(onClickListener);
            recyclerView.setAdapter(annonceAdapterSingle);
        }

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

            if (NetworkReceiver.checkConnection(this)) {
                String query = intent.getStringExtra(SearchManager.QUERY);

                ElasticsearchRequest request = new ElasticsearchRequest(1, PER_PAGE_REQUEST, query);

                // Création d'une nouvelle request dans la table request
                DatabaseReference newRequestRef = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_REQUEST_REF).push();
                newRequestRef.setValue(request);

                // Ensuite on va écouter les changements pour cette nouvelle requête
                newRequestRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child("results").exists()) {

                            // Récupération des données
                            List<AnnonceSearchDto> listAnnonceReturned = (List<AnnonceSearchDto>) dataSnapshot.child("results").getValue();
                            if (listAnnonceReturned != null) {
                                linearLayout.setVisibility(View.GONE);
                                List<AnnonceWithPhotos> listAnnonceWithPhoto = new ArrayList<>();
                                for (AnnonceSearchDto dto : listAnnonceReturned) {
                                    listAnnonceWithPhoto.add(Utility.convertDtoToEntity(dto));
                                }
                                if (displayBeautyMode) {
                                    annonceAdapterRaw.setListAnnonces(listAnnonceWithPhoto);
                                } else {
                                    annonceAdapterSingle.setListAnnonces(listAnnonceWithPhoto);
                                }
                            } else {
                                linearLayout.setVisibility(View.VISIBLE);
                            }

                            // After the data has been received we delete the request.
                            newRequestRef.removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Do nothing here
                    }
                });
            } else {
                Toast.makeText(this, "Can't search without internet connection", Toast.LENGTH_LONG).show();
            }
        }
    }
}
