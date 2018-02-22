package oliweb.nc.oliweb;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.List;

import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.AnnonceWithPhotos;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceSearchDto;
import oliweb.nc.oliweb.network.elasticsearchDto.CategorieSearchDto;
import oliweb.nc.oliweb.network.elasticsearchDto.UtilisateurSearchDto;
import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;


public class Utility {

    private Utility() {
    }

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

        CategorieEntity categorieEntity = annonceFull.getCategorie().get(0);
        annonceSearchDto.setCategorie(new CategorieSearchDto(categorieEntity.getIdCategorie(), categorieEntity.getName()) );

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
