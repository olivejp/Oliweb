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
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;

import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.SharedPreferencesHelper;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.media.MediaType;
import oliweb.nc.oliweb.media.MediaUtility;
import oliweb.nc.oliweb.ui.activity.viewmodel.PostAnnonceActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.SpinnerAdapter;
import oliweb.nc.oliweb.ui.fragment.WorkImageFragment;
import oliweb.nc.oliweb.ui.glide.GlideApp;

public class PostAnnonceActivity extends AppCompatActivity {

    private static final String TAG = PostAnnonceActivity.class.getName();

    public static final int RC_POST_ANNONCE = 881;

    public static final String TAG_WORKING_IMAGE = "TAG_WORKING_IMAGE";
    public static final String BUNDLE_KEY_ID_ANNONCE = "ID_ANNONCE";
    public static final String BUNDLE_KEY_MODE = "MODE";

    public static final int DIALOG_REQUEST_IMAGE = 100;
    private static final int DIALOG_GALLERY_IMAGE = 200;
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 999;
    private static final int REQUEST_WRITE_EXTERNAL_PERMISSION_CODE = 888;

    private PostAnnonceActivityViewModel viewModel;
    private Uri mFileUriTemp;
    private long idAnnonce = 0;
    private boolean externalStorage;
    private String uidUser;
    private String mode;

    @BindView(R.id.spinner_categorie)
    Spinner spinnerCategorie;

    @BindView(R.id.edit_titre_annonce)
    EditText textViewTitre;

    @BindView(R.id.edit_description_annonce)
    EditText textViewDescription;

    @BindView(R.id.edit_prix_annonce)
    EditText textViewPrix;

    @BindView(R.id.photo_1)
    ImageView photo1;

    @BindView(R.id.photo_2)
    ImageView photo2;

    @BindView(R.id.photo_3)
    ImageView photo3;

    @BindView(R.id.photo_4)
    ImageView photo4;

    @BindView(R.id.view_1)
    FrameLayout view1;

    @BindView(R.id.view_2)
    FrameLayout view2;

    @BindView(R.id.view_3)
    FrameLayout view3;

    @BindView(R.id.view_4)
    FrameLayout view4;

    List<Pair<ImageView, FrameLayout>> arrayImageViews = new ArrayList<>();

    // Evenement sur le spinner
    private AdapterView.OnItemSelectedListener spinnerItemSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            viewModel.setCurrentCategorie((CategorieEntity) parent.getItemAtPosition(position));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ViewModel creation
        viewModel = ViewModelProviders.of(this).get(PostAnnonceActivityViewModel.class);

        // View creation
        setContentView(R.layout.activity_post_annonce);
        ButterKnife.bind(this);

        // Catch preferences
        externalStorage = SharedPreferencesHelper.getInstance(getApplicationContext()).getUseExternalStorage();

        arrayImageViews.add(new Pair<>(photo1, view1));
        arrayImageViews.add(new Pair<>(photo2, view2));
        arrayImageViews.add(new Pair<>(photo3, view3));
        arrayImageViews.add(new Pair<>(photo4, view4));

        // Alimentation du spinner avec la liste des catégories
        viewModel.getLiveDataListCategorie()
                .observe(this, categorieEntities -> {
                    if (categorieEntities != null && !categorieEntities.isEmpty()) {
                        SpinnerAdapter adapter = new SpinnerAdapter(this, categorieEntities);
                        spinnerCategorie.setAdapter(adapter);
                    }
                });

        spinnerCategorie.setOnItemSelectedListener(spinnerItemSelected);

        // Récupération dynamique de la liste des photos
        viewModel.getLiveListPhoto().observe(this, this::initPhotos);

        // Récupération du Uid de l'utilisateur connecté
        uidUser = SharedPreferencesHelper.getInstance(this).getUidFirebaseUser();
        if (uidUser == null || uidUser.isEmpty()) {
            Log.e(TAG, "Missing UID parameter");
            setResult(RESULT_CANCELED);
            finish();
        }

        // Récupération des paramètres
        Bundle bundle;
        if (savedInstanceState != null) {
            bundle = savedInstanceState;
        } else {
            bundle = getIntent().getExtras();
        }
        if (bundle == null || !bundle.containsKey(BUNDLE_KEY_MODE) || bundle.getString(BUNDLE_KEY_MODE) == null) {
            Log.e(TAG, "Missing mandatory parameter");
            setResult(RESULT_CANCELED);
            finish();
        } else {
            mode = bundle.getString(BUNDLE_KEY_MODE);
            if (mode.equals(Constants.PARAM_CRE)) {
                viewModel.createNewAnnonce();
            } else if (mode.equals(Constants.PARAM_MAJ)) {
                // On a une annonce à mettre à jour
                if (!bundle.containsKey(BUNDLE_KEY_ID_ANNONCE)) {
                    Log.e(TAG, "Aucun Id d'annonce passé en paramètre");
                    setResult(RESULT_CANCELED);
                    finish();
                }
                idAnnonce = bundle.getLong(BUNDLE_KEY_ID_ANNONCE);
                viewModel.findAnnonceById(idAnnonce).observe(this, annonceEntity -> {
                    if (annonceEntity != null) {
                        viewModel.setAnnonce(annonceEntity);

                        // Récupération des photos de cette annonce dans l'adapter
                        viewModel.getListPhotoByIdAnnonce(annonceEntity.getIdAnnonce())
                                .observe(PostAnnonceActivity.this, this::initPhotos);

                        displayAnnonce(annonceEntity);
                    }
                });
            }
        }

