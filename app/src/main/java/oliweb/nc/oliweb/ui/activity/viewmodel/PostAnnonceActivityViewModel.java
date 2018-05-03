package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Maybe;
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
    private AnnonceEntity annonce;
    private CategorieEntity categorie;
    private ArrayList<PhotoEntity> listPhoto = new ArrayList<>();
    private PhotoEntity photoEntityUpdated;
    private MutableLiveData<ArrayList<PhotoEntity>> liveListPhoto = new MutableLiveData<>();

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

    public LiveData<ArrayList<PhotoEntity>> getLiveListPhoto() {
        if (this.liveListPhoto == null) {
            this.liveListPhoto = new MutableLiveData<>();
            this.liveListPhoto.setValue(listPhoto);
        }
        return this.liveListPhoto;
    }

    public LiveData<List<PhotoEntity>> getListPhotoByIdAnnonce(long idAnnonce) {
        return this.photoRepository.findAllByIdAnnonce(idAnnonce);
    }

    public Single<List<CategorieEntity>> getLiveDataListCategorie() {
        return categorieRepository.getListCategorie();
    }

    public LiveData<AnnonceEntity> findAnnonceById(long idAnnonce) {
        return this.annonceRepository.findById(idAnnonce);
    }

    public LiveData<AnnonceEntity> findAnnonceByUid(String uidAnnonce) {
        return this.annonceRepository.findByUid(uidAnnonce);
    }

    public void createNewAnnonce() {
        this.annonce = new AnnonceEntity();
        this.annonce.setUuid(null);
        this.annonce.setStatut(StatusRemote.TO_SEND);
        this.annonce.setFavorite(0);
        if (this.liveListPhoto == null) {
            this.liveListPhoto = new MutableLiveData<>();
        }
        this.liveListPhoto.postValue(this.listPhoto);
    }

    public void setAnnonce(AnnonceEntity annonce) {
        this.annonce = annonce;
    }

    public AnnonceEntity getAnnonce() {
        return this.annonce;
    }

    public Single<AnnonceEntity> saveAnnonce(String titre, String description, int prix, String uidUser, boolean email, boolean message, boolean telephone, long idCategorie) {
        Log.d(TAG, "Starting saveAnnonce");
        if (this.annonce == null) {
            createNewAnnonce();
        }
        return Single.create(emitter -> {
            annonce.setTitre(titre);
            annonce.setDescription(description);
            annonce.setPrix(prix);
            annonce.setIdCategorie(idCategorie);
            annonce.setStatut(StatusRemote.TO_SEND);
            annonce.setContactByEmail(email ? "O" : "N");
            annonce.setContactByTel(telephone ? "O" : "N");
            annonce.setContactByMsg(message ? "O" : "N");
            annonce.setUuidUtilisateur(uidUser);

            annonceRepository.saveWithSingle(annonce)
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnSuccess(t -> {
                        Log.d(TAG, "doOnSuccess saveWithSingle " + t);
                        emitter.onSuccess(t);
                    })
                    .doOnError(exception -> {
                        Log.d(TAG, "doOnError saveWithSingle " + exception.getLocalizedMessage(), exception);
                        emitter.onError(exception);
                    })
                    .subscribe();
        });
    }

    public Maybe<List<PhotoEntity>> savePhotos(AnnonceEntity annonce) {
        Log.d(TAG, "Starting savePhotos annonce : " + annonce);
        for (PhotoEntity photo : listPhoto) {
            photo.setIdAnnonce(annonce.getIdAnnonce());
        }
        return Maybe.create(emitter ->
                this.photoRepository.saveWithSingle(listPhoto)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(e -> {
                            Log.d(TAG, "savePhotos.doOnError e : " + e.getLocalizedMessage(), e);
                            emitter.onError(e);
                        })
                        .doOnSuccess(listPhotos -> {
                            Log.d(TAG, "savePhotos.doOnSuccess listPhotos : " + listPhotos);
                            emitter.onSuccess(listPhotos);
                        })
                        .subscribe()
        );
    }

    public void updatePhotos() {
        this.liveListPhoto.postValue(this.listPhoto);
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
                this.photoRepository.update(photoEntity);
                retour = true;
            } else {
                retour = this.listPhoto.remove(photoEntity);
            }

            this.liveListPhoto.postValue(this.listPhoto);
        }
        return retour;
    }


    public ArrayList<PhotoEntity> getListPhoto() {
        return this.listPhoto;
    }

    public void setListPhoto(ArrayList<PhotoEntity> list) {
        this.listPhoto = list;
    }

    public void setUpdatedPhoto(PhotoEntity photo) {
        this.photoEntityUpdated = photo;
    }

    public PhotoEntity getUpdatedPhoto() {
        return this.photoEntityUpdated;
    }


    public void setCurrentCategorie(CategorieEntity categorie) {
        this.categorie = categorie;
    }

    public CategorieEntity getCategorie() {
        return categorie;
    }
}
