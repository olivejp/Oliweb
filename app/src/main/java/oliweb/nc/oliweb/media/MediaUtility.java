package oliweb.nc.oliweb.media;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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


    public static File saveInternalFile(Context context, String fileName) {
        File file = new File(context.getFilesDir(), fileName);
        return file;
    }

    /**
     * @param uri
     * @return byte[] containing the image from the Uri after resizing and compression to PNG
     */
    public static byte[] uriToByteArray(Context context, Uri uri) {
        Bitmap bitmap;
        Bitmap bitmapResized;
        byte[] byteArray;
        InputStream imageStream;
        try {
            imageStream = context.getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(imageStream);
            bitmapResized = MediaUtility.resizeBitmap(bitmap, MediaConstants.MAX_IMAGE_SIZE);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmapResized.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byteArray = stream.toByteArray();
            return byteArray;
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    public static String getRealPathFromGaleryUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        String result = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};

            try {
                context.getContentResolver().openInputStream(contentUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index;
            if (cursor != null) {
                column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                result = cursor.getString(column_index);
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Copie un fichier donné dans uriToCopy et renvoie un nouveau fichier
     * Puis supprime le fichier source temporarire.
     *
     * @param context
     * @param externalStorage
     * @param prefix
     * @param uriToCopy
     * @return
     * @throws IOException
     */
    public static File copyFileAndDelete(Context context, boolean externalStorage, String prefix, Uri uriToCopy) throws IOException {
        File newFile;
        if (externalStorage) {
            newFile = MediaUtility.saveExternalMediaFile(MediaType.IMAGE, prefix);
        } else {
            newFile = MediaUtility.saveInternalFile(context, MediaUtility.generatePictureName(MediaType.IMAGE, prefix));
        }
        String realPathSrc = MediaUtility.getRealPathFromGaleryUri(context, uriToCopy);
        File oldFile = new File(realPathSrc);
        copy(oldFile, newFile);
        if (!oldFile.delete()) {
            Log.e(TAG, "Echec de la suppression du fichier source");
        }
        return newFile;
    }

    /**
     * Redimenssionne une image pour qu'elle corresponde aux limitations.
     * On ne fait que de la diminution d'image pas d'agrandissement.
     *
     * @param p_bitmap La bitmap a redimenssionné
     * @param maxPx    Nombre de pixel maximum de l'image (autant en largeur ou hauteur)
     * @return Bitmap redimenssionné
     */
    public static Bitmap resizeBitmap(Bitmap p_bitmap, int maxPx) {
        int newWidth;
        int newHeight;

        // L'image est trop grande il faut la réduire
        if ((p_bitmap.getWidth() > maxPx) || (p_bitmap.getHeight() > maxPx)) {
            int max;
            if (p_bitmap.getWidth() > maxPx) {
                max = p_bitmap.getWidth();
            } else {
                max = p_bitmap.getHeight();
            }

            double prorata = (double) maxPx / max;

            newWidth = (int) (p_bitmap.getWidth() * prorata);
            newHeight = (int) (p_bitmap.getHeight() * prorata);
        } else {
            return p_bitmap;
        }

        return Bitmap.createScaledBitmap(p_bitmap, newWidth, newHeight, true);
    }

    /**
     * Reçoit un byteArray et va écrire son contenu dans un fichier
     *
     * @param byteArray      to save in a file
     * @param UidUtilisateur
     * @return path of the file created
     */
//    public static String saveByteArrayToFile(byte[] byteArray, String UidUtilisateur) {
//        String path;
//        if (byteArray == null) return null;
//        File f = MediaUtility.saveExternalMediaFile(MediaType.IMAGE, UidUtilisateur);
//        if (f == null) return null;
//        try {
//            if (f.createNewFile()) {
//                FileOutputStream fo = new FileOutputStream(f);
//                fo.write(byteArray);
//                fo.close();
//                path = f.getPath();
//                return path;
//            } else {
//                return null;
//            }
//        } catch (IOException e) {
//            Log.e("IOException", TAG + ":travailImage:" + e.getMessage(), e);
//            return null;
//        }
//    }

    /**
     * @param bitmap
     * @param uuidUtilisateur
     * @return path of the file created
     */
    public static String saveBitmapToFile(Bitmap bitmap, String uuidUtilisateur) {
        // On enregistre cette nouvelle image retaillée et on récupère son chemin dans path
        String retour = null;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        File f = saveExternalMediaFile(MediaType.IMAGE, uuidUtilisateur);
        try {
            if (f != null) {
                if (f.createNewFile()) {
                    FileOutputStream fo = new FileOutputStream(f);
                    fo.write(bytes.toByteArray());
                    fo.close();
                    retour = f.getPath();
                }
            }
        } catch (IOException e) {
            Log.e("saveBitmap", e.getMessage(), e);
        }

        return retour;
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
     * @param bitmap
     * @return
     */
    public static byte[] transformBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    /**
     * @param context
     * @param uri
     * @return
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri) {
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

    public static Bitmap getBitmapFromPath(String path) {
        File image = new File(path);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        return BitmapFactory.decodeFile(image.getAbsolutePath(), bmOptions);
    }

    /**
     * Créer un URI pour stocker l'image/la video
     *
     * @param type
     * @param prefixName
     * @return Uri of the file
     */
    @NonNull
    public static Uri getOutputMediaFileUri(MediaType type, String prefixName) {
        return Uri.fromFile(saveExternalMediaFile(type, prefixName));
    }

    /**
     * Retourne le nom d'une nouvelle image / d'une video
     *
     * @param type       MediaType
     * @param prefixName prefixe qu'on ajoutera au nom de la nouvelle ressource
     * @return File
     */
    public static File saveExternalMediaFile(MediaType type, String prefixName) {
        // External sdcard location
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                MediaConstants.IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Oops! Failed create "
                        + MediaConstants.IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        File mediaFile;
        if (type.equals(MediaType.IMAGE)) {
            mediaFile = new File(mediaStorageDir.getPath() + generatePictureName(type, prefixName));
        } else if (type.equals(MediaType.VIDEO)) {
            mediaFile = new File(mediaStorageDir.getPath() + generatePictureName(type, prefixName));
        } else {
            return null;
        }

        return mediaFile;
    }

    public static String generatePictureName(MediaType type, String prefixName) {
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
     * Si un fichier temporaire est présent il faut le supprimer
     */
    public static void deleteTempFile(Uri tempFile) {
        File file = new File(String.valueOf(tempFile));
        if (file.exists()) {
            if (!file.delete()) {
                Log.e(TAG, "Fichier temporaire non supprimé");
            }
        }
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}
