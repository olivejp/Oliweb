package oliweb.nc.oliweb.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.media.MediaUtility;

public class WorkImageActivity extends AppCompatActivity {

    private static final String TAG = WorkImageActivity.class.getName();

    public static final int RESULT_ACTIVITY_ERROR = 999;

    public static final String BUNDLE_KEY_MODE = "MODE";
    public static final String BUNDLE_KEY_ID = "ID";
    public static final String BUNDLE_IN_PATH_IMAGE = "BUNDLE_IN_PATH_IMAGE";
    public static final String BUNDLE_OUT_IMAGE = "BITMAP_OUT";
    public static final String BUNDLE_EXTERNAL_STORAGE = "EXTERNAL_STORAGE";
    public static final String BUNDLE_OUT_URI_IMAGE = "BUNDLE_OUT_URI_IMAGE";

    @BindView(R.id.work_image_button_delete_image)
    Button onDeleteListener;
    @BindView(R.id.work_image_button_rotate_image)
    Button onRotateListener;
    @BindView(R.id.work_image_button_save_image)
    Button onSaveListener;
    @BindView(R.id.work_image_view)
    ImageView workimageview;

    private String pMode; // Le mode Création/Modification qu'on a reçu en paramètre
    private Bitmap pBitmap; // Le bitmap qu'on va travailler et sauver
    private int pIdPhoto; // Id de la photo dans l'arrayList
    private Uri pUriImage; // Uri de l'image à modifier (uniquement dans le cas d'une modification)
    private boolean pExternalStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_work_image);
        ButterKnife.bind(this);

        // Récupération de l'action bar.
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(false);
            ab.setDisplayShowHomeEnabled(false);
        }

        // Soit on vient pour la première fois et savedInstance = null
        // soit on revient sur cette activity et savedInstance contient déjà toutes les données que l'on attend.
        Bundle bundleOrigine;
        if (savedInstanceState != null) {
            bundleOrigine = savedInstanceState;
        } else {
            bundleOrigine = getIntent().getExtras();
        }

        // Vérification des paramètres obligatoires
        if (bundleOrigine == null) {
            Log.e(TAG, "bundleOrigine vide");
            setResult(RESULT_ACTIVITY_ERROR);
            finish();
        }

        if (!bundleOrigine.containsKey(BUNDLE_KEY_MODE)) {
            Log.e(TAG, "bundleOrigine ne contient pas le paramètre obligatoire BUNDLE_KEY_MODE");
            setResult(RESULT_ACTIVITY_ERROR);
            finish();
        }

        if (!bundleOrigine.containsKey(BUNDLE_EXTERNAL_STORAGE)) {
            Log.e(TAG, "bundleOrigine ne contient pas le paramètre obligatoire BUNDLE_EXTERNAL_STORAGE");
            setResult(RESULT_ACTIVITY_ERROR);
            finish();
        }

        pExternalStorage = bundleOrigine.getBoolean(BUNDLE_EXTERNAL_STORAGE);

        pMode = bundleOrigine.getString(BUNDLE_KEY_MODE);
        if (pMode == null) {
            Log.e(TAG, "bundleOrigine ne contient pas le paramètre obligatoire BUNDLE_KEY_MODE");
            setResult(RESULT_ACTIVITY_ERROR);
            finish();
        }

        // Changement des titres du bouton Supprimer/Annuler selon le mode dans lequel on entre
        // En mode MAJ, on récupère l'ID dans l'arraylist du bitmap
        if (pMode.equals(Constants.PARAM_CRE)) {
            onDeleteListener.setText("Annuler");
        } else if (pMode.equals(Constants.PARAM_MAJ)) {
            onDeleteListener.setText("Supprimer");
        }

        // Récupération de l'iD de l'image
        if (bundleOrigine.containsKey(BUNDLE_KEY_ID)) {
            pIdPhoto = bundleOrigine.getInt(BUNDLE_KEY_ID);
        }

        // On récupère TOUJOURS le path d'une image qu'on veut modifier
        if (bundleOrigine.containsKey(BUNDLE_IN_PATH_IMAGE)) {
            pUriImage = bundleOrigine.getParcelable(BUNDLE_IN_PATH_IMAGE);
            pBitmap = MediaUtility.getBitmapFromUri(this, pUriImage);
            workimageview.setImageBitmap(pBitmap);
        }

        // Creation des variables de retour
        Intent intentRetour = new Intent();
        Bundle bundleRetour = new Bundle();

        // Création d'un listener pour faire tourner l'image
        onRotateListener.setOnClickListener(v -> {
            if (pBitmap != null) {
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(pBitmap, pBitmap.getWidth(), pBitmap.getHeight(), true);
                pBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
                workimageview.setImageBitmap(pBitmap);
            }
        });

        // Création d'un listener pour sauver et quitter
        onSaveListener.setOnClickListener(v -> {
            if (pBitmap == null) {
                Log.e(TAG, "pBitmap ne devrait jamais être null");
                setResult(RESULT_ACTIVITY_ERROR);
                finish();
            } else {
                // On sauvegarde la bitmap dans le path reçu en paramètre
                if (!MediaUtility.saveBitmapToUri(pBitmap, pUriImage)) {
                    Log.e(TAG, "Erreur lors de la sauvegarde de l'image");
                    setResult(RESULT_ACTIVITY_ERROR);
                    finish();
                } else {
                    // En mode MAJ on va aussi renvoyer la position dans l'ArrayList de notre photo
                    // Pour que le programme appelant puisse faire sa mise à jour
                    if (pMode.equals(Constants.PARAM_MAJ)) {
                        bundleRetour.putInt(BUNDLE_KEY_ID, pIdPhoto);
                    }
                    bundleRetour.putParcelable(BUNDLE_OUT_URI_IMAGE, pUriImage);
                    intentRetour.putExtras(bundleRetour);
                    setResult(Activity.RESULT_OK, intentRetour);
                    finish();
                }
            }
        });

        // Création d'un listener pour annuler ou supprimer l'image
        onDeleteListener.setOnClickListener(v -> {
            // En mode MAJ on va aussi renvoyer la position dans l'ArrayList de notre photo
            if (pMode.equals(Constants.PARAM_MAJ)) {
                bundleRetour.putInt(BUNDLE_KEY_ID, pIdPhoto);
            }
            intentRetour.putExtras(bundleRetour);
            setResult(Activity.RESULT_CANCELED, intentRetour);
            finish();
        });
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_KEY_MODE, pMode);

        // Sauvegarde des paramètres
        if (pMode != null) {
            if (pMode.equals(Constants.PARAM_CRE)) {
                // On est en mode Mise à jour
            } else if (pMode.equals(Constants.PARAM_MAJ)) {
                // En mise à jour on a besoin du bitmap et de son id (position dans l'arrayList)
            }
        }
    }
}
