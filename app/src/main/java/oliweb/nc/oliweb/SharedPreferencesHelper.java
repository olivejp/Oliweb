package oliweb.nc.oliweb;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by orlanth23 on 10/02/2018.
 */

public class SharedPreferencesHelper {

    private static SharedPreferencesHelper INSTANCE;
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    public static SharedPreferencesHelper getInstance(Context context, String sharedPreferenceName){
        if (INSTANCE == null) {
            INSTANCE = new SharedPreferencesHelper(context, sharedPreferenceName);
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
}
