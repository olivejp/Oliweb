package oliweb.nc.oliweb;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.util.Pair;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.InputStream;

import oliweb.nc.oliweb.utility.MediaUtility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@RunWith(AndroidJUnit4.class)
public class DeletingFileTest {

    private Context context;

    @Before
    public void init() {
        context = InstrumentationRegistry.getTargetContext();
    }

    /**
     * Method tested :
     * - MediaUtility.createNewMediaFileUri()
     * - MediaUtility.saveInputStreamToContentProvider()
     * <p>
     * Create a new File Uri
     * Then save a file in this Uri
     * Then delete the file from the Uri
     */
    @Test
    public void createAndDeleteFile() {
        // Create a file Uri
        Pair<Uri, File> pair = MediaUtility.createNewMediaFileUri(context, false, MediaUtility.MediaType.IMAGE);
        context.grantUriPermission(context.getApplicationContext().getPackageName(), pair.first, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        assertNotNull(pair.first);
        assertNotNull(pair.second);

        // Save the image in the Content Provider
        InputStream inputStream = context.getApplicationContext().getResources().openRawResource(R.drawable.ic_access_time_grey_900_48dp);
        MediaUtility.saveInputStreamToContentProvider(inputStream, pair.second);

        // Call the delete method
        int delete = context.getContentResolver().delete(pair.first, null, null);

        // Check that the delete method returned 1 (number of record deleted)
        assertEquals(1, delete);
    }
}
