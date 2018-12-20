package oliweb.nc.oliweb.utility;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.util.IOUtils;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.exifinterface.media.ExifInterface;
import oliweb.nc.oliweb.BuildConfig;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

/**
 * Created by orlanth23 on 03/02/2018.
 */

@Singleton
public class MediaUtility {

    public enum MediaType {
        IMAGE,
        VIDEO
    }

    private static final String TAG = MediaUtility.class.getName();

    @Inject
    public MediaUtility() {

    }

//    private String getRealPathFromURIBeforeApi19(Context context, Uri contentUri) {
//        String[] proj = {MediaStore.Images.Media.DATA};
//        CursorLoader loader = new CursorLoader(context, contentUri, proj, null, null, null);
//        Cursor cursor = loader.loadInBackground();
//        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//        cursor.moveToFirst();
//        String result = cursor.getString(column_index);
//        cursor.close();
//        return result;
//    }

    public InputStream getInputStream(Context context, Uri contentUri) throws FileNotFoundException {
        return context.getContentResolver().openInputStream(contentUri);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean allPermissionsAreGranted(Context context, List<String> permissionsToCheck) {
        boolean allPermitted = true;
        for (String permission : permissionsToCheck) {
            allPermitted = allPermitted && (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
        }
        return allPermitted;
    }

    @NonNull
    public AlertDialog.Builder getBuilder(Context context) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(context, 0);
        } else {
            builder = new AlertDialog.Builder(context);
        }
        return builder;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public boolean isExternalStorageAvailable() {
        return isExternalStorageReadable() && isExternalStorageWritable();
    }

    /**
     * Force a refresh of media content provider for specific item
     *
     * @param fileName
     */
    public void refreshMediaProvider(Context appContext, String fileName) {
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
     * Save a bitmap into the specified path
     *
     * @param bmp
     * @param uri
     * @return
     */
    public boolean saveBitmapToUri(Bitmap bmp, Uri uri) {
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
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
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
     * Redimenssionne une image pour qu'elle corresponde aux limitations.
     * On ne fait que de la diminution d'image pas d'agrandissement.
     *
     * @param bitmap La bitmap a redimenssionné
     * @param maxPx  Nombre de pixel maximum de l'image (autant en largeur ou hauteur)
     * @return Bitmap redimenssionné
     */
    @Nullable
    private Bitmap resizeBitmap(Bitmap bitmap, int maxPx) {
        int newWidth;
        int newHeight;

        // L'image est trop grande il faut la réduire
        if (isImageTooBig(bitmap, maxPx)) {
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

    private boolean isImageTooBig(Bitmap bitmap, int maxPx) {
        return ((bitmap.getWidth() > maxPx) || (bitmap.getHeight() > maxPx));
    }


    /**
     * @param context
     * @param uri
     * @return
     */
    public Bitmap getBitmapFromUri(Context context, Uri uri) {
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

    public Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            return null;
        }
    }

    public static String getProviderAuthority() {
        return BuildConfig.APPLICATION_ID + ".provider";
    }

    public static String getContentProviderAuthority() {
        return "content://" + BuildConfig.APPLICATION_ID + ".provider/internal_files/";
    }

    public static String getContentProviderAuthorityExternal() {
        return "content://" + BuildConfig.APPLICATION_ID + ".provider/external_files/";
    }

    /**
     * Créer un URI pour stocker l'image/la video
     *
     * @param context
     * @param type
     * @return
     */
    @Nullable
    public static Pair<Uri, File> createNewMediaFileUri(Context context, boolean externalStorage, MediaType type) {
        String fileName = generateMediaName(type);
        File newFile = (externalStorage) ? createExternalMediaFile(fileName) : createInternalMediaFile(context, fileName);
        if (newFile != null) {
            return new Pair<>(FileProvider.getUriForFile(context, getProviderAuthority(), newFile), newFile);
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
    private static File createInternalMediaFile(Context context, String fileName) {
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
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(TAG, "Oops! Failed create "
                    + Constants.IMAGE_DIRECTORY_NAME + " directory");
            return null;
        }

        // Create a media file name
        return new File(mediaStorageDir.getPath() + fileName);
    }

    /**
     * Génération d'un nom d'image ou de vidéo
     *
     * @param type
     * @return
     */
    private static String generateMediaName(MediaType type) {
        String prefixName = UUID.randomUUID().toString();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(new Date());
        if (type.equals(MediaType.IMAGE)) {
            return File.separator + String.valueOf(prefixName) + "_IMG_" + timeStamp + ".jpg";
        } else if (type.equals(MediaType.VIDEO)) {
            return File.separator + String.valueOf(prefixName) + "_VID_" + timeStamp + ".mp4";
        } else {
            return null;
        }
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    /**
     * Copie un fichier dans un autre
     *
     * @param context
     * @param uriSource
     * @param pairUriFile
     * @return false si le fichier source n'a pas pu être lu
     */
    public boolean copyAndResizeUriImages(Context context, Uri uriSource, Pair<Uri, File> pairUriFile, boolean deleteUriSource) {
        // Récupération de la longeur max depuis le remote config.
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        int longeurMax = safeLongToInt(remoteConfig.getLong(Constants.REMOTE_IMAGE_RESOLUTION_RESIZE));
        int decreasingQuality = safeLongToInt(remoteConfig.getLong(Constants.DECREASE_JPEG_QUALITY));

        // Récupération du EXIF du fichier pour récupérer l'orientation de l'image initiale => OK
        ExifInterface oldExif = null;
        try {
            oldExif = new ExifInterface(getInputStream(context, uriSource));
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }

        // Récupération du bitmap depuis le disque du device
        Bitmap bitmapSrc = getBitmapFromUri(context, uriSource);
        if (bitmapSrc == null) {
            Crashlytics.log("L'image avec l'uri : " + uriSource.toString() + " n'a pas pu être récupérée.");
            return false;
        }

        boolean resized = isImageTooBig(bitmapSrc, longeurMax);
        Bitmap bitmapDst = resizeBitmap(bitmapSrc, longeurMax);
        if (bitmapDst == null) {
            Crashlytics.log("Le retaillage de l'image a échoué.");
            return false;
        }

        try (OutputStream out = context.getContentResolver().openOutputStream(pairUriFile.first)) {
            // La photo a été retaillée, on va également baissé la qualité de la photo
            if (resized) {
                bitmapDst.compress(Bitmap.CompressFormat.JPEG, decreasingQuality, out);
                out.flush();
            }
            if (deleteUriSource) {
                deletePhotoFromDevice(context.getContentResolver(), uriSource.toString());
            }
        } catch (IOException exception) {
            Log.e(TAG, exception.getLocalizedMessage(), exception);
            return false;
        }

        // Enregistrement de l'orientation de l'ancienne image
        if (oldExif != null && oldExif.getAttribute(ExifInterface.TAG_ORIENTATION) != null) {
            try {
                ExifInterface newExif = new ExifInterface(pairUriFile.second.getAbsolutePath());
                newExif.setAttribute(ExifInterface.TAG_ORIENTATION, oldExif.getAttribute(ExifInterface.TAG_ORIENTATION));
                newExif.saveAttributes();
                return true;
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
                return false;
            }
        } else {
            // On a retaillé et copié l'image, mais on a pas récupéré son EXIF
            return true;
        }
    }

    public boolean saveBitmapToFileProviderUri(ContentResolver contentResolver, Bitmap bitmapToSave, Uri uriDestination) {
        try {
            OutputStream out = contentResolver.openOutputStream(uriDestination);
            bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, out);
            if (out != null) {
                out.flush();
                out.close();
                return true;
            }
        } catch (IOException e) {
            Log.d(TAG, e.getLocalizedMessage(), e);
        }
        return false;
    }

    public boolean deletePhotoFromDevice(ContentResolver contentResolver, String uriPhoto) {
        try {
            boolean result = (contentResolver.delete(Uri.parse(uriPhoto), null, null) != 0);
            Log.d(TAG, result ? "Photo " + uriPhoto + " successfully deleted !" : "Photo " + uriPhoto + " has not been deleted !");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Exception encountered to delete physical photo : " + uriPhoto);
            return false;
        }
    }

    public static void saveInputStreamToContentProvider(InputStream inputStream, File file) {
        try (OutputStream outStream = new FileOutputStream(file.getAbsoluteFile())) {
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }

            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outStream);
        } catch (IOException exception) {
            Log.e(TAG, exception.getLocalizedMessage(), exception);
        }
    }

    @NonNull
    public Pair<Uri, File> createNewImagePairUriFile(Context context) {
        boolean useExternalStorage = SharedPreferencesHelper.getInstance(context).getUseExternalStorage();
        Pair<Uri, File> pairUriFile = createNewMediaFileUri(context, useExternalStorage, MediaUtility.MediaType.IMAGE);

        if (pairUriFile == null) {
            throw new MediaUtilityException("Pair Uri & File ne peut pas être null");
        }

        if (pairUriFile.second == null) {
            throw new MediaUtilityException("File ne peut pas être null");
        }

        if (pairUriFile.first == null || pairUriFile.first.toString().isEmpty()) {
            throw new MediaUtilityException("Uri ne peut pas être null");
        }

        return pairUriFile;
    }
}
