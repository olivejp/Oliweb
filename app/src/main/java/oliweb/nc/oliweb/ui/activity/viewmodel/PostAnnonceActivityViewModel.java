package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.CategorieRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;

/**
 * Created by orlanth23 on 31/01/2018.
 */

public class PostAnnonceActivityViewModel extends AndroidViewModel {

    private static final String TAG = PostAnnonceActivityViewModel.class.getName();

    private AnnonceRepository annonceRepository;
    private PhotoRepository photoRepository;
    private UtilisateurRepository utilisateurRepository;
    private CategorieRepository categorieRepository;

    private AnnonceEntity currentAnnonce;
    private CategorieEntity currentCategorie;
    private PhotoEntity currentPhoto;
    private List<PhotoEntity> currentListPhoto = new ArrayList<>();

    private MutableLiveData<List<PhotoEntity>> liveListPhoto = new MutableLiveData<>();

    public PostAnnonceActivityViewModel(@NonNull Application application) {
        super(application);
        categorieRepository = CategorieRepository.getInstance(application);
        photoRepository = PhotoRepository.getInstance(application);
        annonceRepository = AnnonceRepository.getInstance(application);
        utilisateurRepository = UtilisateurRepository.getInstance(application);
    }

    public LiveData<UtilisateurEntity> getConnectedUser(String uid) {
        return this.utilisateurRepository.findByUid(uid);
    }

    public LiveData<List<PhotoEntity>> getLiveListPhoto() {
        if (this.liveListPhoto == null) {
            this.liveListPhoto = new MutableLiveData<>();
            this.liveListPhoto.setValue(currentListPhoto);
        }
        return this.liveListPhoto;
    }

    public LiveData<List<PhotoEntity>> getListPhotoByIdAnnonce(long idAnnonce) {
        return this.photoRepository.findAllByIdAnnonce(idAnnonce);
    }

    public Single<List<CategorieEntity>> getListCategorie() {
        return categorieRepository.getListCategorie();
    }

    public LiveData<AnnonceEntity> getAnnonceById(long idAnnonce) {
        return this.annonceRepository.findLiveById(idAnnonce);
    }

    public LiveData<AnnonceEntity> getAnnonceByUid(String uidAnnonce) {
        return this.annonceRepository.findByUid(uidAnnonce);
    }

    public void createNewAnnonce() {
        this.currentAnnonce = new AnnonceEntity();
        this.currentAnnonce.setUid(null);
        this.currentAnnonce.setStatut(StatusRemote.TO_SEND);
        this.currentAnnonce.setFavorite(0);
        if (this.liveListPhoto == null) {
            this.liveListPhoto = new MutableLiveData<>();
        }
        this.liveListPhoto.postValue(this.currentListPhoto);
    }

    public void setCurrentAnnonce(AnnonceEntity currentAnnonce) {
        this.currentAnnonce = currentAnnonce;
    }

    public AnnonceEntity getCurrentAnnonce() {
        return this.currentAnnonce;
    }

    public Single<AnnonceEntity> saveAnnonce(String titre, String description, int prix, String uidUser, boolean email, boolean message, boolean telephone) {
        Log.d(TAG, "Starting saveAnnonce");
        if (this.currentAnnonce == null) {
            createNewAnnonce();
        }
        return Single.create(emitter -> {
            currentAnnonce.setTitre(titre);
            currentAnnonce.setDescription(description);
            currentAnnonce.setPrix(prix);
            currentAnnonce.setIdCategorie(currentCategorie.getIdCategorie());
            currentAnnonce.setStatut(StatusRemote.TO_SEND);
            currentAnnonce.setContactByEmail(email ? "O" : "N");
            currentAnnonce.setContactByTel(telephone ? "O" : "N");
            currentAnnonce.setContactByMsg(message ? "O" : "N");
            currentAnnonce.setUidUser(uidUser);

            annonceRepository.singleSave(currentAnnonce)
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnSuccess(emitter::onSuccess)
                    .doOnError(emitter::onError)
                    .subscribe();
        });
    }

    public Single<List<PhotoEntity>> savePhotos(AnnonceEntity annonce) {
        Log.d(TAG, "Starting savePhotos currentAnnonce : " + annonce);
        for (PhotoEntity photo : currentListPhoto) {
            photo.setIdAnnonce(annonce.getId());
        }
        return Single.create(emitter -> {
                    ArrayList<PhotoEntity> list = new ArrayList<>();
                    Observable.fromIterable(currentListPhoto)
                            .doOnError(emitter::onError)
                            .doOnNext(photoEntity ->
                                    this.photoRepository.singleSave(photoEntity)
                                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                            .doOnError(emitter::onError)
                                            .doOnSuccess(list::add)
                                            .subscribe()
                            )
                            .doOnComplete(() -> emitter.onSuccess(list))
                            .subscribe();
                }
        );
    }

    public void updatePhotos() {
        this.liveListPhoto.postValue(this.currentListPhoto);
    }

    public void addPhotoToCurrentList(String path) {
        PhotoEntity photoEntity = new PhotoEntity();
        photoEntity.setUriLocal(path);
        photoEntity.setStatut(StatusRemote.NOT_TO_SEND);
        this.currentListPhoto.add(photoEntity);
        this.liveListPhoto.postValue(this.currentListPhoto);
    }

    public boolean canHandleAnotherPhoto() {
        return this.currentListPhoto.size() < 4;
    }

    public boolean removePhotoToCurrentList(PhotoEntity photoEntity) {
        boolean retour = false;
        if (this.currentListPhoto.contains(photoEntity)) {

            if (photoEntity.getStatut().equals(StatusRemote.SEND)) {
                photoEntity.setStatut(StatusRemote.TO_DELETE);
                this.photoRepository.update(photoEntity);
                retour = true;
            } else {
                retour = this.currentListPhoto.remove(photoEntity);
            }

            this.liveListPhoto.postValue(this.currentListPhoto);
        }
        return retour;
    }

    public List<PhotoEntity> getCurrentListPhoto() {
        return this.currentListPhoto;
    }

    public void setCurrentListPhoto(List<PhotoEntity> list) {
        this.currentListPhoto = list;
    }

    public void setUpdatedPhoto(PhotoEntity photo) {
        this.currentPhoto = photo;
    }

    public PhotoEntity getUpdatedPhoto() {
        return this.currentPhoto;
    }

    public void setCurrentCategorie(CategorieEntity categorie) {
        this.currentCategorie = categorie;
    }
}
