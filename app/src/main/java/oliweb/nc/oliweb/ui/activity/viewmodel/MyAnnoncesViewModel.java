package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MyAnnoncesViewModel extends AndroidViewModel {

    private static final String TAG = MyAnnoncesViewModel.class.getName();

    private AnnonceWithPhotosRepository annonceWithPhotosRepository;
    private AnnonceRepository annonceRepository;
    private PhotoRepository photoRepository;
    private MutableLiveData<AtomicBoolean> shouldAskQuestion;
    private FirebaseAnnonceRepository firebaseAnnonceRepository;

    public MyAnnoncesViewModel(@NonNull Application application) {
        super(application);
        annonceWithPhotosRepository = AnnonceWithPhotosRepository.getInstance(application.getApplicationContext());
        annonceRepository = AnnonceRepository.getInstance(application.getApplicationContext());
        photoRepository = PhotoRepository.getInstance(application.getApplicationContext());
        firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(application.getApplicationContext());
    }

    public LiveData<List<AnnoncePhotos>> findActiveAnnonceByUidUtilisateur(String uuidUtilisateur) {
        return annonceWithPhotosRepository.findActiveAnnonceByUidUser(uuidUtilisateur);
    }

    // TODO : Peut faire mieux
    public LiveData<AtomicBoolean> shouldIAskQuestionToRetreiveData(@Nullable String uidUtilisateur) {
        if (shouldAskQuestion == null) {
            shouldAskQuestion = new MutableLiveData<>();
        }
        shouldAskQuestion.setValue(new AtomicBoolean(false));

        if (uidUtilisateur != null) {
            firebaseAnnonceRepository.checkFirebaseRepository(uidUtilisateur, shouldAskQuestion);
        }

        return shouldAskQuestion;
    }

    /**
     * Update annonce and photo status to TO_DELETE
     * CoreSync will do the trick.
     *
     * @param idAnnonce
     * @param onRespositoryPostExecute
     */
    public void deleteAnnonceById(long idAnnonce, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        this.annonceRepository.findSingleById(idAnnonce)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(annonceEntity -> updateAnnonceToDelete(annonceEntity, onRespositoryPostExecute))
                .subscribe();
    }

    private void updateAnnonceToDelete(AnnonceEntity annonceEntity, AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        // Update annonce status
        annonceEntity.setStatut(StatusRemote.TO_DELETE);
        annonceRepository.update(onRespositoryPostExecute, annonceEntity);

        // Update photo status
        photoRepository.findAllPhotosByIdAnnonce(annonceEntity.getIdAnnonce())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(this::updateAllPhotosToDelete)
                .subscribe();
    }

    private void updateAllPhotosToDelete(List<PhotoEntity> photoEntities) {
        for (PhotoEntity photoEntity : photoEntities) {
            photoEntity.setStatut(StatusRemote.TO_DELETE);
            photoRepository.update(dataReturn -> {
                if (dataReturn.getNb() != 0) {
                    Log.d(TAG, "PhotoEntity successfully updated TO_DELETE");
                }
            }, photoEntity);
        }
    }
}
