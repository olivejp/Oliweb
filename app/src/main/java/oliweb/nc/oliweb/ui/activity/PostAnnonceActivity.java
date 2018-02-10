package oliweb.nc.oliweb.ui.activity;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.media.MediaType;
import oliweb.nc.oliweb.media.MediaUtility;
import oliweb.nc.oliweb.ui.activity.viewmodel.PostAnnonceActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.PhotoAdapter;
import oliweb.nc.oliweb.ui.adapter.SpinnerAdapter;

public class PostAnnonceActivity extends AppCompatActivity {

    private static final String TAG = PostAnnonceActivity.class.getName();

    public static final int RC_POST_ANNONCE = 881;

    public static final String BUNDLE_KEY_ID_ANNONCE = "ID_ANNONCE";
    public static final String BUNDLE_KEY_MODE = "MODE";

    public static final int DIALOG_REQUEST_IMAGE = 100;
    private static final int DIALOG_GALLERY_IMAGE = 200;
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 999;

    private PostAnnonceActivityViewModel viewModel;
    private Uri mFileUriTemp;
    private PhotoAdapter photoAdapter;
    private boolean externalStorage = true;

    @BindView(R.id.spinner_categorie)
    Spinner spinnerCategorie;

    @BindView(R.id.edit_titre_annonce)
    EditText textViewTitre;

    @BindView(R.id.edit_description_annonce)
    EditText textViewDescription;

    @BindView(R.id.edit_prix_annonce)
    EditText textViewPrix;

    @BindView(R.id.recyclerImages)
    RecyclerView recyclerImages;

    @BindView(R.id.coordinator_post_annonce)
    CoordinatorLayout coordinatorLayout;

