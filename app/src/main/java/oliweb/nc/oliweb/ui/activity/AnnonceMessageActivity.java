package oliweb.nc.oliweb.ui.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.fragment.ListMessageFragment;

public class AnnonceMessageActivity extends AppCompatActivity {

    private MyChatsActivityViewModel viewModel;
    public static final String ARG_UID_CHAT = "ARG_UID_CHAT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_annonce_message);
        viewModel = ViewModelProviders.of(this).get(MyChatsActivityViewModel.class);

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(ARG_UID_CHAT)) {
            String uidChat = getIntent().getExtras().getString(ARG_UID_CHAT);
            viewModel.setSelectedUidChat(uidChat);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_annonce_message, new ListMessageFragment()).commit();
        }
    }
}
