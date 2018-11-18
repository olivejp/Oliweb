package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import oliweb.nc.oliweb.App;
import oliweb.nc.oliweb.utility.MediaUtility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static oliweb.nc.oliweb.utility.Constants.REMOTE_NUMBER_PICTURES;

/**
 * Created by orlanth23 on 12/11/2018.
 */

public class ShootingActivityViewModel extends AndroidViewModel {

    private static final String TAG = ShootingActivityViewModel.class.getName();

    @Inject
    MediaUtility mediaUtility;

    private boolean externalStorage;
    private boolean flashIsOn;
    private boolean switchIsOn;
    private int photoNumber;

    private List<Pair<Uri, File>> listPairFileUri = new ArrayList<>();
    private MutableLiveData<List<Pair<Uri, File>>> liveListPairFileUri = new MutableLiveData<>();
    private MutableLiveData<AtomicBoolean> liveFlashIsOn = new MutableLiveData<>();
    private Long nbMaxPictures;

    public ShootingActivityViewModel(@NonNull Application application) {
        super(application);
        ((App) application).getDatabaseRepositoriesComponent().inject(this);
        externalStorage = SharedPreferencesHelper.getInstance(application).getUseExternalStorage();
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        nbMaxPictures = remoteConfig.getLong(REMOTE_NUMBER_PICTURES);
    }

    public void setPhotoNumber(int photoNumnber) {
        this.photoNumber = photoNumnber;
    }

    public Long getNbMaxPicures() {
        return nbMaxPictures;
    }

    public boolean isAbleToAddNewPicture() {
        return listPairFileUri.size() < nbMaxPictures - photoNumber;
    }

    public LiveData<AtomicBoolean> getLiveFlashIsOn() {
        return liveFlashIsOn;
    }

    public boolean isFlashIsOn() {
        return flashIsOn;
    }

    public void setFlashIsOn(boolean flashIsOn) {
        this.flashIsOn = flashIsOn;
        liveFlashIsOn.postValue(new AtomicBoolean(this.flashIsOn));
    }

    public boolean isSwitchIsOn() {
        return switchIsOn;
    }

    public void setSwitchIsOn(boolean switchIsOn) {
        this.switchIsOn = switchIsOn;
    }

    public MediaUtility getMediaUtility() {
        return mediaUtility;
    }

    public void addPhotoToCurrentList(Pair<Uri, File> pair) {
        listPairFileUri.add(pair);
        liveListPairFileUri.postValue(listPairFileUri);
    }

    public void removePhotoFromCurrentList(Uri uri) {
        ArrayList<Pair<Uri, File>> newList = new ArrayList<>();
        for (Pair<Uri, File> pair : listPairFileUri) {
            if (pair.first != uri) {
                newList.add(pair);
            }
        }
        listPairFileUri = newList;
        liveListPairFileUri.postValue(listPairFileUri);
    }

    public List<Uri> getListPairs() {
        ArrayList<Uri> list = new ArrayList<>();
        for (Pair<Uri, File> pair : listPairFileUri) {
            list.add(pair.first);
        }
        return list;
    }

    public Pair<Uri, File> generateNewPairUriFile() {
        Pair<Uri, File> pair = mediaUtility.createNewMediaFileUri(getApplication().getApplicationContext(), externalStorage, MediaUtility.MediaType.IMAGE);
        if (pair != null && pair.first != null) {
            return pair;
        } else {
            Log.e(TAG, "generateNewPairUriFile() : MediaUtility a renvoyé une pair null");
            return null;
        }
    }

    public LiveData<List<Pair<Uri, File>>> getLiveListPairFileUri() {
        return liveListPairFileUri;
    }
}