        // S'il y avait un fragment, je le remet
        if (savedInstanceState != null && savedInstanceState.containsKey(TAG_WORKING_IMAGE)) {
            Fragment frag = getSupportFragmentManager().getFragment(savedInstanceState, TAG_WORKING_IMAGE);
            getSupportFragmentManager().beginTransaction().replace(R.id.post_annonce_frame, frag, TAG_WORKING_IMAGE).addToBackStack(null).commit();
        }
    }

    /**
     * Try to drop photos in the correct imageView
     *
     * @param photoEntities
     */
    private void initPhotos(List<PhotoEntity> photoEntities) {

        // Init all default Tag to null
        for (Pair pair : arrayImageViews) {
            ImageView imageView = (ImageView) pair.first;
            FrameLayout frame = (FrameLayout) pair.second;

            frame.setTag(null);
            imageView.setImageResource(R.drawable.ic_add_a_photo_grey_900_48dp);
        }

        if (photoEntities != null && !photoEntities.isEmpty()) {
            viewModel.setListPhoto(photoEntities);
            for (PhotoEntity photo : photoEntities) {
                boolean insertion = false;
                int i = 0;
                while (!insertion && i < 4) {
                    insertion = insertPhotoInImageView(arrayImageViews.get(i), photo);
                    i++;
                }
            }
        }
    }

    /**
     * If the imageView has no tag then insert photo and return True, return False otherwise.
     *
     * @param imageView
     * @param photoEntity
     * @return
     */
    private boolean insertPhotoInImageView(Pair<ImageView, FrameLayout> pair, PhotoEntity photoEntity) {
        ImageView imageView = pair.first;
        FrameLayout frameLayout = pair.second;
        if (frameLayout.getTag() == null) {
//            try {
            frameLayout.setTag(photoEntity);
            GlideApp.with(this)
                    .applyDefaultRequestOptions(RequestOptions.circleCropTransform())
                    .load(Uri.parse(photoEntity.getUriLocal()))
                    .placeholder(R.drawable.ic_add_a_photo_grey_900_48dp)
                    .into(imageView);

//                InputStream in = getContentResolver().openInputStream();
//                Bitmap bitmap = BitmapFactory.decodeStream(in);
//                imageView.setImageBitmap(bitmap);
            return true;
//            } catch (FileNotFoundException e) {
//                Log.e(TAG, e.getMessage(), e);
//            }
        }
        return false;
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

                    // Retrieve datas from the ui
                    String titre = textViewTitre.getText().toString();
                    String description = textViewDescription.getText().toString();
                    int prix = Integer.parseInt(textViewPrix.getText().toString());

                    // Save the annonce
                    viewModel.saveAnnonce(titre, description, prix, uidUser, dataReturn -> {
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
        if (requestCode == REQUEST_WRITE_EXTERNAL_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callGalleryIntent();
            }
        }
    }

    @SuppressWarnings("squid:S3776")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DIALOG_REQUEST_IMAGE:
                if (resultCode == RESULT_OK) {
                    try {
                        if (MediaUtility.copyAndResizeUriImages(this, mFileUriTemp, mFileUriTemp)) {
                            viewModel.addPhotoToCurrentList(mFileUriTemp.toString());
                        } else {
                            Snackbar.make(photo1, "L'image " + mFileUriTemp.getPath() + " n'a pas pu être récupérée.", Snackbar.LENGTH_LONG).show();
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
                            if (viewModel.canHandleAnotherPhoto()) {
                                item = data.getClipData().getItemAt(i);
                                insertionFromGallery(item.getUri());
                            }
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(BUNDLE_KEY_ID_ANNONCE, idAnnonce);
        outState.putString(BUNDLE_KEY_MODE, mode);
        Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG_WORKING_IMAGE);
        if (frag != null) {
            getSupportFragmentManager().putFragment(outState, TAG_WORKING_IMAGE, frag);
        }
        super.onSaveInstanceState(outState);
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
                Snackbar.make(photo1, "L'image " + uri.getPath() + " n'a pas pu être récupérée.", Snackbar.LENGTH_LONG).show();
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

    @OnClick(value = {R.id.view_1, R.id.view_2, R.id.view_3, R.id.view_4})
    public void onClick(View v) {
        if (v.getTag() == null) {
            // Mode création d'une nouvelle photo
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
        } else {
            // Mode mise à jour
            PhotoEntity photoEntity = (PhotoEntity) v.getTag();
            viewModel.setUpdatedPhoto(photoEntity);
            WorkImageFragment workImageFragment = new WorkImageFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.post_annonce_frame, workImageFragment, TAG_WORKING_IMAGE)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @OnLongClick(value = {R.id.view_1, R.id.view_2, R.id.view_3, R.id.view_4})
    public boolean onLongClick(View v) {
        return v.getTag() != null && viewModel.removePhotoToCurrentList((PhotoEntity) v.getTag());
    }

    public void onGalleryClick() {
        // Demande de la permission pour utiliser la camera
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_PERMISSION_CODE);
            } else {
                callGalleryIntent();
            }
        } else {
            callGalleryIntent();
        }


    }

    private void callGalleryIntent() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        photoPickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(photoPickerIntent, "Choisissez les images à importer"), DIALOG_GALLERY_IMAGE);
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
        Pair<Uri, File> pair = MediaUtility.createNewMediaFileUri(this, externalStorage, MediaType.IMAGE, uidUser);
        if (pair != null && pair.first != null) {
            return pair.first;
        } else {
            Log.e(TAG, "generateNewUri() : MediaUtility a renvoyé une pair null");
            return null;
        }
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
