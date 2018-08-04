package oliweb.nc.oliweb.ui.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;

import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.fragment.ListMessageFragment;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AnnonceMessageActivity extends AppCompatActivity {

    private static final String TAG = AnnonceMessageActivity.class.getName();

    public static final String ARG_ANNONCE = "ARG_ANNONCE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_annonce_message);
        MyChatsActivityViewModel viewModel = ViewModelProviders.of(this).get(MyChatsActivityViewModel.class);

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(ARG_ANNONCE)) {
            AnnonceEntity annonce = getIntent().getExtras().getParcelable(ARG_ANNONCE);
            setTitle(annonce.getTitre());
            viewModel.rechercheMessageByAnnonce(annonce);
            viewModel.setFirebaseUserUid(FirebaseAuth.getInstance().getUid());
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_annonce_message, new ListMessageFragment()).commit();
        } else {
            Log.e(TAG, "AnnonceMessageActivity need ARG_ANNONCE to launch",new RuntimeException("Missing argument"));
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
