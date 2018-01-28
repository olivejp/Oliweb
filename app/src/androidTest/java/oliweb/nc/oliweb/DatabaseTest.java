package oliweb.nc.oliweb;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.repository.AbstractRepositoryTask;
import oliweb.nc.oliweb.database.repository.CategorieRepository;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseTest {
    private OliwebDatabase oliwebDatabase;
    private CategorieRepository categorieRepository;
    private Context context;

    private CategorieEntity createCategorie(){
        CategorieEntity categorieEntity = new CategorieEntity();
        categorieEntity.setName("Name");
        categorieEntity.setCouleur("Couleur");
        return categorieEntity;
    }

    @Before
    public void createDb() {
        context = InstrumentationRegistry.getTargetContext();
        oliwebDatabase = Room.inMemoryDatabaseBuilder(context, OliwebDatabase.class).build();
        categorieRepository = CategorieRepository.getInstance(context);
    }

    @After
    public void closeDb() throws IOException {
        oliwebDatabase.close();
    }

    @Test
    public void writeCategorie() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        AbstractRepositoryTask.OnRespositoryPostExecute postExecute = new AbstractRepositoryTask.OnRespositoryPostExecute() {
            @Override
            public void onReposirotyPostExecute(Long[] ids) {
                if (ids.length > 0) {
                    Log.d("TEST", "Voici l'ID créé : " + ids[0]);

                    categorieRepository.findCategorieById(ids[0]).getValue();

                    Assert.assertTrue(true);
                } else {
                    Log.d("TEST", "Insertion échouée");
                    Assert.assertTrue(false);
                }
                signal.countDown();
            }
        };
        categorieRepository.insert(postExecute, createCategorie());
        signal.await();
    }
}
