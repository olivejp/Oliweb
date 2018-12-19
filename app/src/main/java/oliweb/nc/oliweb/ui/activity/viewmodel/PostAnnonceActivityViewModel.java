package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import oliweb.nc.oliweb.App;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.repository.local.CategorieRepository;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.utility.MediaUtility;
import oliweb.nc.oliweb.utility.Utility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

/**
 * Created by orlanth23 on 31/01/2018.
 */

public class PostAnnonceActivityViewModel extends AndroidViewModel {

    private static final String TAG = PostAnnonceActivityViewModel.class.getName();

    private static final int NBR_MAX = 4;

    @Inject
    AnnonceRepository annonceRepository;

    @Inject
    AnnonceWithPhotosRepository annoncePhotoRepo;

    @Inject
    PhotoRepository photoRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    CategorieRepository categorieRepository;

    @Inject
    @Named("processScheduler")
    Scheduler processScheduler;

    @Inject
    @Named("androidScheduler")
    Scheduler androidScheduler;

    @Inject
    MediaUtility mediaUtility;

    private AnnoncePhotos currentAnnonce;
    private CategorieEntity currentCategorie;
    private PhotoEntity currentPhoto;
    private MutableLiveData<ArrayList<CategorieEntity>> listCategorie;
    private MutableLiveData<AtomicBoolean> showLoading = new MutableLiveData<>();
    private MutableLiveData<List<PhotoEntity>> liveListPhoto = new MutableLiveData<>();

    public PostAnnonceActivityViewModel(@NonNull Application application) {
        super(application);
        ((App) application).getDatabaseRepositoriesComponent().inject(this);
    }

    /**
     * Lecture de la liste des Uri passée en paramètre.
     * Pour chaque élément on va retailler l'image et l'ajouter à notre liste courante de photo
     *
     * @param listPhotosUri              Liste des Uri à retailler et à enregistrer
     * @param deleteUriSourceAfterResize Doit on supprimer le fichier situé à l'Uri source après le redimensionnement.
     * @return
     */
    public Maybe<AtomicBoolean> resizeListPhotos(List<Uri> listPhotosUri, boolean deleteUriSourceAfterResize) {
        boolean externalStorage = SharedPreferencesHelper.getInstance(getApplication()).getUseExternalStorage();
        return Maybe.create(emitter -> {
            for (Uri uriPhoto : listPhotosUri) {
                Uri uriDst = generateNewUri(externalStorage);
                if (uriDst != null && getMediaUtility().copyAndResizeUriImages(getApplication(), uriPhoto, uriDst, deleteUriSourceAfterResize)) {
                    addPhotoToCurrentList(uriDst.toString());
                } else {
                    Log.e(TAG, String.format("La photo %s n'a pas pu être redimenssionnée et enregistrée", uriPhoto.toString()));
                    emitter.onError(new RuntimeException(String.format("La photo %s n'a pas pu être redimenssionnée et enregistrée", uriPhoto.toString())));
                }
            }
            emitter.onComplete();
        });
    }

    public List<PhotoEntity> removePhotoWithForbiddenStatus(List<PhotoEntity> listBefore) {
        ArrayList<PhotoEntity> nouvelleListe = new ArrayList<>();
        for (PhotoEntity photo : listBefore) {
            if (!Utility.allStatusToAvoid().contains(photo.getStatut().getValue())) {
                nouvelleListe.add(photo);
            }
        }
        return nouvelleListe;
    }

    public int getPhotoNumber() {
        if (liveListPhoto == null || liveListPhoto.getValue().isEmpty()) {
            return 0;
        }
        return removePhotoWithForbiddenStatus(liveListPhoto.getValue()).size();
    }

    public LiveData<AtomicBoolean> isShowLoading() {
        return showLoading;
    }

    public void setShowLoading(boolean showLoading) {
        this.showLoading.postValue(new AtomicBoolean(showLoading));
    }

    public MediaUtility getMediaUtility() {
        return mediaUtility;
    }

    public LiveData<UserEntity> getConnectedUser(String uid) {
        return this.userRepository.findByUid(uid);
    }

    public LiveData<List<PhotoEntity>> getLiveListPhoto() {
        if (this.liveListPhoto == null) {
            this.liveListPhoto = new MutableLiveData<>();
        }
        return this.liveListPhoto;
    }

    public LiveData<ArrayList<CategorieEntity>> getListCategorie() {
        if (listCategorie == null) {
            listCategorie = new MutableLiveData<>();
        }
        categorieRepository.getListCategorie()
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .doOnSuccess(categorieEntities -> listCategorie.postValue(new ArrayList<>(categorieEntities)))
                .subscribe();
        return listCategorie;
    }

    public LiveData<AnnoncePhotos> getAnnonceById(long idAnnonce) {
        return this.annoncePhotoRepo.findLiveById(idAnnonce);
    }

    public LiveData<AnnoncePhotos> getAnnonceByUid(String uidAnnonce) {
        return this.annoncePhotoRepo.findByUid(uidAnnonce);
    }

