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
import oliweb.nc.oliweb.database.entity.AnnonceFull;
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
    private UserService userService;
    private FirebasePhotoStorage firebasePhotoStorage;
    private AnnonceWithPhotosRepository annonceWithPhotosRepository;

    @Inject
    public AnnonceService(Context context,
                          AnnonceRepository annonceRepository,
                          AnnonceWithPhotosRepository annonceWithPhotosRepository,
                          FirebasePhotoStorage firebasePhotoStorage,
                          PhotoService photoService,
                          UserService userService) {
        this.context = context;
        this.annonceRepository = annonceRepository;
        this.userService = userService;
        this.firebasePhotoStorage = firebasePhotoStorage;
        this.annonceWithPhotosRepository = annonceWithPhotosRepository;
        this.photoService = photoService;
    }

    /**
     * @param uidUser
     * @param annonceFull
     * @return Will return ONE_OF_YOURS if annonce already owned by the user
     * Otherwise will return one of these values : ADD_SUCCESSFUL | ADD_FAILED | REMOVE_SUCCESSFUL | REMOVE_FAILED
     */
    public LiveDataOnce<SearchActivityViewModel.AddRemoveFromFavorite> addOrRemoveFromFavorite(String uidUser, AnnonceFull annonceFull) {
        return observer -> {
            if (annonceFull.getAnnonce().getUidUser().equals(uidUser)) {
                observer.onChanged(ONE_OF_YOURS);
            } else {
                if (annonceFull.getAnnonce().getFavorite() == 1) {
                    removeFromFavorite(uidUser, annonceFull)
                            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess(atomicBoolean -> observer.onChanged(atomicBoolean.get() ? REMOVE_SUCCESSFUL : REMOVE_FAILED))
                            .doOnError(e -> {
                                Log.e(TAG, e.getMessage());
                                observer.onChanged(REMOVE_FAILED);
                            })
                            .subscribe();
                } else {
                    saveToFavorite(uidUser, annonceFull)
                            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess(annonceEntity -> observer.onChanged(ADD_SUCCESSFUL))
                            .doOnError(e -> {
                                Log.e(TAG, e.getMessage());
                                observer.onChanged(ADD_FAILED);
                            })
                            .subscribe();
                }
            }
        };
    }

    /**
     * renverra onSuccess avec l'AnnonceEntity qu'elle viendra de créer ou celle déjà existante.
     * renverra onError dans le cas d'une erreur.
     *
     * @param uidUser     qui veut rajouter cette annonce dans ses favoris
     * @param annonceFull qui sera sauvé avec ses photos
     * @return
     */
    private Single<AnnonceEntity> saveToFavorite(String uidUser, AnnonceFull annonceFull) {
        return Single.create(emitter ->
                annonceRepository.getAnnonceFavoriteByUidUserAndUidAnnonce(uidUser, annonceFull.getAnnonce().getUid())
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(emitter::onSuccess)
                        .doOnComplete(() -> {
                            // Sauvegarde de l'utilisateur, créateur de l'annonce
                            if (annonceFull.getUtilisateur() != null && !annonceFull.getUtilisateur().isEmpty()) {
                                userService.saveUserToFavorite(annonceFull.getUtilisateur().get(0));
                            } else {
                                Log.e(TAG, "Tentative d'insertion d'une annonce en favoris sans créateur.");
                            }

                            // Sauvegarde de l'annonce
                            if (annonceFull.getAnnonce() != null) {
                                AnnonceEntity annonceEntity = annonceFull.getAnnonce();
                                annonceEntity.setFavorite(1);
                                annonceEntity.setUidUserFavorite(uidUser);
                                annonceRepository.singleSave(annonceEntity)
                                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                        .flatMap(annonceEntity1 -> firebasePhotoStorage.savePhotosFromRemoteToLocal(context, annonceEntity1.getId(), annonceFull.getPhotos()))
                                        .flatMapMaybe(annonceRepository::findById)
                                        .doOnSuccess(emitter::onSuccess)
                                        .doOnError(emitter::onError)
                                        .subscribe();
                            }

                        })
                        .subscribe()
        );
    }

    private Single<AtomicBoolean> removeFromFavorite(String uidUser, AnnonceFull annonceFull) {
        Log.d(TAG, "Starting removeFromFavorite called with annonceFull = " + annonceFull.toString());
        return Single.create(emitter ->
                annonceWithPhotosRepository.findFavoriteAnnonceByUidAnnonce(uidUser, annonceFull.getAnnonce().getUid())
                        .doOnComplete(() -> emitter.onSuccess(new AtomicBoolean(true)))
                        .flatMapSingle(annoncePhotos -> photoService.deleteListFromDevice(annoncePhotos.getPhotos()))
                        .map(atomicBoolean -> annonceRepository.removeFromFavorite(uidUser, annonceFull.getAnnonce().getUid()))
                        .doOnSuccess(integer -> emitter.onSuccess(new AtomicBoolean(integer >= 1)))
                        .doOnError(emitter::onError)
                        .subscribe()
        );
    }
}
