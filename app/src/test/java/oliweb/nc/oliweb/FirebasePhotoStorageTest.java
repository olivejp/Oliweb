package oliweb.nc.oliweb;

import android.net.Uri;

import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.service.firebase.FirebasePhotoStorage;

import static oliweb.nc.oliweb.service.firebase.FirebasePhotoStorage.ERROR_MISSED_URI;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirebasePhotoStorageTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private StorageReference firestorageReference;

    @Mock
    private StorageReference storageReference;

    @Mock
    private UploadTask uploadTask;

    private void resetMock() {
        Mockito.reset(photoRepository);
    }

    @Test
    public void if_noPhotoUrl_fails() {

        PhotoEntity photoEntity = new PhotoEntity();
        photoEntity.setUriLocal(null);

        // Création de mon service à tester
        FirebasePhotoStorage firebasePhotoStorage = new FirebasePhotoStorage(photoRepository);
        firebasePhotoStorage.setFireStorage(firestorageReference);

        // Appel de ma fonction à tester
        TestObserver<Uri> testSubscriber = new TestObserver<>();
        firebasePhotoStorage.sendPhotoToRemote(photoEntity).subscribe(testSubscriber);

        testSubscriber.assertError(error -> ERROR_MISSED_URI.equals(error.getMessage()));

        resetMock();
    }

    @Test
    public void if_photoUrl_should_pass() {
        PhotoEntity photoEntity = new PhotoEntity();
        photoEntity.setUriLocal("http://bitmap.jpg");

        when(storageReference.putFile(Uri.parse("http://bitmap.jpg"))).thenReturn(uploadTask);
        when(firestorageReference.child("bitmap.jpg")).thenReturn(storageReference);

        // Création de mon service à tester
        FirebasePhotoStorage firebasePhotoStorage = new FirebasePhotoStorage(photoRepository);
        firebasePhotoStorage.setFireStorage(firestorageReference);

        // Appel de ma fonction à tester
        TestObserver<Uri> testSubscriber = new TestObserver<>();
        firebasePhotoStorage.sendPhotoToRemote(photoEntity).subscribe(testSubscriber);

        testSubscriber.assertNoErrors();

        resetMock();
    }
}