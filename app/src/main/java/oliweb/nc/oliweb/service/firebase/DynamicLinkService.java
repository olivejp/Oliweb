package oliweb.nc.oliweb.service.firebase;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.dynamiclinks.DynamicLink;

import androidx.annotation.UiThread;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.service.sharing.DynamicLinksGenerator;
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment;

/**
 * Cette classe permet de lancer l'envoi d'un DynamicLink
 */
public class DynamicLinkService {

    private static final String TAG = DynamicLinkService.class.getName();

    private DynamicLinkService() {
        // Empty construstor
    }

    @UiThread
    public static void shareDynamicLink(Context context, AnnonceFull annonceFull, String uidUser, LoadingDialogFragment loadingDialogFragment, View snackBarAnchor) {
        DynamicLink link = DynamicLinksGenerator.generateLong(uidUser, annonceFull.getAnnonce(), annonceFull.getPhotos());
        DynamicLinksGenerator.generateShortWithLong(link.getUri(), new DynamicLinksGenerator.DynamicLinkListener() {
            @Override
            public void getLink(Uri shortLink, Uri flowchartLink) {
                loadingDialogFragment.dismiss();
                Intent sendIntent = new Intent();
                String msg = context.getString(R.string.default_text_share_link) + shortLink;
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
                sendIntent.setType("text/plain");
                context.startActivity(Intent.createChooser(sendIntent, "Partager avec"));
            }

            @Override
            public void getLinkError() {
                loadingDialogFragment.dismiss();
                Snackbar.make(snackBarAnchor, R.string.link_share_error, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