    private View.OnClickListener onClickPhoto = v -> {
        PhotoEntity photoEntity = (PhotoEntity) v.getTag();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(PostAnnonceActivityViewModel.class);

        setContentView(R.layout.activity_post_annonce);
        ButterKnife.bind(this);

        // Préparation du recycler view qui recevra les images de l'annonce
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(OrientationHelper.HORIZONTAL);
        recyclerImages.setLayoutManager(linearLayoutManager);
        photoAdapter = new PhotoAdapter(this, onClickPhoto);
        recyclerImages.setAdapter(photoAdapter);

        // Alimentation du spinner avec la liste des catégories
        viewModel.getLiveDataListCategorie()
                .observe(this, categorieEntities -> {
                    if (categorieEntities != null && !categorieEntities.isEmpty()) {
                        SpinnerAdapter adapter = new SpinnerAdapter(this, categorieEntities);
                        spinnerCategorie.setAdapter(adapter);
                    }
                });

        // Récupération dynamique de la liste des photos
        viewModel.getLiveListPhoto().observe(this, photoEntities -> {
            if (photoEntities != null) {
                List<PhotoEntity> listPhoto = new ArrayList<>();
                listPhoto.addAll(photoEntities);
                this.photoAdapter.setListPhotos(listPhoto);
            }
        });

        // Récupération des paramètres
        Bundle bundle = getIntent().getExtras();
        if (bundle == null || !bundle.containsKey(BUNDLE_KEY_MODE) || bundle.getString(BUNDLE_KEY_MODE) == null) {
            Log.e(TAG, "Aucun mode passé en paramètre");
            finish();
        } else {
            String mode = bundle.getString(BUNDLE_KEY_MODE);
            if (mode.equals(Constants.PARAM_CRE)) {
                viewModel.createNewAnnonce();
            } else if (mode.equals(Constants.PARAM_MAJ)) {
                if (!bundle.containsKey(BUNDLE_KEY_ID_ANNONCE)) {
                    Log.e(TAG, "Aucun Id d'annonce passé en paramètre");
                    finish();
                }
                long idAnnonce = bundle.getLong(BUNDLE_KEY_ID_ANNONCE);
                viewModel.findAnnonceById(idAnnonce).observe(this, annonceEntity -> {
                    if (annonceEntity != null) {
                        viewModel.setAnnonce(annonceEntity);

                        // Récupération des photos de cette annonce dans l'adapter
                        viewModel.getListPhotoByIdAnnonce(annonceEntity.getIdAnnonce())
                                .observe(PostAnnonceActivity.this, photoEntities -> {
                                    if (photoEntities != null && !photoEntities.isEmpty()) {
                                        viewModel.setListPhoto(photoEntities);
                                        photoAdapter.setListPhotos(photoEntities);
                                    }
                                });

                        displayAnnonce(annonceEntity);
                    }
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.post_annonce_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int idItem = item.getItemId();
        switch (idItem) {
            case R.id.menu_post_valid:
                if (isValidAnnonce()) {

                    // Get annonce from the ui
                    AnnonceEntity annonceEntity = new AnnonceEntity();
                    annonceEntity.setTitre(textViewTitre.getText().toString());
                    annonceEntity.setDescription(textViewDescription.getText().toString());
                    annonceEntity.setPrix(Integer.valueOf(textViewPrix.getText().toString()));
                    annonceEntity.setStatut(StatusRemote.TO_SEND);
                    viewModel.setAnnonce(annonceEntity);

                    // Save the annonce
                    viewModel.saveAnnonce(dataReturn -> {
                        if (dataReturn.getNb() > 0) {
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                    return true;
                }
        }
        return false;
    }

    private boolean isValidAnnonce() {
        boolean isValid = true;
        if (textViewTitre.getText().toString().isEmpty()) {
            textViewTitre.setError("Le titre est obligatoire.");
            isValid = false;
        }
        if (textViewDescription.getText().toString().isEmpty()) {
            textViewDescription.setError("La description est obligatoire.");
            isValid = false;
        }
        if (textViewPrix.getText().toString().isEmpty()) {
            textViewPrix.setError("Le prix est obligatoire.");
            isValid = false;
        }
        return isValid;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callCaptureIntent();
            }
        }
    }

    @Override
    protected void onActivityResult(int code_request, int resultCode, Intent data) {
        switch (code_request) {
            case DIALOG_REQUEST_IMAGE:
                if (resultCode == RESULT_OK) {
                    try {
                        if (MediaUtility.copyAndResizeUriImages(this, mFileUriTemp, mFileUriTemp)) {
                            viewModel.addPhotoToCurrentList(mFileUriTemp.toString());
                        } else {
                            Snackbar.make(coordinatorLayout, "L'image " + mFileUriTemp.getPath() + " n'a pas pu être récupérée.", Snackbar.LENGTH_LONG).show();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
                break;
            case DIALOG_GALLERY_IMAGE:
                if (resultCode == RESULT_OK) {
                    // Insertion multiple
                    if (data.getClipData() != null) {
                        int i = -1;
                        ClipData.Item item;
                        while (i++ < data.getClipData().getItemCount() - 1) {
                            item = data.getClipData().getItemAt(i);
                            insertionFromGallery(item.getUri());
                        }
                    } else {
                        // Insertion simple
                        Uri uri = data.getData();
                        if (uri != null) {
                            insertionFromGallery(uri);
                        }
                    }
                }
                break;
        }
    }

    /**
     * Le paramètre uri représente une image dans la gallerie.
     * nouvelleUri correspond à l'emplacement de destination de cette image.
     * car on veut la déplacer dans le répertoire spécifique à Oliweb.
     * On va donc copier l'image présente dans uri et la mettre dans nouvelleUri.
     *
     * @param uri
     */
    private void insertionFromGallery(Uri uri) {
        try {
            Uri nouvelleUri = generateNewUri();
            if (MediaUtility.copyAndResizeUriImages(getApplicationContext(), uri, nouvelleUri)) {
                viewModel.addPhotoToCurrentList(nouvelleUri.toString());
            } else {
                Snackbar.make(coordinatorLayout, "L'image " + uri.getPath() + " n'a pas pu être récupérée.", Snackbar.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

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

    public void onGalleryClick() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        photoPickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(photoPickerIntent, "Choisissez les images à importer"), DIALOG_GALLERY_IMAGE);
    }

    @OnClick(R.id.add_photo)
    public void onClick(View v) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("Envie d'ajouter une nouvelle image ?")
                .setMessage("Vous pouvez prendre une nouvelle photo ou choisir une photo existante dans votre galerie.")
                .setPositiveButton("Nouvelle image", (dialog, which) -> onNewPictureClick())
                .setNegativeButton("Choisir depuis la galerie", (dialog, which) -> onGalleryClick())
                .setIcon(R.drawable.ic_add_a_photo_black_48dp)
                .show();

    }

    /**
     * Launch activity to take a picture
     */
    private void callCaptureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mFileUriTemp = generateNewUri();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUriTemp);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, DIALOG_REQUEST_IMAGE);
    }

    private Uri generateNewUri() {
        Uri uri;
        if (externalStorage) {
            uri = MediaUtility.createNewMediaFileUri(this, MediaType.IMAGE, viewModel.getUidUtilisateur());
        } else {
            File file = MediaUtility.saveInternalFile(this, MediaUtility.generateMediaName(MediaType.IMAGE, viewModel.getUidUtilisateur()));
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    /**
     * Display the different fields of the annonce
     *
     * @param annonce that we want to show
     */
    private void displayAnnonce(AnnonceEntity annonce) {
        // Récupération du titre, de la description et du prix
        textViewTitre.setText(annonce.getTitre());
        textViewDescription.setText(annonce.getDescription());
        textViewPrix.setText(String.valueOf(annonce.getPrix()));

        // Récupération et sélection dans le spinner de la bonne catégorie
        viewModel.getLiveDataListCategorie()
                .observe(this, categorieEntities -> {
                    if (categorieEntities != null && annonce.getIdCategorie() != null) {
                        for (CategorieEntity categorieEntity : categorieEntities) {
                            if (categorieEntity.getIdCategorie().equals(annonce.getIdCategorie())) {
                                spinnerCategorie.setSelection(categorieEntities.indexOf(categorieEntity));
                                break;
                            }
                        }
                    }
                });
    }
}
