package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import oliweb.nc.oliweb.database.entity.AnnonceWithChats;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Dao
public abstract class AnnonceWithChatsDao {

    @Transaction
    @Query("SELECT * FROM annonce WHERE uidUser = :uidUser")
    public abstract LiveData<List<AnnonceWithChats>> findByUidUser(String uidUser);

}
