package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;

import javax.inject.Inject;

import oliweb.nc.oliweb.App;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.service.AnnonceService;
import oliweb.nc.oliweb.utility.LiveDataOnce;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class AnnonceDetailActivityViewModel extends AndroidViewModel {

    private static final String TAG = AnnonceDetailActivityViewModel.class.getName();

    @Inject
    AnnonceFullRepository annonceFullRepository;

    @Inject
    AnnonceService annonceService;

    public AnnonceDetailActivityViewModel(@NonNull Application application) {
        super(application);

        Log.d(TAG, "Création d'une nouvelle instance de AnnonceDetailActivityViewModel");

        ((App) application).getDatabaseRepositoriesComponent().inject(this);
        ((App) application).getServicesComponent().inject(this);
    }

    public LiveData<List<AnnonceFull>> getFavoritesByUidUser(String uidUtilisateur) {
        return annonceFullRepository.findFavoritesByUidUser(uidUtilisateur);
    }

    public LiveDataOnce<SearchActivityViewModel.AddRemoveFromFavorite> addOrRemoveFromFavorite(String uidUser, AnnonceFull annonceFull) {
        return annonceService.addOrRemoveFromFavorite(uidUser, annonceFull);
    }
}
