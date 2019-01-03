package oliweb.nc.oliweb.utility;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.system.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.ui.DialogInfos;
import oliweb.nc.oliweb.ui.GridSpacingItemDecoration;
import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static androidx.recyclerview.widget.RecyclerView.HORIZONTAL;
import static oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment.TYPE_BOUTON_YESNO;

/**
 * Created by orlanth23 on 04/03/2018.
 */

public class Utility {

    private static final String TAG = Utility.class.getName();
    public static final String DIALOG_FIREBASE_RETRIEVE = "DIALOG_FIREBASE_RETRIEVE";
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

    private Utility() {
    }

    public static int getDefaultActionBarSize(Context context) {
        // Calculate ActionBar's height
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return 0;
    }

    public static boolean hasNavigationBar(Context context) {
        boolean hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        return !hasMenuKey && !hasBackKey;
    }

    public static Executor getNewExecutor() {
        return new ThreadPoolExecutor(NUMBER_OF_CORES * 2,
                NUMBER_OF_CORES * 2,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
    }

    public static void signOut(Context context) {
        AuthUI.getInstance()
                .signOut(context)
                .addOnCompleteListener(task -> {
                    SharedPreferencesHelper.getInstance(context).setUidFirebaseUser(null);
                    Toast.makeText(context, "Vous êtes déconnecté", Toast.LENGTH_SHORT).show();
                });
    }

    public static void signIn(AppCompatActivity appCompatActivity, int requestCode) {
        if (NetworkReceiver.checkConnection(appCompatActivity)) {
            Utility.callLoginUi(appCompatActivity, requestCode);
        } else {
            Toast.makeText(appCompatActivity, "Une connexion réseau est requise", Toast.LENGTH_LONG).show();
        }
    }

    public static void callLoginUi(AppCompatActivity activityCaller, int requestCode) {
        List<String> listPermissionsFacebook = new ArrayList<>(
                Arrays.asList("public_profile",
                        "email",
                        "user_friends",
                        "user_hometown",
                        "user_likes"));
        List<AuthUI.IdpConfig> listProviders = new ArrayList<>();
        listProviders.add(new AuthUI.IdpConfig.GoogleBuilder().build());
        listProviders.add(new AuthUI.IdpConfig.FacebookBuilder().setPermissions(listPermissionsFacebook).build());
        listProviders.add(new AuthUI.IdpConfig.EmailBuilder().build());

        activityCaller.startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(listProviders)
                        .setLogo(R.mipmap.ic_banana_launcher_foreground)
                        .setTheme(R.style.AppTheme)
                        .build(),
                requestCode);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager inputManager = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        View v = activity.getCurrentFocus();
        if (v == null)
            return;

        if (inputManager != null) {
            inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    public static List<String> allStatusToAvoid() {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, StatusRemote.TO_DELETE.getValue(), StatusRemote.DELETED.getValue(), StatusRemote.FAILED_TO_DELETE.getValue());
        return list;
    }

    public static List<String> allStatusToDelete() {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, StatusRemote.TO_DELETE.getValue(), StatusRemote.FAILED_TO_DELETE.getValue());
        return list;
    }

