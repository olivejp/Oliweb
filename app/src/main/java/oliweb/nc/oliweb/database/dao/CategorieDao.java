package oliweb.nc.oliweb.database.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.CategorieEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Dao
public abstract class CategorieDao implements AbstractDao<CategorieEntity, Long> {

    @Override
    @Transaction
    @Query("SELECT * FROM categorie WHERE idCategorie = :idCategorie")
    public abstract Maybe<CategorieEntity> findById(Long idCategorie);

    @Override
    @Transaction
    @Query("SELECT * FROM categorie")
    public abstract Single<List<CategorieEntity>> getAll();

    @Override
    @Transaction
    @Query("SELECT COUNT(*) FROM categorie")
    public abstract Single<Integer> count();

    @Transaction
    @Query("SELECT * FROM categorie")
    public abstract Single<List<CategorieEntity>> getListCategorie();

    @Transaction
    @Query("SELECT name FROM categorie")
    public abstract Single<List<String>> getListCategorieLibelle();

    @Transaction
    @Query("DELETE FROM categorie")
    public abstract int deleteAll();
}
