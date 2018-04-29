package oliweb.nc.oliweb.utility;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.firebase.ui.auth.AuthUI;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.ui.DialogInfos;
import oliweb.nc.oliweb.ui.GridSpacingItemDecoration;
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter;
import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;

import static oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment.TYPE_BOUTON_YESNO;

/**
 * Created by orlanth23 on 04/03/2018.
 */

public class Utility {

    private static final String TAG = Utility.class.getName();
    public static final String DIALOG_FIREBASE_RETRIEVE = "DIALOG_FIREBASE_RETRIEVE";

    public static void callLoginUi(AppCompatActivity activityCaller, int requestCode) {
        List<AuthUI.IdpConfig> listProviders = new ArrayList<>();
        listProviders.add(new AuthUI.IdpConfig.GoogleBuilder().build());
        listProviders.add(new AuthUI.IdpConfig.FacebookBuilder().build());
        listProviders.add(new AuthUI.IdpConfig.EmailBuilder().build());

        activityCaller.startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(listProviders)
                        .build(),
                requestCode);
    }

    public static void hideKeyboard(Context ctx) {
        InputMethodManager inputManager = (InputMethodManager) ctx
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        View v = ((Activity) ctx).getCurrentFocus();
        if (v == null)
            return;

        if (inputManager != null) {
            inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    public static List<String> statusToAvoid() {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, StatusRemote.TO_DELETE.getValue(), StatusRemote.DELETED.getValue(), StatusRemote.FAILED_TO_DELETE.getValue());
        return list;
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

    public static GridLayoutManager initGridLayout(Context context, RecyclerView recyclerView, AnnonceBeautyAdapter annonceBeautyAdapter) {
        GridLayoutManager gridLayoutManager;
        int spanCount;
        spanCount = Integer.valueOf(context.getString(R.string.span_count));
        gridLayoutManager = new GridLayoutManager(context, spanCount);
        if (spanCount > 2) {
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    switch (annonceBeautyAdapter.getItemViewType(position)) {
                        case 1:
                            return 1;
                        case 2:
                            return 1;
                        default:
                            return 1;
                    }
                }
            });
        }
        int spacing = context.getResources().getDimensionPixelSize(R.dimen.spacing_tight);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, true));
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
    public static void sendNotificationToRetreiveData(FragmentManager fragmentManager, NoticeDialogFragment.DialogListener listener) {
        DialogInfos dialogInfos = new DialogInfos();
        dialogInfos.setMessage("Des annonces vous appartenant ont été trouvées sur le réseau, voulez vous les récupérer sur votre appareil ?")
                .setButtonType(TYPE_BOUTON_YESNO)
                .setIdDrawable(R.drawable.ic_announcement_white_48dp)
                .setTag(DIALOG_FIREBASE_RETRIEVE);
        NoticeDialogFragment.sendDialog(fragmentManager, dialogInfos, listener);
    }

}
