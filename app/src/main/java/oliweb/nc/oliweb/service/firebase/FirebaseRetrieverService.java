package oliweb.nc.oliweb.service.firebase;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import androidx.annotation.NonNull;
import io.reactivex.Scheduler;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
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
     * Va créer un observer pour toutes les catégories
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
                    String categorieLibelle = data.getValue(String.class);
                    if (categorieLibelle != null && !categorieLibelle.isEmpty()) {

                        // Lecture en base pour voir si la catégorie existe déjà
                        categorieRepository.findByLibelle(categorieLibelle)
                                .subscribeOn(scheduler).observeOn(scheduler)
                                .doOnComplete(() -> {
                                    // La catégorie n'existe pas en local, on va la créer.
                                    CategorieEntity categorieEntity = new CategorieEntity();
                                    categorieEntity.setName(categorieLibelle);
                                    categorieRepository.singleInsert(categorieEntity)
                                            .subscribeOn(scheduler).observeOn(scheduler)
                                            .doOnSuccess(categorieEntity1 -> Log.d(TAG, "Category correctly inserted."))
                                            .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                            .subscribe();
                                })
                                .doOnSuccess(categorieEntity1 -> Log.d(TAG, "Category already exists in local DB : " + categorieEntity1))
                                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                .subscribe();
                    }
                }
            } catch (Exception e1) {
                Log.e(TAG, e1.getLocalizedMessage(), e1);
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

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
     * Lecture de toutes les annonces présentes dans Firebase pour cet utilisateur
     * et récupération de ces annonces dans la base locale
     */
    public void synchronize(Context context, String uidUser) {
        firebaseAnnonceRepository.observeAllAnnonceByUidUser(uidUser)
                .subscribeOn(scheduler).observeOn(scheduler)
                .doOnNext(annonceDto -> {
                    Log.d(TAG, "Starting saveAnnonceDtoToLocalDb called with annonceDto = " + annonceDto.toString());
                    annonceRepository.countByUidUserAndUidAnnonce(annonceDto.getUtilisateur().getUuid(), annonceDto.getUuid())
                            .subscribeOn(scheduler).observeOn(scheduler)
                            .doOnSuccess(integer -> {
                                if (integer == null || integer.equals(0)) {
                                    AnnonceEntity annonceEntity = AnnonceConverter.convertDtoToEntity(annonceDto);
                                    String uidUtilisateur = annonceDto.getUtilisateur().getUuid();
                                    annonceEntity.setUidUser(uidUtilisateur);

                                    annonceRepository.singleSave(annonceEntity)
                                            .filter(annonceEntity1 -> annonceDto.getPhotos() != null && !annonceDto.getPhotos().isEmpty())
                                            .doOnSuccess(annonceEntity1 -> firebasePhotoStorage.savePhotoToLocalByListUrl(context, annonceEntity1.getIdAnnonce(), annonceDto.getPhotos()))
                                            .doOnError(throwable -> Log.e(TAG, "singleSave.doOnError " + throwable.getMessage()))
                                            .subscribe();
                                }
                            })
                            .doOnError(throwable -> Log.e(TAG, "countByUidUserAndUidAnnonce.doOnError " + throwable.getMessage()))
                            .subscribe();
                })
                .doOnError(throwable -> Log.e(TAG, "synchronize.doOnError " + throwable.getMessage()))
                .subscribe();
    }
}
