package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;

import oliweb.nc.oliweb.App;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.AnnonceService;
import oliweb.nc.oliweb.utility.LiveDataOnce;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class FavoriteActivityViewModel extends AndroidViewModel {

    @Inject
    AnnonceFullRepository annonceFullRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    AnnonceService annonceService;

    public FavoriteActivityViewModel(@NonNull Application application) {
        super(application);
        ((App) application).getDatabaseRepositoriesComponent().inject(this);
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
}
