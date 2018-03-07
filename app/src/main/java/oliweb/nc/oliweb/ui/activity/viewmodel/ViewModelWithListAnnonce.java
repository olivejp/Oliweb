package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.arch.lifecycle.LiveData;

import java.util.List;

import oliweb.nc.oliweb.database.entity.AnnonceEntity;

/**
 * Created by 2761oli on 07/03/2018.
 */
// TODO finir d'implémenter cette nouvelle interface
public interface ViewModelWithListAnnonce {
    LiveData<List<AnnonceEntity>> getLiveListAnnonce();
}
