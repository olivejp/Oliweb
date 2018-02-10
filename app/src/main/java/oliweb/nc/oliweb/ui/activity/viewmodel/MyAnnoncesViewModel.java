package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.List;

import oliweb.nc.oliweb.database.entity.AnnonceWithPhotos;
import oliweb.nc.oliweb.database.repository.AnnonceWithPhotosRepository;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MyAnnoncesViewModel extends AndroidViewModel {

    private AnnonceWithPhotosRepository annonceWithPhotosRepository;

    public MyAnnoncesViewModel(@NonNull Application application) {
        super(application);
        annonceWithPhotosRepository = AnnonceWithPhotosRepository.getInstance(application.getApplicationContext());
    }

    public LiveData<List<AnnonceWithPhotos>> findByUuidUtilisateur(String uuidUtilisateur) {
        return annonceWithPhotosRepository.findAllAnnoncesByUuidUtilisateur(uuidUtilisateur);
    }

}
