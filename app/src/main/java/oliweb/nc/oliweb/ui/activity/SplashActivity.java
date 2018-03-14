package oliweb.nc.oliweb.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import oliweb.nc.oliweb.helper.SharedPreferencesHelper;


@SuppressWarnings("squid:MaximumInheritanceDepth")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isFirstTime = SharedPreferencesHelper.getInstance(this).isFirstTime();

        if (isFirstTime) {
            Intent intent = new Intent(this, IntroActivity.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}