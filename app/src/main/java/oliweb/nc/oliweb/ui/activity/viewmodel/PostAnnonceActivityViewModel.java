package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.DateConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.CategorieRepository;
import oliweb.nc.oliweb.database.repository.PhotoRepository;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;

/**
 * Created by orlanth23 on 31/01/2018.
 */

public class PostAnnonceActivityViewModel extends AndroidViewModel {

    private CategorieRepository categorieRepository;
    private AnnonceRepository annonceRepository;
    private PhotoRepository photoRepository;

    private AnnonceEntity annonce;
    private CategorieEntity categorie;
    private List<PhotoEntity> listPhoto = new ArrayList<>();
    private PhotoEntity photoEntityUpdated;
    private MutableLiveData<List<CategorieEntity>> liveDataListCategorie = new MutableLiveData<>();
    private MutableLiveData<List<PhotoEntity>> liveListPhoto = new MutableLiveData<>();

    public PostAnnonceActivityViewModel(@NonNull Application application) {
        super(application);
        categorieRepository = CategorieRepository.getInstance(application);
        photoRepository = PhotoRepository.getInstance(application);
        annonceRepository = AnnonceRepository.getInstance(application);

        // Récupération de toutes la liste des catégories
        liveDataListCategorie = new MutableLiveData<>();
        this.categorieRepository.getListCategorie()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(categorieEntities ->
                        liveDataListCategorie.setValue(categorieEntities)
                );
    }

    public LiveData<List<PhotoEntity>> getLiveListPhoto() {
        if (this.liveListPhoto == null) {
            this.liveListPhoto = new MutableLiveData<>();
            this.liveListPhoto.setValue(listPhoto);
        }
        return this.liveListPhoto;
    }

    public void updatePhotos() {
        this.liveListPhoto.postValue(this.listPhoto);
    }

    public LiveData<List<CategorieEntity>> getLiveDataListCategorie() {
        return liveDataListCategorie;
    }

    public void addPhotoToCurrentList(String path) {
        PhotoEntity photoEntity = new PhotoEntity();
        photoEntity.setUriLocal(path);
        photoEntity.setStatut(StatusRemote.TO_SEND);
        this.listPhoto.add(photoEntity);
        this.liveListPhoto.postValue(this.listPhoto);
    }

    public boolean canHandleAnotherPhoto() {
        return this.listPhoto.size() < 4;
    }

    public boolean removePhotoToCurrentList(PhotoEntity photoEntity) {
        boolean retour = false;
        if (this.listPhoto.contains(photoEntity)) {

            if (photoEntity.getStatut().equals(StatusRemote.SEND)) {
                photoEntity.setStatut(StatusRemote.TO_DELETE);
                this.photoRepository.update(null, photoEntity);
                retour = true;
            } else {
                retour = this.listPhoto.remove(photoEntity);
            }

            this.liveListPhoto.postValue(this.listPhoto);
        }
        return retour;
    }

    public void saveAnnonce(String titre, String description, int prix, String uidUser, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {

        this.annonce.setTitre(titre);
        this.annonce.setDescription(description);
        this.annonce.setPrix(prix);
        this.annonce.setDatePublication(DateConverter.getNowEntity());
        this.annonce.setIdCategorie(categorie.getIdCategorie());
        this.annonce.setStatut(StatusRemote.TO_SEND);
        this.annonce.setUuidUtilisateur(uidUser);

        if (listPhoto == null || listPhoto.isEmpty()) {
            this.annonceRepository.save(annonce, onRespositoryPostExecute);
        } else {
            this.annonceRepository.save(annonce, dataReturn -> {
                if (dataReturn.getNb() > 0) {
                    switch (dataReturn.getTypeTask()) {
                        case INSERT:
                            if (dataReturn.getIds().length > 0) {
                                long idAnnonceInserted = dataReturn.getIds()[0];
                                updatePhotosWithIdAnnonce(this.listPhoto, idAnnonceInserted, onRespositoryPostExecute);
                            }
                            break;
                        case UPDATE:
                            updatePhotosWithIdAnnonce(this.listPhoto, annonce.getIdAnnonce(), onRespositoryPostExecute);
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }

    private void updatePhotosWithIdAnnonce(List<PhotoEntity> listPhoto, long idAnnonce, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        for (PhotoEntity photo : listPhoto) {
            photo.setIdAnnonce(idAnnonce);
        }
        this.photoRepository.save(listPhoto, onRespositoryPostExecute);
    }

    public void createNewAnnonce() {
        this.annonce = new AnnonceEntity();
        this.annonce.setUUID(UUID.randomUUID().toString());
        this.annonce.setStatut(StatusRemote.TO_SEND);
        if (this.liveListPhoto == null) {
            this.liveListPhoto = new MutableLiveData<>();
        }
        this.liveListPhoto.setValue(this.listPhoto);
    }

    public void setAnnonce(AnnonceEntity annonce) {
        this.annonce = annonce;
    }

    public void setListPhoto(List<PhotoEntity> list) {
        this.listPhoto = list;
    }


    public void setUpdatedPhoto(PhotoEntity photo) {
        this.photoEntityUpdated = photo;
    }

    public PhotoEntity getUpdatedPhoto() {
        return this.photoEntityUpdated;
    }

    public LiveData<AnnonceEntity> findAnnonceById(long idAnnonce) {
        return this.annonceRepository.findById(idAnnonce);
    }

    public void setCurrentCategorie(CategorieEntity categorie) {
        this.categorie = categorie;
    }

    public LiveData<List<PhotoEntity>> getListPhotoByIdAnnonce(long idAnnonce) {
        return this.photoRepository.findAllByIdAnnonce(idAnnonce);
    }
}
