package oliweb.nc.oliweb;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.CategorieRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseTest {
    private OliwebDatabase oliwebDatabase;
    private CategorieRepository categorieRepository;
    private UtilisateurRepository utilisateurRepository;
    private AnnonceRepository annonceRepository;
    private PhotoRepository photoRepository;
    private Context context;

    private static final String EMAIL = "orlanth23@hotmail.com";
    private static final String NAME_CATEGORIE = "Automobiles";
    private static final String UUID_UTILISATEUR = "UUIDUtilisateur";

    private CategorieEntity createCategorie() {
        CategorieEntity categorieEntity = new CategorieEntity();
        categorieEntity.setName(NAME_CATEGORIE);
        categorieEntity.setCouleur("Couleur");
        return categorieEntity;
    }

    private UtilisateurEntity createUtilisateur() {
        UtilisateurEntity utilisateurEntity = new UtilisateurEntity();
        utilisateurEntity.setDateCreation(Utility.getNowInEntityFormat());
        utilisateurEntity.setTelephone("123456");
        utilisateurEntity.setEmail(EMAIL);
        utilisateurEntity.setUuidUtilisateur(UUID_UTILISATEUR);
        return utilisateurEntity;
    }

    private AnnonceEntity createAnnonce(String uuidUtilisateur, Long idCategorie) {
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annonceEntity.setTitre("Name");
        annonceEntity.setContactByEmail("1");
        annonceEntity.setContactByMsg("0");
        annonceEntity.setContactByTel("1");
        annonceEntity.setDatePublication(Utility.getNowInEntityFormat());
        annonceEntity.setDescription("Description");
        annonceEntity.setPrix(15000);
        annonceEntity.setStatut(StatusRemote.TO_SEND);
        annonceEntity.setUUID("123456");
        annonceEntity.setIdCategorie(idCategorie);
        annonceEntity.setUuidUtilisateur(uuidUtilisateur);
        return annonceEntity;
    }

    private PhotoEntity createPhoto(Long idAnnonce) {
        PhotoEntity photoEntity = new PhotoEntity();
        photoEntity.setIdAnnonce(idAnnonce);
        photoEntity.setUriLocal("CheminLocal");
        photoEntity.setStatut(StatusRemote.SEND);
        photoEntity.setFirebasePath("firebasePath");
        return photoEntity;
    }

    @Before
    public void createDb() {
        context = InstrumentationRegistry.getTargetContext();
        oliwebDatabase = Room.inMemoryDatabaseBuilder(context, OliwebDatabase.class).build();
        categorieRepository = CategorieRepository.getInstance(context);
        utilisateurRepository = UtilisateurRepository.getInstance(context);
        annonceRepository = AnnonceRepository.getInstance(context);
        photoRepository = PhotoRepository.getInstance(context);
    }

    @Test
    public void writeCategorie() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        AbstractRepositoryCudTask.OnRespositoryPostExecute postExecute = dataReturn -> {
            if (dataReturn.getIds().length > 0) {
                Assert.assertTrue(true);
            } else {
                Assert.assertTrue(false);
            }
            signal.countDown();
        };
        categorieRepository.insert(postExecute, createCategorie());
        signal.await();
    }

    @Test
    public void writeUtilisateur() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        AbstractRepositoryCudTask.OnRespositoryPostExecute postExecute = dataReturn -> {
            if (dataReturn.getIds().length > 0) {
                String email = utilisateurRepository.findByUid(UUID_UTILISATEUR).getValue().getEmail();
                Assert.assertEquals(EMAIL, email);
            } else {
                Assert.assertTrue(false);
            }
            signal.countDown();
        };
        utilisateurRepository.insert(postExecute, createUtilisateur());
        signal.await();
    }

    @After
    public void closeDb() throws IOException {
        oliwebDatabase.close();
    }
}
