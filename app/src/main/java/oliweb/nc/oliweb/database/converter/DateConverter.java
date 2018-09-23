package oliweb.nc.oliweb.database.converter;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by 2761oli on 23/10/2017.
 */

public class DateConverter {
    private static final String TAG = DateConverter.class.getName();

    public enum DatePattern {
        PATTERN_DTO("dd/MM/yyyy HH:mm:ss"),
        PATTERN_ENTITY("yyyyMMddHHmmssSSS"),
        PATTERN_UI("dd MMM yyyy à HH:mm"),
        PATTERN_UI_MESSAGE("dd MMM yy HH:mm"),
        PATTERN_AFTER_HIP("yyyy-MM-dd'T'HH:mm:ss");

        private final String value;

        DatePattern(String value) {
            this.value = value;
        }

        public String getDatePattern() {
            return this.value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static final SimpleDateFormat simpleDtoDateFormat = new SimpleDateFormat(DatePattern.PATTERN_DTO.getDatePattern(), Locale.FRANCE);
    public static final SimpleDateFormat simpleUiDateFormat = new SimpleDateFormat(DatePattern.PATTERN_UI.getDatePattern(), Locale.FRANCE);
    public static final SimpleDateFormat simpleUiMessageDateFormat = new SimpleDateFormat(DatePattern.PATTERN_UI_MESSAGE.getDatePattern(), Locale.FRANCE);
    public static final SimpleDateFormat simpleEntityDateFormat = new SimpleDateFormat(DatePattern.PATTERN_ENTITY.getDatePattern(), Locale.FRANCE);
    public static final SimpleDateFormat simpleAfterShipDateFormat = new SimpleDateFormat(DatePattern.PATTERN_AFTER_HIP.getDatePattern(), Locale.FRANCE);

    private DateConverter() {
    }

    /**
     * Transformation d'une date de type yyyyMMddHHmmss vers le format dd MMM yy HH:mm
     *
     * @param dateEntity
     * @return
     */
    public static String convertDateEntityToUi(Long dateEntity) {
        if (dateEntity != null) {
            try {
                return simpleUiDateFormat.format(simpleEntityDateFormat.parse(String.valueOf(dateEntity)));
            } catch (ParseException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Transformation d'une date de type dd/MM/yyyy HH:mm:ss vers le format yyyyMMddHHmmss en Long
     *
     * @param dateDto
     * @return Long
     */
    public static Long convertDateDtoToEntity(String dateDto) {
        try {
            String dateConverted = simpleEntityDateFormat.format(simpleDtoDateFormat.parse(dateDto));
            return Long.parseLong(dateConverted);
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (NullPointerException e1) {
            Log.e(TAG, e1.getMessage(), e1);
        }
        return 0L;
    }

    /**
     * Transformation d'une date de type yyyy-MM-ddTHH:mm:ss vers le format yyyyMMddHHmmss en Long
     *
     * @param dateAferShip
     * @return
     */
    public static Long convertDateAfterShipToEntity(String dateAferShip) {
        try {
            String dateConverted = simpleEntityDateFormat.format(simpleAfterShipDateFormat.parse(dateAferShip));
            return Long.parseLong(dateConverted);
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (NullPointerException e1) {
            Log.e(TAG, e1.getMessage(), e1);
        }
        return 0L;
    }

    /**
     * Transformation d'une date de type yyyyMMddHHmmss vers le format dd/MM/yyyy HH:mm:ss en Long
     *
     * @param dateEntity
     * @return String
     */
    public static String convertDateEntityToDto(Long dateEntity) {
        try {
            Date dateConverted = simpleEntityDateFormat.parse(String.valueOf(dateEntity));
            return simpleDtoDateFormat.format(dateConverted);
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Transformation d'une date de type yyyyMMddHHmmss vers le format dd/MM/yyyy HH:mm:ss en Long
     *
     * @param dateEntity
     * @return String
     */
    public static String convertDateToUiDate(Long dateEntity) {
        return simpleUiMessageDateFormat.format(new Date(dateEntity));
    }

}