    public void createNewAnnonce() {
        this.currentAnnonce = new AnnoncePhotos();
        this.currentAnnonce.annonceEntity.setUid(null);
        this.currentAnnonce.annonceEntity.setStatut(StatusRemote.TO_SEND);
        this.currentAnnonce.annonceEntity.setFavorite(0);
        this.currentAnnonce.photos = new ArrayList<>();
        if (this.liveListPhoto == null) {
            this.liveListPhoto = new MutableLiveData<>();
        }
        this.liveListPhoto.postValue(this.currentAnnonce.getPhotos());
    }

    public void setCurrentAnnonce(AnnoncePhotos currentAnnonce) {
        this.currentAnnonce = currentAnnonce;
    }

    public AnnoncePhotos getCurrentAnnonce() {
        return this.currentAnnonce;
    }

    public Single<AnnonceEntity> saveAnnonce(String titre, String description, int prix, String uidUser, boolean email, boolean message, boolean telephone) {
        Log.d(TAG, "Starting saveAnnonce");
        if (this.currentAnnonce == null) {
            createNewAnnonce();
        }
        return Single.create(emitter -> {
            currentAnnonce.annonceEntity.setTitre(titre);
            currentAnnonce.annonceEntity.setDescription(description);
            currentAnnonce.annonceEntity.setPrix(prix);
            currentAnnonce.annonceEntity.setIdCategorie(currentCategorie.getIdCategorie());
            currentAnnonce.annonceEntity.setStatut(StatusRemote.TO_SEND);
            currentAnnonce.annonceEntity.setContactByEmail(email ? "O" : "N");
            currentAnnonce.annonceEntity.setContactByTel(telephone ? "O" : "N");
            currentAnnonce.annonceEntity.setContactByMsg(message ? "O" : "N");
            currentAnnonce.annonceEntity.setUidUser(uidUser);

            annonceRepository.singleSave(currentAnnonce.annonceEntity)
                    .subscribeOn(processScheduler).observeOn(processScheduler)
                    .doOnSuccess(emitter::onSuccess)
                    .doOnError(emitter::onError)
                    .subscribe();
        });
    }

    public Single<List<PhotoEntity>> savePhotos(Long idAnnonce) {
        Log.d(TAG, "Starting savePhotos id annonce : " + idAnnonce);
        for (PhotoEntity photo : currentAnnonce.getPhotos()) {
            photo.setIdAnnonce(idAnnonce);
            if (photo.getStatut().equals(StatusRemote.NOT_TO_SEND)) {
                photo.setStatut(StatusRemote.TO_SEND);
            }
        }
        return Single.create(emitter -> {
                    ArrayList<PhotoEntity> list = new ArrayList<>();
                    Observable.fromIterable(currentAnnonce.getPhotos())
                            .doOnError(emitter::onError)
                            .doOnNext(photoEntity ->
                                    this.photoRepository.singleSave(photoEntity)
                                            .subscribeOn(processScheduler).observeOn(processScheduler)
                                            .doOnError(emitter::onError)
                                            .doOnSuccess(list::add)
                                            .subscribe()
                            )
                            .doOnComplete(() -> emitter.onSuccess(list))
                            .subscribe();
                }
        );
    }

    /**
     * Return the actual valid photo number in the annonce
     *
     * @return
     */
    public int getActualNbrPhotos() {
        int i = 0;
        for (PhotoEntity photoEntity : this.currentAnnonce.getPhotos()) {
            if (!Utility.allStatusToAvoid().contains(photoEntity.getStatut().getValue())) {
                i++;
            }
        }
        return i;
    }

    public void updatePhotos() {
        this.liveListPhoto.postValue(this.currentAnnonce.getPhotos());
    }

    private void addPhotoToCurrentList(String path) {
        PhotoEntity photoEntity = new PhotoEntity();
        photoEntity.setUriLocal(path);
        photoEntity.setStatut(StatusRemote.NOT_TO_SEND);
        this.currentAnnonce.getPhotos().add(photoEntity);
        updatePhotos();
    }

    public void removePhotoFromCurrentList(PhotoEntity photoEntity) {
        if (this.currentAnnonce.getPhotos().contains(photoEntity)) {
            photoEntity.setStatut(StatusRemote.TO_DELETE);
            this.photoRepository.update(photoEntity);
            this.liveListPhoto.postValue(this.currentAnnonce.getPhotos());
        }
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

    public CategorieEntity getCurrentCategorie() {
        return currentCategorie;
    }

    private Uri generateNewUri(boolean externalStorage) {
        Pair<Uri, File> pair = mediaUtility.createNewMediaFileUri(getApplication().getApplicationContext(), externalStorage, MediaUtility.MediaType.IMAGE);
        if (pair != null && pair.first != null) {
            return pair.first;
        } else {
            Log.e(TAG, "generateNewUri() : MediaUtility a renvoyé une pair null");
            return null;
        }
    }
}
