package oliweb.nc.oliweb.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by orlanth23 on 10/02/2018.
 */

public class SharedPreferencesHelper {

    private static final String PREF_DISPLAY_MODE = "PREF_DISPLAY_MODE";
    private static final String PREF_FIREBASE_USER_UID = "DISPLAY_FIREBASE_USER_UID";
    private static final String PREF_USE_EXTERNAL_STORAGE = "PREF_USE_EXTERNAL_STORAGE";
    private static final String PREF_DISPLAY_GRID = "PREF_DISPLAY_GRID";
    private static final String PREF_FIRST_TIME = "PREF_FIRST_TIME";
    private static final String PREF_SORT = "PREF_SORT";
    public static final int PREF_VALUE_DISPLAY_MODE_RAW = 500;
    public static final int PREF_VALUE_DISPLAY_MODE_BEAUTY = 600;


    private static SharedPreferencesHelper INSTANCE;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public static SharedPreferencesHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SharedPreferencesHelper(context);
        }
        return INSTANCE;
    }

    private SharedPreferencesHelper(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public SharedPreferences.Editor getEditor() {
        return editor;
    }

    public boolean isFirstTime() {
        return sharedPreferences.getBoolean(PREF_FIRST_TIME, true);
    }

    public void setFirstTime(boolean firstTime) {
        editor.putBoolean(PREF_FIRST_TIME, firstTime).commit();
    }

    public boolean getDisplayBeautyMode() {
        return sharedPreferences.getBoolean(PREF_DISPLAY_MODE, true);
    }

    public boolean getUseExternalStorage() {
        return sharedPreferences.getBoolean(PREF_USE_EXTERNAL_STORAGE, false);
    }

    public boolean setDisplayBeautyMode(boolean value) {
        return editor.putBoolean(PREF_DISPLAY_MODE, value).commit();
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

    public boolean getGridMode() {
        return sharedPreferences.getBoolean(PREF_DISPLAY_GRID, true);
    }

    public boolean setGridMode(boolean gridMode) {
        return editor.putBoolean(PREF_DISPLAY_GRID, gridMode).commit();
    }

    public int getPrefSort() {
        return sharedPreferences.getInt(PREF_SORT, 0);
    }

    public boolean setPrefSort(int sort) {
        return editor.putInt(PREF_SORT, sort).commit();
    }
}
