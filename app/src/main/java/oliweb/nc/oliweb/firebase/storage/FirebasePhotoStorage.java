package oliweb.nc.oliweb.firebase.storage;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.PhotoEntity;

public class FirebasePhotoStorage {

    private static final String TAG = FirebasePhotoStorage.class.getName();

    private static FirebasePhotoStorage instance;

    private static StorageReference fireStorage;

    private FirebasePhotoStorage() {
    }

    public static FirebasePhotoStorage getInstance() {
        if (instance == null) {
            instance = new FirebasePhotoStorage();
            fireStorage = FirebaseStorage.getInstance().getReference();
        }
        return instance;
    }

    /**
     * Read the photo URI, then send the photo to Firebase Storage.
     * The Single<String> will return the Downloadpath of the photo
     *
     * @param photo to store
     * @return the download path of the downloaded photo
     */
    public Single<String> savePhotoToStorage(PhotoEntity photo) {
        Log.d(TAG, "Starting sendPhotoToFirebaseStorage");
        return Single.create(e -> {
            File file = new File(photo.getUriLocal());
            String fileName = file.getName();
            StorageReference storageReference = fireStorage.child(fileName);
            storageReference.putFile(Uri.parse(photo.getUriLocal()))
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "Succeed to send the photo to Fb Storage : " + taskSnapshot.getDownloadUrl());
                        e.onSuccess(taskSnapshot.getDownloadUrl().toString());
                    })
                    .addOnFailureListener(e::onError);
        });
    }
}
