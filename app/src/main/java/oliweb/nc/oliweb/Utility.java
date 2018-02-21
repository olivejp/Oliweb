package oliweb.nc.oliweb;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.AnnonceWithPhotos;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceSearchDto;
import oliweb.nc.oliweb.network.elasticsearchDto.UtilisateurSearchDto;
import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;


public class Utility {

    //Email Pattern
    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    /**
     * @param fragmentManager Get from the context
     * @param message         The message to be send
     * @param type            From NoticeDialogFragment
     * @param idDrawable      From NoticeDialogFragment
     * @param tag             A text to be a tag
     */
    public static void sendDialogByFragmentManagerWithRes(FragmentManager fragmentManager, String message, int type, @DrawableRes int idDrawable, @Nullable String tag, @Nullable Bundle bundlePar, NoticeDialogFragment.DialogListener listener) {
        NoticeDialogFragment dialogErreur = new NoticeDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NoticeDialogFragment.P_MESSAGE, message);
        bundle.putInt(NoticeDialogFragment.P_TYPE, type);
        bundle.putInt(NoticeDialogFragment.P_IMG, idDrawable);
        bundle.putBundle(NoticeDialogFragment.P_BUNDLE, bundlePar);
        dialogErreur.setListener(listener);
        dialogErreur.setArguments(bundle);
        dialogErreur.show(fragmentManager, tag);
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

    /**
     * @param annonceSearchDto
     * @return
     */
    public static AnnonceWithPhotos convertDtoToEntity(AnnonceSearchDto annonceSearchDto) {
        AnnonceWithPhotos annonceWithPhotos = new AnnonceWithPhotos();
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annonceWithPhotos.setPhotos(new ArrayList<>());

        annonceEntity.setUUID(annonceSearchDto.getUuid());
        annonceEntity.setTitre(annonceSearchDto.getTitre());
        annonceEntity.setDescription(annonceSearchDto.getDescription());
        // annonceEntity.setIdCategorie(annonceSearchDto.getCategorie().getId());
        annonceEntity.setDatePublication(annonceSearchDto.getDatePublication());
        annonceEntity.setPrix(annonceSearchDto.getPrix());

        annonceWithPhotos.setAnnonceEntity(annonceEntity);

        return annonceWithPhotos;
    }

    /**
     * @param annonceFull
     * @return
     */
    public static AnnonceSearchDto convertEntityToDto(AnnonceFull annonceFull) {
        AnnonceSearchDto annonceSearchDto = new AnnonceSearchDto();
        UtilisateurEntity utilisateurEntity = annonceFull.getUtilisateur().get(0);
        UtilisateurSearchDto utilisateurSearchDto = new UtilisateurSearchDto(utilisateurEntity.getProfile(), utilisateurEntity.getUuidUtilisateur(), utilisateurEntity.getTelephone(), utilisateurEntity.getEmail());
        annonceSearchDto.setUtilisateur(utilisateurSearchDto);

        annonceSearchDto.setCategorie(annonceFull.getCategorie().get(0).getName());

        annonceSearchDto.setDatePublication(annonceFull.getAnnonce().getDatePublication());
        annonceSearchDto.setDescription(annonceFull.getAnnonce().getDescription());
        annonceSearchDto.setTitre(annonceFull.getAnnonce().getTitre());
        annonceSearchDto.setPrix(annonceFull.getAnnonce().getPrix());
        annonceSearchDto.setUuid(annonceFull.getAnnonce().getUUID());

        List<String> listPhotoDto = new ArrayList<>();
        for (PhotoEntity photo : annonceFull.getPhotos()) {
            listPhotoDto.add(photo.getFirebasePath());
        }
        annonceSearchDto.setPhotos(listPhotoDto);

        return annonceSearchDto;
    }
}
