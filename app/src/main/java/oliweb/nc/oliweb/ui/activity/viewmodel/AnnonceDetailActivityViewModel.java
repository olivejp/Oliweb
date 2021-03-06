package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.util.Log;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.App;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
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
    FirebaseAnnonceRepository firebaseAnnonceRepository;

    @Inject
    AnnonceService annonceService;

    public AnnonceDetailActivityViewModel(@NonNull Application application) {
        super(application);

        Log.d(TAG, "Création d'une nouvelle instance de AnnonceDetailActivityViewModel");

        ((App) application).getDatabaseRepositoriesComponent().inject(this);
        ((App) application).getServicesComponent().inject(this);
    }

    public LiveData<Integer> getCountFavoritesByUidUserAndByUidAnnonce(String uidUtilisateur, String uidAnnonce) {
        return annonceFullRepository.findFavoritesByUidUserAndByUidAnnonce(uidUtilisateur, uidAnnonce);
    }

    public LiveDataOnce<SearchActivityViewModel.AddRemoveFromFavorite> addOrRemoveFromFavorite(String uidUser, AnnonceFull annonceFull) {
        return annonceService.addOrRemoveFromFavorite(uidUser, annonceFull);
    }

    public Single<List<AnnonceFirebase>> getListAnnonceByUidUser(String uidUser) {
        return firebaseAnnonceRepository.observeAllAnnonceByUidUser(uidUser)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .toList();
    }
}
