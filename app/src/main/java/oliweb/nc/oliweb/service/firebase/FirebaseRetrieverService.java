package oliweb.nc.oliweb.service.firebase;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import androidx.annotation.NonNull;
import io.reactivex.Emitter;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.CategorieRepository;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.LiveDataOnce;

/**
 * Created by orlanth23 on 03/03/2018.
 * This class allows to retrieve {@link AnnonceFirebase} from Firebase corresponding to the given UID User.
 */
@Singleton
public class FirebaseRetrieverService {

    private static final String TAG = FirebaseRetrieverService.class.getCanonicalName();
    private FirebaseAnnonceRepository firebaseAnnonceRepository;
    private AnnonceRepository annonceRepository;
    private CategorieRepository categorieRepository;
    private FirebasePhotoStorage firebasePhotoStorage;
    private Scheduler scheduler;
    private Scheduler androidScheduler;

    @Inject
    public FirebaseRetrieverService(FirebaseAnnonceRepository firebaseAnnonceRepository,
                                    AnnonceRepository annonceRepository,
                                    CategorieRepository categorieRepository,
                                    FirebasePhotoStorage firebasePhotoStorage,
                                    @Named("processScheduler") Scheduler scheduler,
                                    @Named("androidScheduler") Scheduler androidScheduler) {
        this.firebaseAnnonceRepository = firebaseAnnonceRepository;
        this.annonceRepository = annonceRepository;
        this.categorieRepository = categorieRepository;
        this.firebasePhotoStorage = firebasePhotoStorage;
        this.scheduler = scheduler;
        this.androidScheduler = androidScheduler;
    }

