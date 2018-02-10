package oliweb.nc.oliweb.ui.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.SharedPreferencesHelper;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyAnnoncesViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceAdapterRaw;
import oliweb.nc.oliweb.ui.adapter.AnnonceAdapterSingle;

public class MyAnnoncesActivity extends AppCompatActivity {

    @BindView(R.id.recycler_annonces)
    RecyclerView recyclerView;

    private MyAnnoncesViewModel viewModel;
    private AnnonceAdapterRaw annonceAdapterRaw;
    private AnnonceAdapterSingle annonceAdapterSingle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = ViewModelProviders.of(this).get(MyAnnoncesViewModel.class);

        setContentView(R.layout.activity_my_annonces);
        ButterKnife.bind(this);

        // Ouvre l'activité pour modifier l'annonce.
        View.OnClickListener onClickListener = v -> {
            AnnonceEntity annonce = (AnnonceEntity) v.getTag();
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            intent.setClass(this, PostAnnonceActivity.class);
            bundle.putString(PostAnnonceActivity.BUNDLE_KEY_MODE, Constants.PARAM_MAJ);
            bundle.putLong(PostAnnonceActivity.BUNDLE_KEY_ID_ANNONCE, annonce.getIdAnnonce());
            intent.putExtras(bundle);
            startActivity(intent);
        };

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        // Recherche du mode display actuellement dans les préférences.
        int displayMode = SharedPreferencesHelper.getInstance(getApplicationContext(), Constants.SHARED_PREFERENCE_NAME).getSharedPreferences().getInt(Constants.DISPLAY_MODE, Constants.DISPLAY_MODE_RAW);

        if (displayMode == Constants.DISPLAY_MODE_RAW) {
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

        viewModel.findByUuidUtilisateur(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .observe(this, annonceWithPhotos -> {
                    if (displayMode == Constants.DISPLAY_MODE_RAW) {
                        annonceAdapterRaw.setListAnnonces(annonceWithPhotos);
                    } else {
                        annonceAdapterSingle.setListAnnonces(annonceWithPhotos);
                    }
                });
    }
}
