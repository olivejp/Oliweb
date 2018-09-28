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
import android.support.v7.widget.AppCompatSpinner;
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
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.ui.activity.viewmodel.PostAnnonceActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.SpinnerAdapter;
import oliweb.nc.oliweb.ui.fragment.WorkImageFragment;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.MediaUtility;
import oliweb.nc.oliweb.utility.Utility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class PostAnnonceActivity extends AppCompatActivity {

    private static final String TAG = PostAnnonceActivity.class.getName();

    public static final int RC_POST_ANNONCE = 881;

    public static final String TAG_WORKING_IMAGE = "TAG_WORKING_IMAGE";
    public static final String BUNDLE_KEY_ID_ANNONCE = "ID_ANNONCE";
    public static final String BUNDLE_KEY_UID_ANNONCE = "BUNDLE_KEY_UID_ANNONCE";
    public static final String BUNDLE_UID_USER = "BUNDLE_UID_USER";
    public static final String BUNDLE_KEY_MODE = "MODE";

    public static final int DIALOG_REQUEST_IMAGE = 100;
    private static final int DIALOG_GALLERY_IMAGE = 200;
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 999;
    private static final int REQUEST_WRITE_EXTERNAL_PERMISSION_CODE = 888;
    private static final String SAVE_ANNONCE = "SAVE_ANNONCE";
    private static final String SAVE_FILE_URI_TEMP = "SAVE_FILE_URI_TEMP";

    private PostAnnonceActivityViewModel viewModel;

    private Uri mFileUriTemp;
    private Long idAnnonce;
    private boolean externalStorage;
    private String uidUser;
    private String uidAnnonce;
    private String mode;

    private ArrayList<CategorieEntity> listCategorieEntities;

    @BindView(R.id.spinner_categorie)
    AppCompatSpinner spinnerCategorie;

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
                viewModel.setCurrentAnnonce(savedInstanceState.getParcelable(SAVE_ANNONCE));
            }
            if (savedInstanceState.containsKey(SAVE_FILE_URI_TEMP)) {
                mFileUriTemp = savedInstanceState.getParcelable(SAVE_FILE_URI_TEMP);
            }

            // S'il y avait un fragment, je le remet
            if (savedInstanceState.containsKey(TAG_WORKING_IMAGE)) {
                Fragment frag = getSupportFragmentManager().getFragment(savedInstanceState, TAG_WORKING_IMAGE);
                if (frag != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.post_annonce_frame, frag, TAG_WORKING_IMAGE)
                            .commit();
                }
            }
        }

        Bundle bundle = (savedInstanceState != null) ? savedInstanceState : getIntent().getExtras();
        if (!checkAndCatchParameter(bundle)) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            setTitle(mode.equals(Constants.PARAM_CRE) ? "Ajouter une annonce" : "Modifier une annonce");
            initViewModel();
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
        Log.d(TAG, "Starting saveAnnonce");
        if (!checkIfAnnonceIsValid()) {
            return;
        }

        // Retrieve datas from the ui
        String titre = textViewTitre.getText().toString();
        String description = textViewDescription.getText().toString();
        int prix = Integer.parseInt(textViewPrix.getText().toString());
        boolean contactEmail = checkBoxEmail.isChecked();
        boolean contactMsg = checkBoxMsg.isChecked();
        boolean contactTel = checkBoxTel.isChecked();

        // Save the annonce to the local DB
        viewModel.saveAnnonce(titre, description, prix, uidUser, contactEmail, contactMsg, contactTel)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(annonce -> {
                    Log.d(TAG, "saveAnnonce.doOnSuccess annonce : " + annonce);
                    viewModel.savePhotos(annonce.getIdAnnonce())
                            .doOnSuccess(listPhotos -> {
                                Log.d(TAG, "savePhotos.doOnSuccess listPhotos : " + listPhotos);
                                setResult(RESULT_OK);
                                finish();
                            })
                            .doOnError(exception -> Log.e(TAG, "savePhotos.doOnError " + exception.getLocalizedMessage(), exception))
                            .subscribe();
                })
                .doOnError(throwable -> Log.e(TAG, "saveAnnonce.doOnError " + throwable.getLocalizedMessage(), throwable))
                .subscribe();
    }

    private boolean checkIfAnnonceIsValid() {
        boolean isValid = true;
        if (StringUtils.isEmpty(textViewTitre.getText().toString())) {
            textViewTitre.setError("Le titre est obligatoire.");
            isValid = false;
        }
        if (StringUtils.isEmpty(textViewDescription.getText().toString())) {
            textViewDescription.setError("La description est obligatoire.");
            isValid = false;
        }

        String prixStr = textViewPrix.getText().toString();
        if (StringUtils.isEmpty(prixStr) || Integer.parseInt(prixStr) <= 0) {
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
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == DIALOG_REQUEST_IMAGE) {
            resizePhotoThenInsertToCurrentList(mFileUriTemp);
        } else if (requestCode == DIALOG_GALLERY_IMAGE) {
            // Insertion multiple
            if (data.getClipData() != null) {
                int i = -1;
                ClipData.Item item;
                while (i++ < data.getClipData().getItemCount() - 1) {
                    if (viewModel.canHandleAnotherPhoto()) {
                        item = data.getClipData().getItemAt(i);
                        resizePhotoThenInsertToCurrentList(item.getUri());
                    }
                }
            } else {
                // Insertion simple
                if (data.getData() != null) {
                    resizePhotoThenInsertToCurrentList(data.getData());
                }
            }
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save the mode (CRE / MAJ)
        outState.putString(BUNDLE_KEY_MODE, mode);

        // Save the Id or Uid of the annonce
        if (idAnnonce != null) {
            outState.putLong(BUNDLE_KEY_ID_ANNONCE, idAnnonce);
        }
        if (uidAnnonce != null) {
            outState.putString(BUNDLE_KEY_UID_ANNONCE, uidAnnonce);
        }

        // Save the uid user
        outState.putString(BUNDLE_UID_USER, uidUser);


        // Save annonce infos
        String prixStr = textViewPrix.getText().toString();

        AnnoncePhotos currentAnnonce = viewModel.getCurrentAnnonce();
        currentAnnonce.annonceEntity.setTitre(textViewTitre.getText().toString());
        currentAnnonce.annonceEntity.setDescription(textViewDescription.getText().toString());
        currentAnnonce.annonceEntity.setPrix((StringUtils.isEmpty(prixStr)) ? 0 : Integer.parseInt(prixStr));
        currentAnnonce.annonceEntity.setContactByMsg(checkBoxMsg.isChecked() ? "O" : "N");
        currentAnnonce.annonceEntity.setContactByTel(checkBoxTel.isChecked() ? "O" : "N");
        currentAnnonce.annonceEntity.setContactByEmail(checkBoxEmail.isChecked() ? "O" : "N");
        currentAnnonce.annonceEntity.setIdCategorie(viewModel.getCurrentCategorie().getIdCategorie());
        outState.putParcelable(SAVE_ANNONCE, currentAnnonce);

        // Save the uri temporary
        if (mFileUriTemp != null && StringUtils.isNotEmpty(mFileUriTemp.toString())) {
            outState.putParcelable(SAVE_FILE_URI_TEMP, mFileUriTemp);
        }

        // Save the fragment if any
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
            builder = getBuilder();
            builder.setTitle(R.string.add_new_photo)
                    .setMessage(R.string.add_photo_question)
                    .setPositiveButton(R.string.take_new_picture, (dialog, which) -> onNewPictureClick())
                    .setNegativeButton(R.string.choose_from_galery, (dialog, which) -> onGalleryClick())
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

    @NonNull
    private AlertDialog.Builder getBuilder() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        return builder;
    }

    @OnLongClick(value = {R.id.view_1, R.id.view_2, R.id.view_3, R.id.view_4})
    public boolean onLongClick(View v) {
        if (v.getTag() != null) {
            AlertDialog.Builder builder;
            builder = getBuilder();

            builder.setTitle(R.string.delete_photo)
                    .setMessage(R.string.delete_photo_are_you_sure)
                    .setPositiveButton(R.string.yes, (dialog, which) -> viewModel.removePhotoFromCurrentList((PhotoEntity) v.getTag()))
                    .setNegativeButton(R.string.no, (dialog, which) -> {
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
    private boolean checkAndCatchParameter(Bundle bundle) {
        // Catch preferences
        externalStorage = SharedPreferencesHelper.getInstance(getApplicationContext()).getUseExternalStorage();

        // Récupération des paramètres
        if (bundle == null || !bundle.containsKey(BUNDLE_KEY_MODE) || bundle.getString(BUNDLE_KEY_MODE) == null) {
            Log.e(TAG, "Missing mandatory parameter");
            return false;
        } else {
            mode = bundle.getString(BUNDLE_KEY_MODE);
            if (mode != null && mode.equals(Constants.PARAM_MAJ) && !bundle.containsKey(BUNDLE_KEY_ID_ANNONCE) && !bundle.containsKey(BUNDLE_KEY_UID_ANNONCE)) {
                Log.e(TAG, "Aucun Id ou UID d'annonce passé en paramètre");
                return false;
            } else {
                idAnnonce = bundle.getLong(BUNDLE_KEY_ID_ANNONCE);
                uidAnnonce = bundle.getString(BUNDLE_KEY_UID_ANNONCE);
            }

            // Récupération du Uid de l'utilisateur
            uidUser = bundle.getString(BUNDLE_UID_USER);
            if (uidUser == null || uidUser.isEmpty()) {
                Log.e(TAG, "Missing UID parameter");
                return false;
            }
        }
        return true;
    }

    private void defineSpinnerCategorie(ArrayList<CategorieEntity> categorieEntities) {
        listCategorieEntities = categorieEntities;

        SpinnerAdapter adapter = new SpinnerAdapter(PostAnnonceActivity.this, listCategorieEntities);
        spinnerCategorie.setAdapter(adapter);
        spinnerCategorie.setOnItemSelectedListener(spinnerItemSelected);
    }

    private void changeUserContactMethod(UserEntity userEntity) {
        if (userEntity == null) {
            return;
        }
        if (userEntity.getEmail() == null || userEntity.getEmail().isEmpty()) {
            textCheckboxEmail.setVisibility(View.GONE);
            checkBoxEmail.setVisibility(View.GONE);
            checkBoxEmail.setChecked(false);
        }
        if (userEntity.getTelephone() == null || userEntity.getTelephone().isEmpty()) {
            textCheckboxTelephone.setVisibility(View.GONE);
            checkBoxTel.setVisibility(View.GONE);
            checkBoxTel.setChecked(false);
        }
    }

    private void initObservers() {
        // Alimentation du spinner avec la liste des catégories
        viewModel.getListCategorie().observeOnce(this::defineSpinnerCategorie);

        // Initialise les moyens de contacts de l'utilisateur selon ses données.
        viewModel.getConnectedUser(uidUser).observe(this, this::changeUserContactMethod);

        // Initialisation pour écouter les changements sur les photos
        viewModel.getLiveListPhoto().observe(this, this::displayPhotos);
    }

    private void initViewModel() {
        if (viewModel.getCurrentAnnonce() != null) {
            displayCurrentAnnonce();
        } else {
            if (mode.equals(Constants.PARAM_CRE)) {
                viewModel.createNewAnnonce();
            } else {
                if (idAnnonce != null) {
                    viewModel.getAnnonceById(idAnnonce).observe(this, annoncePhotos -> {
                        if (annoncePhotos != null) {
                            viewModel.setCurrentAnnonce(annoncePhotos);
                            displayCurrentAnnonce();
                        }
                    });
                }
                if (uidAnnonce != null) {
                    viewModel.getAnnonceByUid(uidAnnonce).observe(this, annoncePhotos -> {
                        if (annoncePhotos != null) {
                            viewModel.setCurrentAnnonce(annoncePhotos);
                            displayCurrentAnnonce();
                        }
                    });
                }
            }
        }
    }

    private void clearImageViews() {
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

    private void displayPhotos(List<PhotoEntity> photoEntities) {
        clearImageViews();
        if (photoEntities != null && !photoEntities.isEmpty()) {
            for (PhotoEntity photo : photoEntities) {
                if (!Utility.allStatusToAvoid().contains(photo.getStatut().getValue())) {
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

    private void resizePhotoThenInsertToCurrentList(Uri uriSrc) {
        Uri uriDst = viewModel.generateNewUri(externalStorage);
        if (uriDst != null) {
            if (MediaUtility.copyAndResizeUriImages(getApplicationContext(), uriSrc, uriDst)) {
                viewModel.addPhotoToCurrentList(uriDst.toString());
            } else {
                Snackbar.make(photo1, "L'image " + uriSrc.getPath() + " n'a pas pu être récupérée.", Snackbar.LENGTH_LONG).show();
            }
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
        mFileUriTemp = viewModel.generateNewUri(externalStorage);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUriTemp);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, DIALOG_REQUEST_IMAGE);
    }


    private void displayCurrentAnnonce() {
        AnnoncePhotos annoncePhotos = viewModel.getCurrentAnnonce();

        // Display annonce
        AnnonceEntity annonce = annoncePhotos.getAnnonceEntity();
        textViewTitre.setText(annonce.getTitre());
        textViewDescription.setText(annonce.getDescription());
        textViewPrix.setText(String.valueOf(annonce.getPrix()));
        checkBoxMsg.setChecked(annonce.getContactByMsg() != null && annonce.getContactByMsg().equals("O"));
        checkBoxEmail.setChecked(annonce.getContactByEmail() != null && annonce.getContactByEmail().equals("O"));
        checkBoxTel.setChecked(annonce.getContactByTel() != null && annonce.getContactByTel().equals("O"));

        // Display the categorie
        selectCategorieInSpinner(annonce.getIdCategorie());

        // Display the photos
        displayPhotos(annoncePhotos.getPhotos());
    }

    private void selectCategorieInSpinner(Long idCategorie) {
        viewModel.getListCategorie().observeOnce(listCategorie -> {
            if (listCategorie != null && idCategorie != null) {
                for (CategorieEntity categorieEntity : listCategorie) {
                    if (categorieEntity.getId().equals(idCategorie)) {
                        spinnerCategorie.setSelection(listCategorie.indexOf(categorieEntity), true);
                        break;
                    }
                }
            }
        });
    }
}
