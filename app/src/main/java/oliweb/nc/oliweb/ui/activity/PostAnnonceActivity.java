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
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.service.sync.SyncService;
import oliweb.nc.oliweb.ui.activity.viewmodel.PostAnnonceActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.SpinnerAdapter;
import oliweb.nc.oliweb.ui.fragment.WorkImageFragment;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.MediaUtility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class PostAnnonceActivity extends AppCompatActivity {

    private static final String TAG = PostAnnonceActivity.class.getName();

    public static final int RC_POST_ANNONCE = 881;

    public static final String TAG_WORKING_IMAGE = "TAG_WORKING_IMAGE";
    public static final String BUNDLE_KEY_ID_ANNONCE = "ID_ANNONCE";
    public static final String BUNDLE_KEY_UID_ANNONCE = "BUNDLE_KEY_UID_ANNONCE";
    public static final String BUNDLE_KEY_MODE = "MODE";

    public static final int DIALOG_REQUEST_IMAGE = 100;
    private static final int DIALOG_GALLERY_IMAGE = 200;
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 999;
    private static final int REQUEST_WRITE_EXTERNAL_PERMISSION_CODE = 888;
    private static final String SAVE_ANNONCE = "SAVE_ANNONCE";
    private static final String SAVE_LIST_PHOTO = "SAVE_LIST_PHOTO";
    private static final String SAVE_FILE_URI_TEMP = "SAVE_FILE_URI_TEMP";

    private PostAnnonceActivityViewModel viewModel;
    private Uri mFileUriTemp;
    private long idAnnonce = 0;
    private boolean externalStorage;
    private String uidUser;
    private String uidAnnonce;
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

    @BindView(R.id.checkbox_email)
    SwitchCompat checkBoxEmail;

    @BindView(R.id.checkbox_telephone)
    SwitchCompat checkBoxTel;

    @BindView(R.id.checkbox_message)
    SwitchCompat checkBoxMsg;

    @BindView(R.id.text_checkbox_email)
    TextView textCheckboxEmail;

    @BindView(R.id.text_checkbox_telephone)
    TextView textCheckboxTelephone;

    List<Pair<ImageView, FrameLayout>> arrayImageViews = new ArrayList<>();

    // Evenement sur le spinner
    private AdapterView.OnItemSelectedListener spinnerItemSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            viewModel.setCurrentCategorie((CategorieEntity) parent.getItemAtPosition(position));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing
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

        arrayImageViews.add(new Pair<>(photo1, view1));
        arrayImageViews.add(new Pair<>(photo2, view2));
        arrayImageViews.add(new Pair<>(photo3, view3));
        arrayImageViews.add(new Pair<>(photo4, view4));

        initObservers();

        // Sur l'action finale du prix on va sauvegarder l'annonce.
        textViewPrix.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveAnnonce();
                return true;
            }
            return false;
        });

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SAVE_ANNONCE)) {
                viewModel.setAnnonce(savedInstanceState.getParcelable(SAVE_ANNONCE));
            }

            if (savedInstanceState.containsKey(SAVE_FILE_URI_TEMP)) {
                mFileUriTemp = savedInstanceState.getParcelable(SAVE_FILE_URI_TEMP);
            }

            if (savedInstanceState.containsKey(SAVE_LIST_PHOTO)) {
                initPhotosViews(savedInstanceState.getParcelableArrayList(SAVE_LIST_PHOTO));
            }

            // S'il y avait un fragment, je le remet
            if (savedInstanceState.containsKey(TAG_WORKING_IMAGE)) {
                Fragment frag = getSupportFragmentManager().getFragment(savedInstanceState, TAG_WORKING_IMAGE);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.post_annonce_frame, frag, TAG_WORKING_IMAGE)
                        .commit();
            }
        }

        Bundle bundle = (savedInstanceState != null) ? savedInstanceState : getIntent().getExtras();
        if (!catchAndCheckParameter(bundle)) {
            setResult(RESULT_CANCELED);
            finish();
        } else if (savedInstanceState == null) {
            initViewModelDataDependingOnMode(bundle);
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
        if (idItem == R.id.menu_post_valid) {
            saveAnnonce();
            return true;
        }
        if (idItem == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveAnnonce() {
        if (!checkIfAnnonceIsValid()) {
            return;
        }

        // Retrieve datas from the ui
        String titre = textViewTitre.getText().toString();
        String description = textViewDescription.getText().toString();
        int prix = Integer.parseInt(textViewPrix.getText().toString());

        // Save the annonce to the local DB
        viewModel.saveAnnonceToDb(titre, description, prix, uidUser, checkBoxEmail.isChecked(), checkBoxMsg.isChecked(), checkBoxTel.isChecked(), dataReturn -> {
            if (dataReturn.isSuccessful()) {
                if (NetworkReceiver.checkConnection(this)) {
                    SyncService.launchSynchroForAll(getApplicationContext());
                }
                setResult(RESULT_OK);
            } else {
                setResult(RESULT_CANCELED);
            }
            finish();
        });
    }

    private boolean checkIfAnnonceIsValid() {
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
        if (!checkBoxTel.isChecked() && !checkBoxEmail.isChecked() && !checkBoxMsg.isChecked()) {
            checkBoxMsg.setError("");
            Toast.makeText(this, "Un moyen de contact obligatoire", Toast.LENGTH_LONG).show();
            isValid = false;
        }
        return isValid;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callCaptureIntent();
        }

        if (requestCode == REQUEST_WRITE_EXTERNAL_PERMISSION_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callGalleryIntent();
        }
    }

    @SuppressWarnings("squid:S3776")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_CANCELED) {
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
                                    insertFromGallery(item.getUri());
                                }
                            }
                        } else {
                            // Insertion simple
                            Uri uri = data.getData();
                            if (uri != null) {
                                insertFromGallery(uri);
                            }
                        }
                    }
                    break;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(BUNDLE_KEY_ID_ANNONCE, idAnnonce);
        outState.putString(BUNDLE_KEY_UID_ANNONCE, uidAnnonce);
        outState.putString(BUNDLE_KEY_MODE, mode);
        outState.putParcelable(SAVE_FILE_URI_TEMP, mFileUriTemp);
        outState.putParcelable(SAVE_ANNONCE, viewModel.getAnnonce());
        outState.putParcelableArrayList(SAVE_LIST_PHOTO, viewModel.getListPhoto());
        Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG_WORKING_IMAGE);
        if (frag != null) {
            getSupportFragmentManager().putFragment(outState, TAG_WORKING_IMAGE, frag);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
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
                    .addSharedElement(v, getString(R.string.image_working_transition))
                    .replace(R.id.post_annonce_frame, workImageFragment, TAG_WORKING_IMAGE)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @OnLongClick(value = {R.id.view_1, R.id.view_2, R.id.view_3, R.id.view_4})
    public boolean onLongClick(View v) {
        if (v.getTag() != null) {
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth);
            } else {
                builder = new AlertDialog.Builder(this);
            }

            builder.setTitle("Supprimer une photo")
                    .setMessage("Etes vous sûr de vouloir supprimer la photo ?")
                    .setPositiveButton("Oui", (dialog, which) -> viewModel.removePhotoToCurrentList((PhotoEntity) v.getTag()))
                    .setNegativeButton("Non", (dialog, which) -> {
                    })
                    .setIcon(R.drawable.ic_add_a_photo_black_48dp)
                    .show();
            return true;
        }
        return false;
    }

    /**
     * Method called in the onCreate method to check if every mandatory arguments is present
     *
     * @param bundle to check
     * @return true if everything is ok, false otherwise
     */
    private boolean catchAndCheckParameter(Bundle bundle) {
        // Catch preferences
        externalStorage = SharedPreferencesHelper.getInstance(getApplicationContext()).getUseExternalStorage();

        // Récupération du Uid de l'utilisateur connecté
        uidUser = SharedPreferencesHelper.getInstance(this).getUidFirebaseUser();
        if (uidUser == null || uidUser.isEmpty()) {
            Log.e(TAG, "Missing UID parameter");
            return false;
        }

        // Récupération des paramètres
        if (bundle == null || !bundle.containsKey(BUNDLE_KEY_MODE) || bundle.getString(BUNDLE_KEY_MODE) == null) {
            Log.e(TAG, "Missing mandatory parameter");
            return false;
        } else {
            mode = bundle.getString(BUNDLE_KEY_MODE);
            if (mode != null && mode.equals(Constants.PARAM_MAJ) && !bundle.containsKey(BUNDLE_KEY_ID_ANNONCE) && !bundle.containsKey(BUNDLE_KEY_UID_ANNONCE)) {
                Log.e(TAG, "Aucun Id ou UID d'annonce passé en paramètre");
                return false;
            }
        }
        return true;
    }

    private void initObservers() {
        spinnerCategorie.setOnItemSelectedListener(spinnerItemSelected);

        // Récupération dynamique de la liste des photos
        viewModel.getLiveListPhoto().observe(this, this::initPhotosViews);

        // Alimentation du spinner avec la liste des catégories
        viewModel.getLiveDataListCategorie()
                .observe(this, categorieEntities -> {
                    if (categorieEntities != null && !categorieEntities.isEmpty()) {
                        SpinnerAdapter adapter = new SpinnerAdapter(this, categorieEntities);
                        spinnerCategorie.setAdapter(adapter);
                    }
                });

        viewModel.getConnectedUser().observe(this, utilisateurEntity -> {
            if (utilisateurEntity != null) {
                if (utilisateurEntity.getEmail() == null || utilisateurEntity.getEmail().isEmpty()) {
                    textCheckboxEmail.setVisibility(View.GONE);
                    checkBoxEmail.setVisibility(View.GONE);
                    checkBoxEmail.setChecked(false);
                }
                if (utilisateurEntity.getTelephone() == null || utilisateurEntity.getTelephone().isEmpty()) {
                    textCheckboxTelephone.setVisibility(View.GONE);
                    checkBoxTel.setVisibility(View.GONE);
                    checkBoxTel.setChecked(false);
                }
            }
        });
    }

    private void initViewModelDataDependingOnMode(Bundle bundle) {
        if (mode.equals(Constants.PARAM_CRE)) {
            setTitle("Ajouter une annonce");
            viewModel.createNewAnnonce();
        } else if (mode.equals(Constants.PARAM_MAJ)) {
            setTitle("Modifier une annonce");
            if (bundle.containsKey(BUNDLE_KEY_ID_ANNONCE)) {
                idAnnonce = bundle.getLong(BUNDLE_KEY_ID_ANNONCE);
                viewModel.findAnnonceById(idAnnonce).observe(this, this::initAnnonce);
            }
            if (bundle.containsKey(BUNDLE_KEY_UID_ANNONCE)) {
                uidAnnonce = bundle.getString(BUNDLE_KEY_UID_ANNONCE);
                viewModel.findAnnonceByUid(uidAnnonce).observe(this, this::initAnnonce);
            }
        }
    }

    private void initAnnonce(AnnonceEntity annonceEntity) {
        if (annonceEntity != null) {
            viewModel.setAnnonce(annonceEntity);

            // Récupération des photos de cette annonce dans l'adapter
            viewModel.getListPhotoByIdAnnonce(annonceEntity.getIdAnnonce())
                    .observe(PostAnnonceActivity.this, photoEntities -> {
                        if (photoEntities != null && !photoEntities.isEmpty()) {
                            initPhotosViews(new ArrayList<>(photoEntities));
                        }
                    });
            displayAnnonce(annonceEntity);
        }
    }

    private void initImageViewToDefault() {
        for (Pair pair : arrayImageViews) {
            ImageView imageView = (ImageView) pair.first;
            FrameLayout frame = (FrameLayout) pair.second;
            if (imageView != null) {
                imageView.setImageResource(R.drawable.ic_add_a_photo_grey_900_48dp);
            }
            if (frame != null) {
                frame.setTag(null);
            }
        }
    }

    private void initPhotosViews(ArrayList<PhotoEntity> photoEntities) {
        initImageViewToDefault();
        if (photoEntities != null && !photoEntities.isEmpty()) {
            viewModel.setListPhoto(photoEntities);
            for (PhotoEntity photo : photoEntities) {
                if (!photo.getStatut().equals(StatusRemote.TO_DELETE)) {
                    boolean insertion = false;
                    int i = 0;
                    while (!insertion && i < arrayImageViews.size()) {
                        insertion = insertPhotoInImageView(arrayImageViews.get(i), photo);
                        i++;
                    }
                }
            }
        }
    }

    private boolean insertPhotoInImageView(Pair<ImageView, FrameLayout> pair, PhotoEntity photoEntity) {
        ImageView imageView = pair.first;
        FrameLayout frameLayout = pair.second;
        if (imageView != null && frameLayout != null && frameLayout.getTag() == null) {
            frameLayout.setTag(photoEntity);
            GlideApp.with(imageView)
                    .load(Uri.parse(photoEntity.getUriLocal()))
                    .apply(RequestOptions.circleCropTransform())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(imageView);
            return true;
        }
        return false;
    }

    /**
     * Le paramètre uri représente une image dans la gallerie.
     * nouvelleUri correspond à l'emplacement de destination de cette image.
     * car on veut la déplacer dans le répertoire spécifique à Oliweb.
     * On va donc copier l'image présente dans uri et la mettre dans nouvelleUri.
     *
     * @param uri
     */
    private void insertFromGallery(Uri uri) {
        try {
            Uri newUri = generateNewUri();
            if (newUri != null) {
                if (MediaUtility.copyAndResizeUriImages(getApplicationContext(), uri, newUri)) {
                    viewModel.addPhotoToCurrentList(newUri.toString());
                } else {
                    Snackbar.make(photo1, "L'image " + uri.getPath() + " n'a pas pu être récupérée.", Snackbar.LENGTH_LONG).show();
                }
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

    @Nullable
    private Uri generateNewUri() {
        Pair<Uri, File> pair = MediaUtility.createNewMediaFileUri(this, externalStorage, MediaUtility.MediaType.IMAGE, uidUser);
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

        checkBoxMsg.setChecked(annonce.getContactByMsg() != null && annonce.getContactByMsg().equals("O"));
        checkBoxEmail.setChecked(annonce.getContactByEmail() != null && annonce.getContactByEmail().equals("O"));
        checkBoxTel.setChecked(annonce.getContactByTel() != null && annonce.getContactByTel().equals("O"));

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
