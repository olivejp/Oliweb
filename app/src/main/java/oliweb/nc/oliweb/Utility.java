package oliweb.nc.oliweb;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;


public class Utility {

    public static final String DIALOG_TAG_UNREGISTER = "UNREGISTER";

    //Email Pattern
    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public static boolean isTextViewOnError(boolean condition, TextView textView, String msgError, boolean requestFocus) {
        if (condition) {
            textView.setError(msgError);
            if (requestFocus) {
                textView.requestFocus();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Only check if the string contains an @
     *
     * @param email to check
     * @return false if password valid, true otherwise
     */
    public static boolean validateEmail(String email) {
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);

        return matcher.matches() && email.contains("@");
    }

    /**
     * Password should contains at least 6 caracters
     *
     * @param password to check
     * @return false if password valid, true otherwise
     */
    public static boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }

    public static boolean isNotNull(String txt) {
        return txt != null && txt.trim().length() > 0;
    }

    /**
     * Check WiFi connection
     *
     * @param context
     * @return
     */
    private static boolean isWifiActivated(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null) {
            NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        } else {
            return false;
        }
    }

    /**
     * Check 3G connection
     *
     * @param context
     * @return
     */
    private static boolean is3GActivated(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null) {
            NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
        } else {
            return false;
        }
    }

    private static int getColorFromString(String color) {
        String colorComplete = "#".concat(color);
        return Color.parseColor(colorComplete);
    }

    public static int getColorFromInteger(Integer color) {
        String colorString = color.toString();
        return getColorFromString(colorString);
    }

    /**
     * Récupération des préférences du nombre de caractère
     *
     * @param context
     * @return
     */
    public static int getPrefNumberCar(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String number = sharedPrefs.getString("pref_key_number_car", context.getResources().getString(R.string.pref_default_number_car));
        return Integer.valueOf(number);
    }

    /**
     * @param fragmentManager Get from the context
     * @param message         The message to be send
     * @param type            From NoticeDialogFragment
     * @param img             From NoticeDialogFragment
     * @param tag             A text to be a TAG
     */
    public static void SendDialogByFragmentManager(FragmentManager fragmentManager, String message, int type, int img, @Nullable String tag) {
        NoticeDialogFragment dialogErreur = new NoticeDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NoticeDialogFragment.P_MESSAGE, message);
        bundle.putInt(NoticeDialogFragment.P_TYPE, type);
        bundle.putInt(NoticeDialogFragment.P_IMG, img);
        dialogErreur.setArguments(bundle);
        dialogErreur.show(fragmentManager, tag);
    }

    // Envoi d'un message
    public static void SendDialogByActivity(AppCompatActivity activity, String message, int type, int img, String tag) {
        SendDialogByFragmentManager(activity.getSupportFragmentManager(), message, type, img, tag);
    }

    public static String convertDate(String dateYMDHM) {
        SimpleDateFormat originalDateFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.FRENCH);
        SimpleDateFormat newDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH);
        Date dateOriginal = null;
        try {
            dateOriginal = originalDateFormat.parse(dateYMDHM);
        } catch (ParseException e) {
            Log.e("convertDate", e.getMessage());
        }
        return newDateFormat.format(dateOriginal);
    }

    public static String convertPrice(Integer prix) {
        return NumberFormat.getNumberInstance(Locale.FRENCH).format(prix) + " " + Constants.CURRENCY;
    }

    public static boolean checkWifiAndMobileData(Context context) {
        return (Utility.isWifiActivated(context) || Utility.is3GActivated(context));
    }

    public static void hideKeyboard(Context ctx) {
        InputMethodManager inputManager = (InputMethodManager) ctx
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        View v = ((Activity) ctx).getCurrentFocus();
        if (v == null)
            return;

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}
