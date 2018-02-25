package oliweb.nc.oliweb.ui.adapter;

import java.util.List;

import oliweb.nc.oliweb.database.entity.AnnonceWithPhotos;

/**
 * Created by orlanth23 on 25/02/2018.
 */

public interface AnnonceAdapter {
    void setListAnnonces(final List<AnnonceWithPhotos> newListAnnonces);
}
