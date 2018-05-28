package oliweb.nc.oliweb.service.sharing;

import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;

import static oliweb.nc.oliweb.utility.Constants.OLIWEB_ANNONCE_VIEW_PATH;
import static oliweb.nc.oliweb.utility.Constants.OLIWEB_DYNAMIC_LINK_DOMAIN;
import static oliweb.nc.oliweb.utility.Constants.OLIWEB_SITE;

/**
 * Created by orlanth23 on 24/05/2018.
 */
public class DynamicLynksGenerator {

    private static String generateLink(String uidUser, String uidAnnonce) {
        return OLIWEB_SITE + OLIWEB_ANNONCE_VIEW_PATH + uidAnnonce + "?from=" + uidUser;
    }

    /**
     * @param uidUser
     * @param uidAnnonce
     * @param dynamicLinkListener
     * @return
     */
    public static Task<ShortDynamicLink> generateShortLink(String uidUser, String uidAnnonce, DynamicLinkListener dynamicLinkListener) {
        String generatedLink = generateLink(uidUser, uidAnnonce);
        return FirebaseDynamicLinks.getInstance()
                .createDynamicLink()
                .setLink(Uri.parse(generatedLink))
                .setDynamicLinkDomain(OLIWEB_DYNAMIC_LINK_DOMAIN)
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder().build())
                .setIosParameters(new DynamicLink.IosParameters.Builder(generatedLink).build())
                .buildShortDynamicLink()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Uri shortLink = task.getResult().getShortLink();
                        Uri flowchartLink = task.getResult().getPreviewLink();
                        dynamicLinkListener.getLink(shortLink, flowchartLink);
                    } else {
                        dynamicLinkListener.getLinkError();
                    }
                });
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