    public static List<String> allStatusToSend() {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, StatusRemote.TO_SEND.getValue(), StatusRemote.FAILED_TO_SEND.getValue());
        return list;
    }

    /**
     * Will return :
     *
     * @param context
     * @return 1 = Portrait ||  2 = Landscape
     */
    public static int getScreenOrientation(Context context) {
        WindowManager windowManager = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
        if (windowManager != null) {
            final int screenOrientation = windowManager.getDefaultDisplay().getRotation();
            switch (screenOrientation) {
                case Surface.ROTATION_0:
                    return 1;
                case Surface.ROTATION_90:
                    return 2;
                case Surface.ROTATION_180:
                    return 1;
                case Surface.ROTATION_270:
                    return 2;
                default:
                    return 1;
            }
        }
        return 1;
    }

    /**
     * Va renvoyer la date du jour au format yyyyMMddHHmmss en Long
     *
     * @return Long
     */
    public static Long getNowInEntityFormat() {
        Calendar cal = Calendar.getInstance();
        return Long.parseLong(DateConverter.simpleEntityDateFormat.format(cal.getTime()));
    }

    /**
     * Va renvoyer la date du jour au format dd/MM/yyyy HH:mm:ss en String
     *
     * @return String
     */
    public static String getNowDto() {
        Calendar cal = Calendar.getInstance();
        return DateConverter.simpleDtoDateFormat.format(cal.getTime());
    }

    /**
     * @param timestamp
     * @return
     */
    public static String howLongFromNow(Long timestamp) {
        if (timestamp != null) {
            return howLongFromNow(DateConverter.simpleDtoDateFormat.format(new Date(timestamp)));
        }
        return null;
    }

    /**
     * @param dateDto
     * @return
     */
    private static String howLongFromNow(String dateDto) {
        if (dateDto == null) {
            return null;
        }
        try {
            Date now = Calendar.getInstance().getTime();
            Date date = DateConverter.simpleDtoDateFormat.parse(dateDto);
            long duration = now.getTime() - date.getTime();

            long nbSecond = duration / 1000;
            long nbMinute = nbSecond / 60;
            long nbHour = nbMinute / 60;
            long nbDay = nbHour / 24;
            long nbWeek = nbDay / 7;
            long nbMonth = nbDay / 30;
            long nbYear = nbMonth / 12;

            // On est au dessus de l'année on affiche les années
            if (nbYear >= 1) {
                return String.valueOf(nbYear).concat(" années");
            }
            // On est en dessous de l'année on affiche les mois
            if (nbMonth >= 1) {
                return String.valueOf(nbMonth).concat(" mois");
            }
            // On est en dessous du mois on affiche les semaines
            if (nbWeek >= 1) {
                return String.valueOf(nbWeek).concat(" sem");
            }
            // On est en dessous de la semaine on affiche les jours
            if (nbDay >= 1) {
                return String.valueOf(nbDay).concat(" j");
            }
            // On est en dessous de la journée on affiche les heures
            if (nbHour >= 1) {
                return String.valueOf(nbHour).concat(" hr");
            }
            // On est en dessous de l'heure on affiche les minutes
            if (nbMinute >= 1) {
                return String.valueOf(nbMinute).concat(" min");
            }
            // On est inférieur à la minute, on affiche les secondes
            return String.valueOf(nbSecond).concat(" sec");
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param stringDate
     * @param patternOrigin
     * @return
     */
    public static long howLongSince(@NonNull String stringDate, @NonNull DateConverter.DatePattern patternOrigin) throws ParseException {

        Date dateConverted = null;
        long duration = 0L;

        switch (patternOrigin) {
            case PATTERN_AFTER_HIP:
                dateConverted = DateConverter.simpleAfterShipDateFormat.parse(stringDate);
                break;
            case PATTERN_DTO:
                dateConverted = DateConverter.simpleDtoDateFormat.parse(stringDate);
                break;
            case PATTERN_UI:
                dateConverted = DateConverter.simpleUiDateFormat.parse(stringDate);
                break;
            case PATTERN_ENTITY:
                dateConverted = DateConverter.simpleEntityDateFormat.parse(stringDate);
                break;
        }

        if (dateConverted != null) {
            Date now = Calendar.getInstance().getTime();
            duration = now.getTime() - dateConverted.getTime();
        }

        return duration;
    }

    public static GridLayoutManager initGridLayout(Context context, RecyclerView recyclerView) {

        // Récupération du nombre de colonne à partir de FirebaseRemoteConfig.
        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        int spanCount;
        if (getScreenOrientation(context) == 1) {
            spanCount = (int) firebaseRemoteConfig.getLong(Constants.REMOTE_COLUMN_NUMBER);
        } else {
            spanCount = (int) firebaseRemoteConfig.getLong(Constants.REMOTE_COLUMN_NUMBER_LANDSCAPE);
        }

        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, spanCount);
        if (spanCount >= 2) {
            int spacing = context.getResources().getDimensionPixelSize(R.dimen.spacing_extra_small);
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return 1;
                }
            });
            recyclerView.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, true));
        } else {
            // TODO Ajout d'un divider, non operationnel pour le moment
            recyclerView.addItemDecoration(new DividerItemDecoration(context, HORIZONTAL));
        }
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setHasFixedSize(false);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        return gridLayoutManager;
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    // TODO Permettre de paramétrer cette méthode
    public static void sendNotificationToRetreiveData(FragmentManager fragmentManager, NoticeDialogFragment.DialogListener listener, String textToDisplay) {
        DialogInfos dialogInfos = new DialogInfos();
        dialogInfos.setMessage(textToDisplay)
                .setButtonType(TYPE_BOUTON_YESNO)
                .setIdDrawable(R.drawable.ic_announcement_white_48dp)
                .setTag(DIALOG_FIREBASE_RETRIEVE);
        NoticeDialogFragment.sendDialog(fragmentManager, dialogInfos, listener);
    }

    public static int getNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }
}