    /**
     * Va récupérer la liste de toutes les catégories dans Firebase
     * et va comparer si on a les mêmes noms de catégories dans la DB locale.
     */
    public void checkRemoteCategoriesEqualToLocalCategories() {
        Log.d(TAG, "Starting checkRemoteCategoriesEqualToLocalCategories");

        // Création de listener pour toutes les catégories
        Query query = FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_CATEGORIE_REF);
        query.addListenerForSingleValueEvent(categorieValueListener);
    }

    private ValueEventListener categorieValueListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            try {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    findAndReplaceCategory(data);
                }
                Log.d(TAG, "Finish to checkRemoteCategoriesEqualToLocalCategories");
            } catch (Exception e1) {
                Log.e(TAG, e1.getLocalizedMessage(), e1);
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            // Do nothing
        }

        /**
         * On va rechercher la catégorie avec l'ID récupéré.
         * Si elle n'existe pas en base, on la créée.
         * Si elle existe en base, on va vérifier que le libellé est le même
         * --> Si le libellé est différent, on met à jour
         * --> Si le libellé est identique, on ne fait rien
         *
         * @param data DataSnapshot dans lequel on trouve le libellé de la catégorie
         */
        private void findAndReplaceCategory(DataSnapshot data) {
            try {
                String categorieLibelle = data.getValue(String.class);
                Long idCategory = Long.valueOf(data.getKey());
                if (categorieLibelle != null && !categorieLibelle.isEmpty()) {
                    // Lecture en base pour voir si la catégorie existe déjà avec cet identifiant
                    categorieRepository.findById(idCategory)
                            .subscribeOn(scheduler).observeOn(scheduler)
                            .doOnComplete(() -> createNewCategory(idCategory, categorieLibelle))
                            .doOnSuccess(categorieEntity1 -> checkCategoryLibelle(categorieEntity1, categorieLibelle))
                            .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                            .subscribe();
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        }

        /**
         * Création d'une nouvelle CategoryEntity
         *
         * @param idCategory Long représentant l'ID de la nouvelle CategoryEntity
         * @param categorieLibelle String représentant le libellé de la nouvelle CategoryEntity
         */
        private void createNewCategory(Long idCategory, String categorieLibelle) {
            // La catégorie n'existe pas en local, on va la créer.
            CategorieEntity categorieEntity = new CategorieEntity();
            categorieEntity.setIdCategorie(idCategory);
            categorieEntity.setName(categorieLibelle);
            categorieRepository.singleInsert(categorieEntity)
                    .subscribeOn(scheduler).observeOn(scheduler)
                    .doOnSuccess(categorieEntity1 -> Log.d(TAG, "Category correctly inserted ==> " + categorieLibelle))
                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                    .subscribe();
        }

        private void checkCategoryLibelle(CategorieEntity oldCategory, String newCategorieLibelle) {
            if (!newCategorieLibelle.equals(oldCategory.getName())) {
                oldCategory.setName(newCategorieLibelle);
                categorieRepository.singleSave(oldCategory)
                        .subscribeOn(scheduler).observeOn(scheduler)
                        .doOnSuccess(categorieEntity1 -> Log.d(TAG, "Category's libelle has been correctly updated ==> " + newCategorieLibelle))
                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                        .subscribe();
            } else {
                Log.d(TAG, "Category already exists with same libelle in local DB : " + oldCategory.getName());
            }
        }
    };

    /**
     * Retrieve all the annonces on the Fb database for the specified User uid.
     * Then we try to find them in the local DB.
     * If not present the MutableLiveData shouldAskQuestion will return True.
     *
     * @param uidUser
     */
    public LiveDataOnce<AtomicBoolean> checkFirebaseRepository(final String uidUser) {
        return observer -> firebaseAnnonceRepository.observeAllAnnonceByUidUser(uidUser)
                .subscribeOn(androidScheduler).observeOn(scheduler)
                .switchMapSingle(annonceDto -> annonceRepository.countByUidUserAndUidAnnonce(uidUser, annonceDto.getUuid()))
                .any(integer -> integer != null && integer == 0)
                .doOnSuccess(aBoolean -> observer.onChanged(new AtomicBoolean(aBoolean)))
                .doOnError(throwable -> Log.e(TAG, throwable.getMessage()))
                .subscribe();
    }

    /**
     * Doit renvoyer les annonces firebase qui ne sont pas présentes dans la base locale
     *
     * @return
     */
    private Single<List<AnnonceFirebase>> getAnnonceToRetrieve(String uidUser) {
        return Single.create(emitter -> {
            List<AnnonceFirebase> listToReturn = new ArrayList<>();
            firebaseAnnonceRepository.observeAllAnnonceByUidUser(uidUser)
                    .subscribeOn(scheduler).observeOn(scheduler)
                    .toList()
                    .flattenAsObservable(annonceFirebases -> {
                        listToReturn.addAll(annonceFirebases);
                        return annonceFirebases;
                    })
                    .flatMapMaybe(annonce -> annonceRepository.getMaybeByUidUserAndUidAnnonce(uidUser, annonce.getUuid()))
                    .doOnNext(annonceEntity -> {
                        for (Iterator<AnnonceFirebase> iter = listToReturn.listIterator(); iter.hasNext(); ) {
                            AnnonceFirebase a = iter.next();
                            if (a.getUuid().equals(annonceEntity.getUid())) {
                                iter.remove();
                                break;
                            }
                        }
                    })
                    .doOnComplete(() -> emitter.onSuccess(listToReturn))
                    .subscribe();
        });
    }

    /**
     * Lecture de toutes les annonces présentes dans Firebase pour cet utilisateur
     * et récupération de ces annonces dans la base locale
     * puis récupération des photos de chaque annonce.
     */
    public Observable<String> synchronize(Context context, String uidUser) {
        return Observable.create(emitterMessage ->
                getAnnonceToRetrieve(uidUser)
                        .subscribeOn(scheduler).observeOn(scheduler)
                        .doOnSuccess(annonceFirebases -> synchroniseAnnonce(annonceFirebases, emitterMessage))
                        .subscribe()
        );
    }

    private Observable<AnnonceEntity> synchroniseAnnonce(List<AnnonceFirebase> listAnnonce, Emitter<String> emitterMessage) {
        return Observable.fromIterable(listAnnonce)
                .flatMapSingle(this::createNewAnnonceEntity)
                .flatMapSingle(annonceRepository::singleSave);
    }

    private Observable<PhotoEntity> synchronisePhoto(Context context, String uidUser, List<AnnonceFirebase> listAnnonce, Emitter<String> emitterMessage) {
        return Observable.fromIterable(listAnnonce)
                .subscribeOn(scheduler).observeOn(scheduler)
                .filter(annonceFirebase -> annonceFirebase.getPhotos() != null && !annonceFirebase.getPhotos().isEmpty())
                .doOnError(emitterMessage::onError)
                .doOnNext(annonceFirebase ->
                        annonceRepository.findByUidUserUidAnnonce(uidUser, annonceFirebase.getUuid())
                                .flatMapObservable(annonceEntity -> firebasePhotoStorage.saveSinglePhotoToLocalByListUrl(context, annonceEntity.getIdAnnonce(), annonceFirebase.getPhotos()))
                                .doOnNext(photoEntity -> {
                                    sendMessage(emitterMessage, photoEntity);
                                })
                                .doOnError(emitterMessage::onError)
                                .subscribe()
                );
    }

    private void sendMessage(Emitter<String> emitter, AnnonceEntity annonceEntity) {
        emitter.onNext("Récupération de l'annonce " + annonceEntity.getTitre());
    }

    private void sendMessage(Emitter<String> emitter, PhotoEntity photoEntity) {
        emitter.onNext("Récupération de la photo " + photoEntity.getFirebasePath());
    }

    private Observable<AnnonceFirebase> synchroniseAnnonceFirebase(Context context, String uidUser) {
        return Observable.create(emitter ->
                firebaseAnnonceRepository.observeAllAnnonceByUidUser(uidUser)
                        .subscribeOn(scheduler).observeOn(scheduler)
                        .doOnNext(annonceFirebase ->
                                annonceRepository.getMaybeByUidUserAndUidAnnonce(uidUser, annonceFirebase.getUuid())
                                        .subscribeOn(scheduler).observeOn(scheduler)
                                        .switchIfEmpty(createNewAnnonceEntity(annonceFirebase))
                                        .flatMap(annonceEntity -> annonceRepository.singleSave(annonceEntity))
                                        .doOnSuccess(annonceEntitySaved -> {
                                            if (annonceFirebase.getPhotos() != null && !annonceFirebase.getPhotos().isEmpty()) {
                                                firebasePhotoStorage.saveSinglePhotoToLocalByListUrl(context, annonceEntitySaved.getIdAnnonce(), annonceFirebase.getPhotos())
                                                        .subscribeOn(scheduler).observeOn(scheduler)
                                                        .doOnComplete(() -> emitter.onNext(annonceFirebase))
                                                        .subscribe();
                                            } else {
                                                emitter.onNext(annonceFirebase);
                                            }
                                        })
                                        .doOnError(emitter::onError)
                                        .subscribe()
                        )
                        .doOnError(emitter::onError)
                        .subscribe()
        );
    }

    private Single<AnnonceEntity> createNewAnnonceEntity(AnnonceFirebase annonceFirebase) {
        return Single.create(emitter -> {
            AnnonceEntity annonceEntity = AnnonceConverter.convertDtoToEntity(annonceFirebase);
            String uidUtilisateur = annonceFirebase.getUtilisateur().getUuid();
            annonceEntity.setUidUser(uidUtilisateur);
            emitter.onSuccess(annonceEntity);
        });
    }
}
