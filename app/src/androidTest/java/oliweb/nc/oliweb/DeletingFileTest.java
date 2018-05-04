package oliweb.nc.oliweb;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.util.Pair;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import oliweb.nc.oliweb.utility.MediaUtility;

/**
 * Created by orlanth23 on 28/01/2018.
 * TODO finir ce test
 */
@RunWith(AndroidJUnit4.class)
public class DeletingFileTest {

    private Context context;

    @Before
    public void init() {
        context = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void createAndDeleteFile() throws IOException {
        Pair<Uri, File> pair = MediaUtility.createNewMediaFileUri(context, false, MediaUtility.MediaType.IMAGE);
        context.grantUriPermission(context.getApplicationContext().getPackageName(), pair.first, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Assert.assertNotNull(pair.first);
        Assert.assertNotNull(pair.second);

        // Sauvegarde d'une image dans le Content Provider
        InputStream inputStream = context.getApplicationContext().getResources().openRawResource(R.drawable.beast);
        MediaUtility.saveInputStreamToContentProvider(inputStream, pair.second);

        int delete = context.getContentResolver().delete(pair.first, null, null);
        Assert.assertEquals(1, delete);
    }
}
