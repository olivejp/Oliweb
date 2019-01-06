package oliweb.nc.oliweb;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MediaUtilityTest {

    private Context appContext;

    @Before
    public void init() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    @After
    public void reset() {

    }

    @Test
    public void testOnImages() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            InputStream inputStream = appContext.getResources().getAssets().open("clothing_leather_wooden.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos);
            byte[] bitmapdata = bos.toByteArray();
            System.out.println("Longeur JPEG " + bitmapdata.length);
            assertTrue(bitmapdata.length > 0);

            ByteArrayOutputStream bosPng = new ByteArrayOutputStream();
            InputStream inputStreamPng = appContext.getResources().getAssets().open("clothing_leather_wooden.png");
            Bitmap bitmapPng = BitmapFactory.decodeStream(inputStreamPng);
            bitmapPng.compress(Bitmap.CompressFormat.PNG, 50, bosPng);
            byte[] bitmapdataPng = bosPng.toByteArray();
            System.out.println("Longeur PNG " + bitmapdataPng.length);
            assertTrue(bitmapdataPng.length > 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
