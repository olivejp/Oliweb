package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.service.firebase.FirebaseRetrieverService;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;
import oliweb.nc.oliweb.utility.CustomLiveData;
import oliweb.nc.oliweb.utility.LiveDataOnce;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MyAnnoncesViewModel extends AndroidViewModel {

    private static final String TAG = MyAnnoncesViewModel.class.getName();

    private AnnonceWithPhotosRepository annonceWithPhotosRepository;
    private AnnonceRepository annonceRepository;
    private CustomLiveData<AtomicBoolean> shouldAskQuestion;
    private FirebaseRetrieverService fbRetrieverService;

    public MyAnnoncesViewModel(@NonNull Application application) {
        super(application);
        ContextModule contextModule = new ContextModule(application);
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().contextModule(contextModule).build();
        FirebaseServicesComponent componentFbServices = DaggerFirebaseServicesComponent.builder().contextModule(contextModule).build();
        annonceWithPhotosRepository = component.getAnnonceWithPhotosRepository();
        annonceRepository = component.getAnnonceRepository();
        fbRetrieverService = componentFbServices.getFirebaseRetrieverService();
    }

    public LiveData<List<AnnoncePhotos>> findAnnoncesByUidUser(String uuidUtilisateur) {
        return annonceWithPhotosRepository.findActiveAnnonceByUidUser(uuidUtilisateur);
    }

    public LiveDataOnce<AtomicBoolean> shouldIAskQuestionToRetreiveData(@Nullable String uidUtilisateur) {
        if (shouldAskQuestion == null) {
            shouldAskQuestion = new CustomLiveData<>();
        }
        if (uidUtilisateur != null) {
            fbRetrieverService.checkFirebaseRepository(uidUtilisateur, shouldAskQuestion);
        }
        return shouldAskQuestion;
    }

    public LiveDataOnce<AtomicBoolean> markToDelete(long idAnnonce) {
        CustomLiveData<AtomicBoolean> customLiveData = new CustomLiveData<>();
        this.annonceRepository.markAsToDelete(idAnnonce)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(customLiveData::postValue)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
        return customLiveData;
    }
}
