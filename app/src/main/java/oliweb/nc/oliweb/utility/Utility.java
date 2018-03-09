package oliweb.nc.oliweb.utility;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import oliweb.nc.oliweb.database.converter.DateConverter;

/**
 * Created by orlanth23 on 04/03/2018.
 */

public class Utility {

    private static final String TAG = Utility.class.getName();

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
     * @param dateEntity
     * @return
     */
    public static String howLongFromNow(Long dateEntity) {
        if (dateEntity != null) {
            return howLongFromNow(DateConverter.convertDateEntityToDto(dateEntity));
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
}