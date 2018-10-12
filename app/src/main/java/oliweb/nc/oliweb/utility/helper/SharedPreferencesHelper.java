package oliweb.nc.oliweb.utility.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

/**
 * Created by orlanth23 on 10/02/2018.
 */

public class SharedPreferencesHelper {

    private static final String NOTIFICATIONS_NEW_MESSAGE = "notifications_new_message";
    private static final String NOTIFICATIONS_NEW_MESSAGE_RINGTONE = "notifications_new_message_ringtone";
    private static final String PREF_FIREBASE_USER_UID = "DISPLAY_FIREBASE_USER_UID";
    private static final String PREF_USE_EXTERNAL_STORAGE = "PREF_USE_EXTERNAL_STORAGE";
    private static final String PREF_DISPLAY_GRID = "PREF_DISPLAY_GRID";
    private static final String PREF_FIRST_TIME = "PREF_FIRST_TIME";
    private static final String PREF_SORT = "PREF_SORT";
    private static final String PREF_RETRIEVE_PREVIOUS_ANNONCES = "PREF_RETRIEVE_PREVIOUS_ANNONCES";


    private static SharedPreferencesHelper INSTANCE;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public static synchronized SharedPreferencesHelper getInstance(Context context) {
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

    public boolean getUseExternalStorage() {
        return sharedPreferences.getBoolean(PREF_USE_EXTERNAL_STORAGE, false);
    }

    public boolean getNotificationsMessage() {
        return sharedPreferences.getBoolean(NOTIFICATIONS_NEW_MESSAGE, false);
    }

    public Uri getNotificationsMessageRingtone() {
        return Uri.parse(sharedPreferences.getString(NOTIFICATIONS_NEW_MESSAGE_RINGTONE, "content://settings/system/notification_sound"));
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

    public boolean getRetrievePreviousAnnonces() {
        return sharedPreferences.getBoolean(PREF_RETRIEVE_PREVIOUS_ANNONCES, true);
    }

    public boolean setRetrievePreviousAnnonces(boolean retrieve) {
        return editor.putBoolean(PREF_RETRIEVE_PREVIOUS_ANNONCES, retrieve).commit();
    }

    public int getPrefSort() {
        return sharedPreferences.getInt(PREF_SORT, 0);
    }

    public boolean setPrefSort(int sort) {
        return editor.putInt(PREF_SORT, sort).commit();
    }
}
