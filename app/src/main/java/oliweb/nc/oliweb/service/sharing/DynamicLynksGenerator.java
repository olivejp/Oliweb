package oliweb.nc.oliweb.service.sharing;

import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;

import static oliweb.nc.oliweb.utility.Constants.OLIWEB_SITE;

/**
 * Created by orlanth23 on 24/05/2018.
 */
public class DynamicLynksGenerator {
    public static Task<ShortDynamicLink> generate(String uidUser, String uidAnnonce, DynamicLinkListener dynamicLinkListener) {
        return FirebaseDynamicLinks.getInstance()
                .createDynamicLink()
                .setLink(Uri.parse(OLIWEB_SITE + "/annonces/view/" + uidAnnonce + "?from=" + uidUser))
                .setDynamicLinkDomain("g2gb6.app.goo.gl")
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder().build())
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

    public static Uri generateLong(String uidUser, String uidAnnonce) {
        DynamicLink dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse(OLIWEB_SITE + "/annonces/view/" + uidAnnonce + "?from=" + uidUser))
                .setDynamicLinkDomain("g2gb6.app.goo.gl")
                // Open links with this app on Android
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder().build())
                // Open links with com.example.ios on iOS
                .setIosParameters(new DynamicLink.IosParameters.Builder(OLIWEB_SITE + "/annonces/view/" + uidAnnonce + "?from=" + uidUser).build())
                .buildDynamicLink();

        return dynamicLink.getUri();
    }

    public interface DynamicLinkListener {
        void getLink(Uri shortLink, Uri flowchartLink);

        void getLinkError();
    }
}
