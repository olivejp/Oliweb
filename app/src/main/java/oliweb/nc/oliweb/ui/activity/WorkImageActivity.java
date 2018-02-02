package oliweb.nc.oliweb.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;

public class WorkImageActivity extends AppCompatActivity {

    private static final String TAG = WorkImageActivity.class.getName();

    public static final String BUNDLE_KEY_MODE = "MODE";
    public static final String BUNDLE_KEY_ID = "ID";
    public static final String BUNDLE_IN_IMAGE = "BITMAP_IN";
    public static final String BUNDLE_OUT_IMAGE = "BITMAP_OUT";
    public static final String BUNDLE_OUT_MAJ = "MAJ_IMAGE";

    @BindView(R.id.work_image_button_delete_image)
    Button workimagebuttondelete;
    @BindView(R.id.work_image_button_rotate_image)
    Button workimagebuttonrotate;
    @BindView(R.id.work_image_button_save_image)
    Button workimagebuttonsave;
    @BindView(R.id.work_image_view)
    ImageView workimageview;

    private String pMode; // Le mode Création/Modification qu'on a reçu en paramètre
    private Bitmap pBitmap; // Le bitmap qu'on va travailler et renvoyer
    private int pIdPhoto; // Id de la photo dans l'arrayList
    private byte[] pByteArray;
    private ByteArrayOutputStream pStream = new ByteArrayOutputStream();
    private boolean pReturnByteArray = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_work_image);
        ButterKnife.bind(this);

        // Creation des variables de retour
        Intent intentRetour = new Intent();
        Bundle bundleRetour = new Bundle();

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

        // Récupération des paramètres
        if (bundleOrigine != null && bundleOrigine.containsKey(BUNDLE_KEY_MODE)) {
            pMode = bundleOrigine.getString(BUNDLE_KEY_MODE);
            if (pMode != null) {
                // Changement des titres du bouton Supprimer/Annuler selon le mode dans lequel on entre
                // En mode MAJ, on récupère l'ID dans l'arraylist du bitmap
                switch (pMode) {
                    case Constants.PARAM_CRE:
                        workimagebuttondelete.setText("Annuler");
                        break;
                    case Constants.PARAM_MAJ:
                        pIdPhoto = bundleOrigine.getInt(BUNDLE_KEY_ID);
                        workimagebuttondelete.setText("Supprimer");
                        break;
                }

                // On va récupérer le bytearray, puis la bitmap qui est rattachée
                pByteArray = bundleOrigine.getByteArray(BUNDLE_IN_IMAGE);
                if (pByteArray != null) {
                    pBitmap = BitmapFactory.decodeByteArray(pByteArray, 0, pByteArray.length);
                }

                // On affecte le bitmap qu'on a récupéré dans l'ImageView
                workimageview.setImageBitmap(pBitmap);
            } else {
                Log.e(TAG, "pMode non renseigné");
                finish();
            }
        } else {
            Log.e(TAG, "bundleOrigine vide ou ne contient pas un paramètre BUNDLE_KEY_MODE");
            finish();
        }

        // Création d'un listener pour faire tourner l'image
        workimagebuttonrotate.setOnClickListener(v -> {
            if (pBitmap != null) {
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(pBitmap, pBitmap.getWidth(), pBitmap.getHeight(), true);
                pBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
                workimageview.setImageBitmap(pBitmap);

                // On était en mise à jour et on bien fait une modif sur notre image, donc on renverra bien un Byte array
                if (pMode.equals(Constants.PARAM_MAJ)) {
                    pReturnByteArray = true;
                }
            }
        });

        // Création d'un listener pour sauver et quitter
        workimagebuttonsave.setOnClickListener(v -> {
            if (pBitmap != null) {
                // En mode MAJ on va aussi renvoyer la position dans l'ArrayList de notre photo
                // Pour que le programme appelant puisse faire sa mise à jour
                switch (pMode) {
                    case Constants.PARAM_MAJ:
                        bundleRetour.putInt(BUNDLE_KEY_ID, pIdPhoto);
                        break;
                    case Constants.PARAM_CRE:
                        pReturnByteArray = true; // On est en création, alors forcément on renverra un byte array
                        break;
                }

                bundleRetour.putBoolean(BUNDLE_OUT_MAJ, pReturnByteArray);

                if (pReturnByteArray) {
                    // Envoi du bitmap par un byteArray
                    pStream.reset();
                    pBitmap.compress(Bitmap.CompressFormat.PNG, 100, pStream);
                    pByteArray = pStream.toByteArray();
                    bundleRetour.putByteArray(BUNDLE_OUT_IMAGE, pByteArray);
                }

                intentRetour.putExtras(bundleRetour);
                setResult(Activity.RESULT_OK, intentRetour);
                finish();
            }
        });

        // Création d'un listener pour annuler ou supprimer l'image
        workimagebuttondelete.setOnClickListener(v -> {
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
            switch (pMode) {

                // On est en mode Création
                case Constants.PARAM_CRE:
                    pStream.reset();
                    pBitmap.compress(Bitmap.CompressFormat.PNG, 100, pStream);
                    pByteArray = pStream.toByteArray();
                    outState.putByteArray(BUNDLE_IN_IMAGE, pByteArray);
                    break;

                // On est en mode Mise à jour
                case Constants.PARAM_MAJ:
                    // En mise à jour on a besoin du bitmap et de son id (position dans l'arrayList)
                    outState.putInt(BUNDLE_KEY_ID, pIdPhoto);

                    pStream.reset();
                    pBitmap.compress(Bitmap.CompressFormat.PNG, 100, pStream);
                    pByteArray = pStream.toByteArray();
                    outState.putByteArray(BUNDLE_IN_IMAGE, pByteArray);
                    break;
            }
        }
    }
}
