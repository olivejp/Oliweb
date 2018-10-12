package oliweb.nc.oliweb;

import android.content.Context;
import android.net.Uri;

import com.google.firebase.storage.StorageReference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.service.firebase.FirebasePhotoStorage;
import oliweb.nc.oliweb.utility.MediaUtility;
import oliweb.nc.oliweb.utility.MediaUtilityException;

import static oliweb.nc.oliweb.service.firebase.FirebasePhotoStorage.ERROR_MISSED_URI;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirebasePhotoStorageTest {

    public static final String TEST_ERROR = "test_error";
    @Mock
    private Context context;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private StorageReference firestorageReference;

    @Mock
    private MediaUtility mediaUtility;

    private TestScheduler testScheduler;

    private void resetMock() {
        Mockito.reset(photoRepository);
    }

    @Test
    public void if_noPhotoUrl_sendPhotoToRemote_shouldFails() {

        testScheduler = new TestScheduler();

        PhotoEntity photoEntity = new PhotoEntity();
        photoEntity.setUriLocal(null);

        // Création de mon service à tester
        FirebasePhotoStorage firebasePhotoStorage = new FirebasePhotoStorage(photoRepository, testScheduler, mediaUtility);
        firebasePhotoStorage.setFireStorage(firestorageReference);

        // Appel de ma fonction à tester
        TestObserver<Uri> testSubscriber = new TestObserver<>();
        firebasePhotoStorage.sendPhotoToRemote(photoEntity).subscribe(testSubscriber);

        testSubscriber.assertError(error -> ERROR_MISSED_URI.equals(error.getMessage()));

        resetMock();
    }

    @Test
    public void if_mediaUtilitySendException_savePhotosFromRemoteToLocal_shouldFails() {

        testScheduler = new TestScheduler();

        when(mediaUtility.createNewImagePairUriFile(any())).thenThrow(new MediaUtilityException(TEST_ERROR));

        PhotoEntity photoEntity = new PhotoEntity();
        photoEntity.setUriLocal(null);
        photoEntity.setFirebasePath("firebasePath");

        // Création de mon service à tester
        FirebasePhotoStorage firebasePhotoStorage = new FirebasePhotoStorage(photoRepository, testScheduler, mediaUtility);
        firebasePhotoStorage.setFireStorage(firestorageReference);

        // Appel de ma fonction à tester
        TestObserver<Long> testSubscriber = new TestObserver<>();
        firebasePhotoStorage.savePhotosFromRemoteToLocal(context, 1L, Collections.singletonList(photoEntity))
                .subscribeOn(testScheduler).observeOn(testScheduler)
                .subscribe(testSubscriber);

        testScheduler.triggerActions();

        testSubscriber.assertError(error -> TEST_ERROR.equals(error.getMessage()));

        resetMock();
    }
}