package oliweb.nc.oliweb.ui.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.fragment.ListChatFragment;
import oliweb.nc.oliweb.ui.fragment.ListMessageFragment;

public class MyChatsActivity extends AppCompatActivity {

    public static final String TAG_MASTER_FRAGMENT = "TAG_MASTER_FRAGMENT";
    public static final String TAG_DETAIL_FRAGMENT = "TAG_DETAIL_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyChatsActivityViewModel viewModel = ViewModelProviders.of(this).get(MyChatsActivityViewModel.class);

        setContentView(R.layout.activity_my_chats);

        viewModel.setTwoPane(findViewById(R.id.frame_messages) != null);

        // Manage the fragments
        Fragment listChatFragment = getSupportFragmentManager().findFragmentByTag(TAG_MASTER_FRAGMENT);
        if (listChatFragment == null) {
            listChatFragment = new ListChatFragment();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_chats, listChatFragment, TAG_MASTER_FRAGMENT).commit();

        Fragment listMessageFragment1 = getSupportFragmentManager().findFragmentByTag(TAG_DETAIL_FRAGMENT);
        if (listMessageFragment1 != null) {
            getSupportFragmentManager().beginTransaction().remove(listMessageFragment1).commit();
            ListMessageFragment listMessageFragment = new ListMessageFragment();
            if (viewModel.isTwoPane()) {
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_messages, listMessageFragment, TAG_DETAIL_FRAGMENT).commit();
            } else {
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_chats, listMessageFragment, TAG_DETAIL_FRAGMENT).addToBackStack(null).commit();
            }
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

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
