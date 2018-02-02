package oliweb.nc.oliweb.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.Utility;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.ui.dialog.PhotoSourceDialog;

public class PostAnnonceActivity extends AppCompatActivity implements PhotoSourceDialog.PhotoSourceListener {

    private static final String TAG = PostAnnonceActivity.class.getName();
    public static final int DIALOG_REQUEST_IMAGE = 100;
    private static final int DIALOG_GALLERY_IMAGE = 200;
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 999;
    private static final int CODE_WORK_IMAGE_CREATION = 300;
    private static final int CODE_WORK_IMAGE_MODIFICATION = 400;

    private Uri mFileUriTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_annonce);
    }

    private void saveAnnonce() {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.bottom_post_save) {
            saveAnnonce();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Launch activity to take a picture
     */
    private void callCaptureIntent() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mFileUriTemp = Utility.getOutputMediaFileUri(Constants.MEDIA_TYPE_IMAGE, currentUser.getUid(), TAG);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUriTemp);
            startActivityForResult(intent, DIALOG_REQUEST_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callCaptureIntent();
            }
        }
    }

    @Override
    public void onNewPictureClick() {
        // Demande de la permission pour utiliser la camera
        if (Build.VERSION.SDK_INT >= 23) {
            if ((checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) ||
                    (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CAMERA_PERMISSION_CODE);
            } else {
                callCaptureIntent();
            }
        } else {
            callCaptureIntent();
        }
    }

    @Override
    public void onGalleryClick() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, DIALOG_GALLERY_IMAGE);
    }

    @Override
    protected void onActivityResult(int code_request, int resultCode, Intent data) {
        // if the result is capturing Image
        int position;
        byte[] byteArray;
        switch (code_request) {
            case CODE_WORK_IMAGE_CREATION:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    if (data.getExtras().getBoolean(WorkImageActivity.BUNDLE_OUT_MAJ)) {
                        // Récupération du Byte Array
                        byteArray = data.getExtras().getByteArray(WorkImageActivity.BUNDLE_OUT_IMAGE);
                        if (travailImage(byteArray, true, 0)) {
                            deleteTempFile();
                        }
                    }
                    presentPhoto();
                }
                break;
            case CODE_WORK_IMAGE_MODIFICATION:
                switch (resultCode) {
                    case RESULT_OK:
                        // Récupération de l'ancienne position
                        if (data != null && data.getExtras() != null) {
                            position = data.getExtras().getInt(WorkImageActivity.BUNDLE_KEY_ID);

                            // Récupération du BITMAP
                            if (data.getExtras().getBoolean(WorkImageActivity.BUNDLE_OUT_MAJ)) {
                                byteArray = data.getExtras().getByteArray(WorkImageActivity.BUNDLE_OUT_IMAGE);
                                if (travailImage(byteArray, false, position)) {
                                    deleteTempFile();
                                }
                            }
                            presentPhoto();
                        }
                        break;

                    // On veut supprimer la photo
                    case RESULT_CANCELED:
                        if (data != null && data.getExtras() != null) {
                            position = data.getExtras().getInt(WorkImageActivity.BUNDLE_KEY_ID);
                            PhotoEntity photo = mAnnonce.getPhotos().get(position);
                            photo.setStatutPhoto(StatutPhoto.ToDelete.valeur());
                            presentPhoto();
                        }
                }
                break;
            case DIALOG_REQUEST_IMAGE:
                dialogImageChoice.dismiss(); // On ferme la boite de dialogue
                if (resultCode == RESULT_OK) {
                    callWorkingImageActivity(mFileUriTemp, Constants.PARAM_CRE, CODE_WORK_IMAGE_CREATION);  // On va appeler WorkImageActivity
                } else if (resultCode == RESULT_CANCELED) {
                    // user cancelled Image capture
                    Toast.makeText(this,
                            "Annulation de la capture",
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    // failed to capture image
                    Toast.makeText(this,
                            "Echec de la capture",
                            Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            case DIALOG_GALLERY_IMAGE:
                dialogImageChoice.dismiss();
                if (resultCode == RESULT_OK) {
                    // On revient de la galerie où on a choisit une image.
                    Uri uri = data.getData();

                    // On va appeler WorkImageActivity avec l'uri récupéré
                    callWorkingImageActivity(uri, Constants.PARAM_CRE, CODE_WORK_IMAGE_CREATION);

                } else if (resultCode == RESULT_CANCELED) {
                    // user cancelled Image capture
                    Toast.makeText(getApplicationContext(),
                            "Annulation de la capture",
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    // failed to capture image
                    Toast.makeText(getApplicationContext(),
                            "Echec de la capture",
                            Toast.LENGTH_SHORT)
                            .show();
                }
                break;
        }
    }

    /**
     * Méthode qui va récupérer une image et la retailler et ensuite va l'attacher à l'annonce
     * On va créer un nouveau fichier et y enregistrer le ByteArray qu'on a recu.
     * Si c'est une nouvelle photo on ajoute cette photo à la liste des photos
     * Si c'est une photo existante on met juste à jour dans notre liste son path
     * On supprime ensuite le fichier temporaire dont on s'était servi
     *
     * @param byteArray   Nouvelle Photo en format Byte Array
     * @param nouvelleImg Boolean pour savoir si c'est une nouvelle photo ou pas
     * @param position    Si c'est une photo existante qu'on met à jour, ceci est la position dans la liste des photos de l'annonce
     */
    @NonNull
    protected boolean travailImage(byte[] byteArray, boolean nouvelleImg, int position) {
        String path;
        boolean retour = true;

        if (byteArray == null) {
            retour = false;
        }

        File f = Utility.getOutputMediaFile(Constants.MEDIA_TYPE_IMAGE, CurrentUser.getInstance().getIdUTI(), TAG);

        if (f == null) {
            retour = false;
        }

        try {
            if (f.createNewFile()) {
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(byteArray);
                fo.close();
                path = f.getPath();

                if (nouvelleImg) {
                    // On est en mode création
                    // On insère un nouvel enregistrement dans l'arrayList
                    PhotoEntity photo = new PhotoEntity(UUID.randomUUID().toString(), path, mAnnonce.getUUIDANO(), StatutPhoto.ToSend.valeur());
                    mAnnonce.getPhotos().add(photo);
                } else {
                    // On est en mode modification
                    // On va se positionner sur la bonne photo pour faire la modification de chemin
                    PhotoEntity photo = mAnnonce.getPhotos().get(position);
                    photo.setPathPhoto(path);
                    photo.setStatutPhoto(StatutPhoto.ToUpdate.valeur());
                }
            }
        } catch (IOException e) {
            Log.e("IOException", TAG + ":travailImage:" + e.getMessage(), e);
            retour = false;
        }
        return retour;
    }

    /**
     * Appelle de l'activity de travail d'une image
     * Cette activité va nous permettre de faire tourner une image.
     *
     * @param uri
     * @param mode
     * @param requestCode
     */
    private void callWorkingImageActivity(Uri uri, String mode, int requestCode) {
        Bitmap bitmap;
        Bitmap bitmapResized;
        byte[] byteArray;

        Intent intent = new Intent();
        intent.setClass(this, WorkImageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(WorkImageActivity.BUNDLE_KEY_MODE, mode);

        // Récupération du bitmap à partir de l'Uri qu'on a reçu.
        InputStream imageStream;
        try {
            imageStream = getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(imageStream);
            bitmapResized = Utility.resizeBitmap(bitmap, Constants.MAX_IMAGE_SIZE);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmapResized.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byteArray = stream.toByteArray();
            bundle.putByteArray(WorkImageActivity.BUNDLE_IN_IMAGE, byteArray);
        } catch (FileNotFoundException e) {
            Log.e(e.getClass().getName(), e.getMessage(), e);
        }

        intent.putExtras(bundle);
        startActivityForResult(intent, requestCode);
    }
}
