package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.app.NotificationChannelGroup;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
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
    private List<PhotoEntity> listPhoto;

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
        }
        return this.liveListPhoto;
    }

    public String getUidUtilisateur() {
        if (FirebaseAuth.getInstance() != null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }

    public LiveData<List<CategorieEntity>> getLiveDataListCategorie() {
        return liveDataListCategorie;
    }

    public void addPhotoToCurrentList(String path) {
        PhotoEntity photoEntity = new PhotoEntity();
        photoEntity.setUUID(UUID.randomUUID().toString());
        photoEntity.setCheminLocal(path);
        photoEntity.setStatut(StatusRemote.TO_SEND);
        this.listPhoto.add(photoEntity);
        this.liveListPhoto.postValue(this.listPhoto);
    }

    public void saveAnnonce(@Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        // Sauvegarde de l'annonce
        this.annonceRepository.save(annonce, ids -> {
            // Sauvegarde des photos
            this.photoRepository.save(listPhoto, null);
        });
    }

    public void createNewAnnonce() {
        this.annonce = new AnnonceEntity();
        this.annonce.setUUID(UUID.randomUUID().toString());
        this.annonce.setStatut(StatusRemote.TO_SEND);
        this.listPhoto = new ArrayList<>();
        this.liveListPhoto.setValue(this.listPhoto);
    }

    public void setAnnonce(AnnonceEntity annonce) {
        this.annonce = annonce;
    }

    public void setListPhoto(List<PhotoEntity> list) {
        this.listPhoto = list;
        this.liveListPhoto.postValue(list);
    }

    public AnnonceEntity getCurrentAnnonce() {
        return annonce;
    }

    public void deletePhoto(long idPhoto) {
        this.photoRepository.singleById(idPhoto)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(photoEntity -> {
                    if (photoEntity != null) this.photoRepository.delete(null, photoEntity);
                });
    }

    public LiveData<AnnonceEntity> findAnnonceById(long idAnnonce) {
        return this.annonceRepository.findById(idAnnonce);
    }

    public void setCurrentCategorie(CategorieEntity categorie) {
        this.categorie = categorie;
    }

    public CategorieEntity getCurrentCategorie() {
        return this.categorie;
    }

    public LiveData<CategorieEntity> findCategorieById(long idCategorie) {
        return this.categorieRepository.findById(idCategorie);
    }

    public LiveData<List<PhotoEntity>> getListPhotoByIdAnnonce(long idAnnonce) {
        return this.photoRepository.findAllByIdAnnonce(idAnnonce);
    }
}
