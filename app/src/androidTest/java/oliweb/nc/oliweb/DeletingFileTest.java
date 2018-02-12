package oliweb.nc.oliweb;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import oliweb.nc.oliweb.media.MediaUtility;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@RunWith(AndroidJUnit4.class)
public class DeletingFileTest {

    private Context context;

    @Before
    public void createDb() {
        context = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void writeCategorie() throws Exception {
        File myNewFile = MediaUtility.createExternalMediaFile("TEST_A_JPO");
        context.deleteFile(myNewFile.getName());
    }
}
