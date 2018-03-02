package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;

import io.reactivex.Single;
import oliweb.nc.oliweb.DateConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.UtilisateurRepository;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceSearchDto;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MainActivityViewModel extends AndroidViewModel {

    private AnnonceRepository annonceRepository;
    private AnnonceWithPhotosRepository annonceWithPhotosRepository;
    private UtilisateurRepository utilisateurRepository;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        annonceRepository = AnnonceRepository.getInstance(application.getApplicationContext());
        utilisateurRepository = UtilisateurRepository.getInstance(application.getApplicationContext());
        annonceWithPhotosRepository = AnnonceWithPhotosRepository.getInstance(application.getApplicationContext());
    }

    // TODO Should be deleted after this test.
    public Single<Integer> existByUidUtilisateurAndUidAnnonce(String UidUtilisateur, String UidAnnonce) {
        return annonceRepository.existByUidUtilisateurAndUidAnnonce(UidUtilisateur, UidAnnonce);
    }

    public void createUtilisateur(FirebaseUser user, AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        UtilisateurEntity utilisateurEntity = new UtilisateurEntity();
        utilisateurEntity.setDateCreation(DateConverter.getNowEntity());
        utilisateurEntity.setEmail(user.getEmail());
        utilisateurEntity.setUuidUtilisateur(user.getUid());
        utilisateurEntity.setTelephone(user.getPhoneNumber());
        utilisateurRepository.save(utilisateurEntity, onRespositoryPostExecute);
    }

    public void saveAnnonceFromFirebaseToLocalDb(AnnonceSearchDto annonceSearchDto) {
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annonceEntity.setStatut(StatusRemote.SEND);
        annonceEntity.setTitre(annonceSearchDto.getTitre());
        annonceEntity.setDescription(annonceSearchDto.getDescription());
        annonceEntity.setDatePublication(annonceSearchDto.getDatePublication());
        annonceEntity.setPrix(annonceSearchDto.getPrix());
        annonceEntity.setIdCategorie(annonceSearchDto.getCategorie().getId());
        annonceEntity.setUuidUtilisateur(annonceSearchDto.getUtilisateur().getUuid());
        annonceRepository.save(annonceEntity, dataReturn -> {
            // Now we should save Photos
            if (dataReturn.getIds().length > 0){
                long idAnnonce = dataReturn.getIds()[0];
                for (String photoUrl : annonceSearchDto.getPhotos()) {
                    // TODO finir la récupération des annonces pour un utilisateur
                }
            }
        });
    }
}
