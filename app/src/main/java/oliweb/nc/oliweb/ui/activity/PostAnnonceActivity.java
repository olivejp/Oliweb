package oliweb.nc.oliweb.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import oliweb.nc.oliweb.R;

public class PostAnnonceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_annonce);
    }

    private void saveAnnonce(){

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.bottom_post_save) {
            saveAnnonce();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
