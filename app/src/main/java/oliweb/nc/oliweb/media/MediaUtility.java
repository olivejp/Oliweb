package oliweb.nc.oliweb.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.util.Pair;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import oliweb.nc.oliweb.BuildConfig;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.ui.activity.PostAnnonceActivity;

/**
 * Created by orlanth23 on 03/02/2018.
 */

public class MediaUtility {

    private static final String TAG = PostAnnonceActivity.class.getName();

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }


    /**
     * Force a refresh of media content provider for specific item
     *
     * @param fileName
     */
    public static void refreshMediaProvider(Context appContext, String fileName) {
        MediaScannerConnection scanner = null;
        try {
            scanner = new MediaScannerConnection(appContext, null);
            scanner.connect();
            try {
                Thread.sleep(200);
            } catch (Exception e) {
            }
            if (scanner.isConnected()) {
                Log.d(TAG, "Requesting scan for file " + fileName);
                scanner.scanFile(fileName, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot to scan file", e);
        } finally {
            if (scanner != null) {
                scanner.disconnect();
            }
        }
    }

    /**
     * Redimenssionne une image pour qu'elle corresponde aux limitations.
     * On ne fait que de la diminution d'image pas d'agrandissement.
     *
     * @param bitmap La bitmap a redimenssionné
     * @param maxPx  Nombre de pixel maximum de l'image (autant en largeur ou hauteur)
     * @return Bitmap redimenssionné
     */
    @Nullable
    private static Bitmap resizeBitmap(Bitmap bitmap, int maxPx) {
        int newWidth;
        int newHeight;

        // L'image est trop grande il faut la réduire
        if ((bitmap.getWidth() > maxPx) || (bitmap.getHeight() > maxPx)) {
            int max;
            if (bitmap.getWidth() > maxPx) {
                max = bitmap.getWidth();
            } else {
                max = bitmap.getHeight();
            }

            double prorata = (double) maxPx / max;

            newWidth = (int) (bitmap.getWidth() * prorata);
            newHeight = (int) (bitmap.getHeight() * prorata);
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        } else {
            return bitmap;
        }
    }

    /**
     * Save a bitmap into the specified path
     *
     * @param bmp
     * @param uri
     * @return
     */
    public static boolean saveBitmapToUri(Bitmap bmp, Uri uri) {
        boolean saved = false;
        FileOutputStream out = null;
        File newFile;
        newFile = new File(uri.getPath());
        if (newFile.exists()) {
            newFile.delete();
        }
        try {
            out = new FileOutputStream(newFile);
            if (!newFile.exists()) {
                try {
                    newFile.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                    saved = true;
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                saved = false;
            }
        }
        return saved;
    }

    /**
     * @param context
     * @param uri
     * @return
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri) {
        Log.d(TAG, "getBitmapFromUri uri = " + uri.toString());
        InputStream imageStream;
        Bitmap bitmap = null;
        try {
            imageStream = context.getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(imageStream);
        } catch (FileNotFoundException e) {
            Log.e("getBitmapFromUri", e.getMessage(), e);
        }
        return bitmap;
    }

    /**
     * Créer un URI pour stocker l'image/la video
     *
     * @param context
     * @param type
     * @param prefixName
     * @return
     */
    @Nullable
    public static Pair<Uri, File> createNewMediaFileUri(Context context, boolean externalStorage, MediaType type, String prefixName) {
        String fileName = generateMediaName(type, prefixName);
        File newFile;
        if (externalStorage) {
            newFile = createExternalMediaFile(fileName);
        } else {
            newFile = createInternalMediaFile(context, fileName);
        }
        if (newFile != null) {
            return new Pair<>(FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", newFile), newFile);
        }
        return null;
    }

    /**
     * Create new internal file
     *
     * @param context
     * @param fileName
     * @return
     */
    public static File createInternalMediaFile(Context context, String fileName) {
        return new File(context.getFilesDir(), fileName);
    }


    /**
     * Retourne le nom d'une nouvelle image / d'une video
     *
     * @param fileName
     * @return
     */
    private static File createExternalMediaFile(String fileName) {
        // External sdcard location
        File mediaStorageDir = new File(
                // TODO voir pour remplacer par context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                // Suivre lien https://developer.android.com/training/data-storage/files.html#java
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Constants.IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Oops! Failed create "
                        + Constants.IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        return new File(mediaStorageDir.getPath() + fileName);
    }

    /**
     * Génération d'un nom d'image ou de vidéo
     *
     * @param type
     * @param prefixName
     * @return
     */
    public static String generateMediaName(MediaType type, String prefixName) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(new Date());
        if (type.equals(MediaType.IMAGE)) {
            return File.separator + String.valueOf(prefixName) + "_IMG_" + timeStamp + ".jpg";
        } else if (type.equals(MediaType.VIDEO)) {
            return File.separator + String.valueOf(prefixName) + "_VID_" + timeStamp + ".mp4";
        } else {
            return null;
        }
    }

    /**
     * Copie un fichier dans un autre
     *
     * @param context
     * @param src
     * @param dst
     * @return false si le fichier source n'a pas pu être lu
     * @throws IOException
     */
    public static boolean copyAndResizeUriImages(Context context, Uri src, Uri dst) throws IOException {
        Bitmap bitmapSrc = getBitmapFromUri(context, src);
        if (bitmapSrc == null) {
            Log.e(TAG, "L'image avec l'uri : " + src.toString() + " n'a pas pu être récupérée.");
            return false;
        }

        Bitmap bitmapDst = resizeBitmap(bitmapSrc, Constants.MAX_SIZE);
        if (bitmapDst == null) {
            Log.e(TAG, "Le retaillage de l'image a échoué.");
            return false;
        }

        OutputStream out = context.getContentResolver().openOutputStream(dst);
        try {
            bitmapDst.compress(Bitmap.CompressFormat.PNG, 100, out);
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        return true;
    }
}
