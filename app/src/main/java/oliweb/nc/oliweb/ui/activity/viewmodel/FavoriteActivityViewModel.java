package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;

import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.AnnonceService;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.ServicesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;
import oliweb.nc.oliweb.utility.LiveDataOnce;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class FavoriteActivityViewModel extends AndroidViewModel {

    private static final String TAG = FavoriteActivityViewModel.class.getName();

    private AnnonceFullRepository annonceFullRepository;
    private UserRepository userRepository;
    private AnnonceService annonceService;
    private MutableLiveData<UserEntity> liveUserConnected;

    public FavoriteActivityViewModel(@NonNull Application application) {
        super(application);

        Log.d(TAG, "Création d'une nouvelle instance de MainActivityViewModel");

        ContextModule contextModule = new ContextModule(application);
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().contextModule(contextModule).build();
        ServicesComponent componentServices = DaggerServicesComponent.builder().contextModule(contextModule).build();
        annonceFullRepository = component.getAnnonceFullRepository();
        userRepository = component.getUserRepository();
        annonceService = componentServices.getAnnonceService();
    }

    public LiveData<List<AnnonceFull>> getFavoritesByUidUser(String uidUtilisateur) {
        return annonceFullRepository.findFavoritesByUidUser(uidUtilisateur);
    }

    public LiveData<UserEntity> findByUid(String uidUser) {
        return userRepository.findByUid(uidUser);
    }

    public LiveDataOnce<SearchActivityViewModel.AddRemoveFromFavorite> removeFromFavorite(String uidUser, AnnonceFull annonceFull) {
        return annonceService.addOrRemoveFromFavorite(uidUser, annonceFull);
    }

    public LiveData<UserEntity> getLiveUserConnected() {
        if (liveUserConnected == null) {
            liveUserConnected = new MutableLiveData<>();
        }
        return liveUserConnected;
    }
}
