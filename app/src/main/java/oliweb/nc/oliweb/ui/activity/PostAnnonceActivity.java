package oliweb.nc.oliweb.ui.activity;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.media.MediaType;
import oliweb.nc.oliweb.media.MediaUtility;
import oliweb.nc.oliweb.ui.activity.viewmodel.PostAnnonceActivityViewModel;
import oliweb.nc.oliweb.ui.dialog.PhotoSourceDialog;

import static oliweb.nc.oliweb.database.entity.StatusRemote.TO_SEND;

public class PostAnnonceActivity extends AppCompatActivity implements PhotoSourceDialog.PhotoSourceListener {

    private static final String TAG = PostAnnonceActivity.class.getName();
    public static final String PHOTO_SOURCE_DIALOG_TAG = "photoSourceDialogTag";

    public static final int DIALOG_REQUEST_IMAGE = 100;
    private static final int DIALOG_GALLERY_IMAGE = 200;
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 999;
    private static final int CODE_WORK_IMAGE_CREATION = 300;
    private static final int CODE_WORK_IMAGE_MODIFICATION = 400;

    private PostAnnonceActivityViewModel viewModel;
    private Uri mFileUriTemp;
    private String uidUtilisateur;
    private PhotoSourceDialog photoSourcedialog;

    @BindView(R.id.spinner_categorie)
    Spinner spinnerCategorie;

    @BindView(R.id.edit_titre_annonce)
    EditText textViewTitre;

    @BindView(R.id.edit_description_annonce)
    EditText textViewDescription;

    @BindView(R.id.edit_prix_annonce)
    EditText textViewPrix;

    private View.OnClickListener askForNewPhotoSource = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dismissPreviousPhotoSourceDialog();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().addToBackStack(null);
            photoSourcedialog.show(transaction, PHOTO_SOURCE_DIALOG_TAG);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(PostAnnonceActivityViewModel.class);

        setContentView(R.layout.activity_post_annonce);
        ButterKnife.bind(this);



        this.photoSourcedialog = new PhotoSourceDialog();
        if (FirebaseAuth.getInstance() != null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            uidUtilisateur = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Log.e(TAG, "impossible de lancer PostAnnonceActivity sans être connecté");
            finish();
        }
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
    protected void onActivityResult(int code_request, int resultCode, Intent data) {
        // if the result is capturing Image
        int position;
        byte[] byteArray;
        switch (code_request) {
            case CODE_WORK_IMAGE_CREATION:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    if (data.getExtras().getBoolean(WorkImageActivity.BUNDLE_OUT_MAJ)) {
                        byteArray = data.getExtras().getByteArray(WorkImageActivity.BUNDLE_OUT_IMAGE);
                        String path = MediaUtility.saveByteArrayToFile(byteArray, uidUtilisateur);
                        savePhoto(UUID.randomUUID().toString(), path, annonce);
                        if (travailImage(byteArray, true, 0)) {
                            MediaUtility.deleteTempFile(mFileUriTemp);
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
                                    MediaUtility.deleteTempFile(mFileUriTemp);
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
                dismissPreviousPhotoSourceDialog();
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
                dismissPreviousPhotoSourceDialog();
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

    private void savePhoto(String path, boolean nouvelleImg, AnnonceEntity annonceEntity) {
        if (nouvelleImg) {
            // On est en mode création
            // On insère un nouvel enregistrement dans l'arrayList
            PhotoEntity photo = new PhotoEntity();
            photo.setUUID(UUID.randomUUID().toString());
            photo.setCheminLocal(path);
            photo.setIdAnnonce(annonceEntity.getIdAnnonce());
            photo.setStatut(TO_SEND);
        } else {
            // On est en mode modification
            // Recherche dans le repository de notre photo par rapport à son id
            photo.setCheminLocal(path);
            photo.setStatut(StatusRemote.TO_UPDATE);
        }
    }

    /**
     * Try to find a PhotoSourceDialog in the fragment manager, if found we remove it
     */
    private void dismissPreviousPhotoSourceDialog() {
        Fragment prev = getSupportFragmentManager().findFragmentByTag(PHOTO_SOURCE_DIALOG_TAG);
        if (prev != null) {
            getSupportFragmentManager().beginTransaction().remove(prev).commit();
        }
    }

    /**
     * Launch activity to take a picture
     */
    private void callCaptureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mFileUriTemp = MediaUtility.getOutputMediaFileUri(MediaType.IMAGE, uidUtilisateur);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUriTemp);
        startActivityForResult(intent, DIALOG_REQUEST_IMAGE);
    }


    /**
     * Appel de l'activity de travail d'une image
     * Cette activité va nous permettre de faire tourner une image.
     * Passage de l'image à modifier sous forme d'un ByteArray
     *
     * @param uri         where we can find the image
     * @param mode
     * @param requestCode
     */
    private void callWorkingImageActivity(Uri uri, String mode, int requestCode) {
        Intent intent = new Intent();
        intent.setClass(this, WorkImageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(WorkImageActivity.BUNDLE_KEY_MODE, mode);

        // Récupération du bitmap à partir de l'Uri qu'on a reçu.
        byte[] byteArray = MediaUtility.uriToByteArray(this, uri);
        bundle.putByteArray(WorkImageActivity.BUNDLE_IN_IMAGE, byteArray);
        intent.putExtras(bundle);
        startActivityForResult(intent, requestCode);
    }
}
