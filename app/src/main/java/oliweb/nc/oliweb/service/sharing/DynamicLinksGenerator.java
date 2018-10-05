package oliweb.nc.oliweb.service.sharing;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;

import java.util.List;

import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;

import static oliweb.nc.oliweb.utility.Constants.OLIWEB_ANNONCE_VIEW_PATH;
import static oliweb.nc.oliweb.utility.Constants.OLIWEB_DYNAMIC_LINK_DOMAIN;
import static oliweb.nc.oliweb.utility.Constants.OLIWEB_SITE;

/**
 * Created by orlanth23 on 24/05/2018.
 */
public class DynamicLinksGenerator {

    private static final String TAG = DynamicLinksGenerator.class.getCanonicalName();

    private static String generateLink(String uidUser, String uidAnnonce) {
        return OLIWEB_SITE + OLIWEB_ANNONCE_VIEW_PATH + uidAnnonce + "?from=" + uidUser;
    }

    /**
     * @param uidUser
     * @param annonce
     * @param dynamicLinkListener
     * @return
     */
    public static void generateShortLink(String uidUser, AnnonceEntity annonce, List<PhotoEntity> listPhoto, DynamicLinkListener dynamicLinkListener) {
        String generatedLink = generateLink(uidUser, annonce.getUid());
        Uri photoUri = null;

        if (listPhoto != null && !listPhoto.isEmpty()) {
            photoUri = Uri.parse(listPhoto.get(0).getFirebasePath());
        }

        FirebaseDynamicLinks.getInstance()
                .createDynamicLink()
                .setLink(Uri.parse(generatedLink))
                .setDynamicLinkDomain(OLIWEB_DYNAMIC_LINK_DOMAIN)
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder().build())
                .setIosParameters(new DynamicLink.IosParameters.Builder(generatedLink).build())
                .setSocialMetaTagParameters(new DynamicLink.SocialMetaTagParameters.Builder()
                        .setTitle(annonce.getTitre())
                        .setDescription(annonce.getDescription())
                        .setImageUrl(photoUri)
                        .build())
                .buildShortDynamicLink()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Uri shortLink = task.getResult().getShortLink();
                        Uri flowchartLink = task.getResult().getPreviewLink();
                        dynamicLinkListener.getLink(shortLink, flowchartLink);
                    } else {
                        dynamicLinkListener.getLinkError();
                    }
                }).addOnFailureListener(e -> Log.e(TAG, e.getLocalizedMessage(), e)
        );
    }

    /**
     * @param uidUser
     * @param uidAnnonce
     * @return
     */
    public static Uri generateLong(String uidUser, String uidAnnonce) {
        String generatedLink = generateLink(uidUser, uidAnnonce);
        DynamicLink dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse(generatedLink))
                .setDynamicLinkDomain(OLIWEB_DYNAMIC_LINK_DOMAIN)
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder().build())
                .setIosParameters(new DynamicLink.IosParameters.Builder(generatedLink).build())
                .buildDynamicLink();

        return dynamicLink.getUri();
    }

    public interface DynamicLinkListener {
        void getLink(Uri shortLink, Uri flowchartLink);

        void getLinkError();
    }
}
