package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import oliweb.nc.oliweb.App;
import oliweb.nc.oliweb.utility.MediaUtility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

/**
 * Created by orlanth23 on 12/11/2018.
 */

public class ShootingActivityViewModel extends AndroidViewModel {

    private static final String TAG = ShootingActivityViewModel.class.getName();

    @Inject
    MediaUtility mediaUtility;

    private boolean externalStorage;

    private List<Pair<Uri, File>> listPairFileUri = new ArrayList<>();
    private MutableLiveData<List<Pair<Uri, File>>> liveListPairFileUri = new MutableLiveData<>();

    public ShootingActivityViewModel(@NonNull Application application) {
        super(application);
        ((App) application).getDatabaseRepositoriesComponent().inject(this);
        externalStorage = SharedPreferencesHelper.getInstance(application).getUseExternalStorage();
    }

    public MediaUtility getMediaUtility() {
        return mediaUtility;
    }

    public void addPhotoToCurrentList(Pair<Uri, File> pair) {
        listPairFileUri.add(pair);
        liveListPairFileUri.postValue(listPairFileUri);
    }

    public void removePhotoFromCurrentList(String path) {
        if (listPairFileUri.contains(path) && listPairFileUri.remove(path)) {
            liveListPairFileUri.postValue(listPairFileUri);
        }
    }

    public ArrayList<Uri> getListPairs() {
        ArrayList<Uri> list = new ArrayList<>();
        for (Pair<Uri, File> pair : listPairFileUri) {
            list.add(pair.first);
        }
        return list;
    }

    public Pair<Uri, File> generateNewPair_UriFile() {
        Pair<Uri, File> pair = mediaUtility.createNewMediaFileUri(getApplication().getApplicationContext(), externalStorage, MediaUtility.MediaType.IMAGE);
        if (pair != null && pair.first != null) {
            return pair;
        } else {
            Log.e(TAG, "generateNewPair_UriFile() : MediaUtility a renvoyé une pair null");
            return null;
        }
    }

    public LiveData<List<Pair<Uri, File>>> getLiveListPairFileUri() {
        return liveListPairFileUri;
    }
}
