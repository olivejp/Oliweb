package oliweb.nc.oliweb;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by orlanth23 on 10/02/2018.
 */

public class SharedPreferencesHelper {

    private static final String SHARED_PREFERENCE_NAME = "OLIWEB";
    private static final String PREF_DISPLAY_MODE = "DISPLAY_MODE";
    private static final String PREF_FIREBASE_USER_UID = "DISPLAY_FIREBASE_USER_UID";
    private static final String PREF_USE_EXTERNAL_STORAGE = "PREF_USE_EXTERNAL_STORAGE";
    public static final int PREF_VALUE_DISPLAY_MODE_RAW = 500;
    public static final int PREF_VALUE_DISPLAY_MODE_BEAUTY = 600;


    private static SharedPreferencesHelper INSTANCE;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public static SharedPreferencesHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SharedPreferencesHelper(context, SHARED_PREFERENCE_NAME);
        }
        return INSTANCE;
    }

    private SharedPreferencesHelper(Context context, String sharedPreferenceName) {
        sharedPreferences = context.getSharedPreferences(sharedPreferenceName, Context.MODE_PRIVATE);
        editor = context.getSharedPreferences(sharedPreferenceName, Context.MODE_PRIVATE).edit();
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public SharedPreferences.Editor getEditor() {
        return editor;
    }

    public int getDisplayMode() {
        return sharedPreferences.getInt(PREF_DISPLAY_MODE, PREF_VALUE_DISPLAY_MODE_RAW);
    }

    public boolean getUseExternalStorage() {
        return sharedPreferences.getBoolean(PREF_USE_EXTERNAL_STORAGE, false);
    }

    public boolean setDisplayMode(int value) {
        return editor.putInt(PREF_DISPLAY_MODE, value).commit();
    }

    public boolean setUseExternalStorage(boolean useExternalStorage) {
        return editor.putBoolean(PREF_USE_EXTERNAL_STORAGE, useExternalStorage).commit();
    }

    public String getUidFirebaseUser() {
        return sharedPreferences.getString(PREF_FIREBASE_USER_UID, null);
    }

    public boolean setUidFirebaseUser(String uidFirebaseUser) {
        return editor.putString(PREF_FIREBASE_USER_UID, uidFirebaseUser).commit();
    }
}
