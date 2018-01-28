package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import io.reactivex.Maybe;
import oliweb.nc.oliweb.database.entity.CategorieEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Dao
public interface CategorieDao extends AbstractDao<CategorieEntity> {
    @Transaction
    @Query("SELECT * FROM categorie WHERE idCategorie = :idCategorie")
    LiveData<CategorieEntity> findById(Long idCategorie);

    @Transaction
    @Query("SELECT * FROM categorie")
    Maybe<List<CategorieEntity>> getListCategorie();
}
