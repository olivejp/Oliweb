package oliweb.nc.oliweb.service;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.service.firebase.FirebasePhotoStorage;
import oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel;
import oliweb.nc.oliweb.utility.LiveDataOnce;

import static oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel.AddRemoveFromFavorite.ADD_FAILED;
import static oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel.AddRemoveFromFavorite.ADD_SUCCESSFUL;
import static oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel.AddRemoveFromFavorite.ONE_OF_YOURS;
import static oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel.AddRemoveFromFavorite.REMOVE_FAILED;
import static oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel.AddRemoveFromFavorite.REMOVE_SUCCESSFUL;

/**
 * Created by orlanth23 on 18/08/2018.
 */
@Singleton
public class AnnonceService {

    private static final String TAG = AnnonceService.class.getName();

    private Context context;
    private AnnonceRepository annonceRepository;
    private PhotoService photoService;
    private FirebasePhotoStorage firebasePhotoStorage;
    private AnnonceWithPhotosRepository annonceWithPhotosRepository;

    @Inject
    public AnnonceService(Context context,
                          AnnonceRepository annonceRepository,
                          AnnonceWithPhotosRepository annonceWithPhotosRepository,
                          FirebasePhotoStorage firebasePhotoStorage,
                          PhotoService photoService) {
        this.context = context;
        this.annonceRepository = annonceRepository;
        this.firebasePhotoStorage = firebasePhotoStorage;
        this.annonceWithPhotosRepository = annonceWithPhotosRepository;
        this.photoService = photoService;
    }


    /**
     * renverra onSuccess avec l'AnnonceEntity qu'elle viendra de créer ou celle déjà existante.
     * renverra onError dans le cas d'une erreur.
     *
     * @param uidUser       qui veut rajouter cette annonce dans ses favoris
     * @param annoncePhotos qui sera sauvé avec ses photos
     * @return
     */
    private Single<AnnonceEntity> saveToFavorite(String uidUser, AnnoncePhotos annoncePhotos) {
        return Single.create(emitter ->
                annonceRepository.getAnnonceFavoriteByUidUserAndUidAnnonce(uidUser, annoncePhotos.getAnnonceEntity().getUid())
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(emitter::onSuccess)
                        .doOnComplete(() -> {
                            AnnonceEntity annonceEntity = annoncePhotos.getAnnonceEntity();
                            annonceEntity.setFavorite(1);
                            annonceEntity.setUidUserFavorite(uidUser);
                            annonceRepository.singleSave(annonceEntity)
                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                    .doOnError(emitter::onError)
                                    .doOnSuccess(annonceEntity1 -> {
                                        firebasePhotoStorage.savePhotosFromRemoteToLocal(context, annonceEntity1.getId(), annoncePhotos.getPhotos());
                                        emitter.onSuccess(annonceEntity1);
                                    })
                                    .subscribe();
                        })
                        .subscribe()
        );
    }

    private Single<AtomicBoolean> removeFromFavorite(String uidUser, AnnoncePhotos annoncePhotos) {
        Log.d(TAG, "Starting removeFromFavorite called with annoncePhotos = " + annoncePhotos.toString());
        return Single.create(emitter ->
                annonceWithPhotosRepository.findFavoriteAnnonceByUidAnnonce(uidUser, annoncePhotos.getAnnonceEntity().getUid())
                        .doOnError(emitter::onError)
                        .doOnSuccess(annoncePhotos1 -> {
                            photoService.deleteListPhoto(annoncePhotos.getPhotos());
                            annonceRepository.removeFromFavorite(uidUser, annoncePhotos1);
                            emitter.onSuccess(new AtomicBoolean(true));
                        })
                        .subscribe()
        );
    }

    public LiveDataOnce<SearchActivityViewModel.AddRemoveFromFavorite> addOrRemoveFromFavorite(String uidUser, AnnoncePhotos annoncePhotos) {
        return observer -> {
            if (annoncePhotos.getAnnonceEntity().getUidUser().equals(uidUser)) {
                observer.onChanged(ONE_OF_YOURS);
            } else {
                if (annoncePhotos.getAnnonceEntity().getFavorite() == 1) {
                    removeFromFavorite(uidUser, annoncePhotos)
                            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                            .doOnError(e -> observer.onChanged(REMOVE_FAILED))
                            .doOnSuccess(atomicBoolean -> observer.onChanged(atomicBoolean.get() ? REMOVE_SUCCESSFUL : REMOVE_FAILED))
                            .subscribe();
                } else {
                    saveToFavorite(uidUser, annoncePhotos)
                            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                            .doOnError(e -> observer.onChanged(ADD_FAILED))
                            .doOnSuccess(annonceEntity -> observer.onChanged(ADD_SUCCESSFUL))
                            .subscribe();
                }
            }
        };
    }
}
