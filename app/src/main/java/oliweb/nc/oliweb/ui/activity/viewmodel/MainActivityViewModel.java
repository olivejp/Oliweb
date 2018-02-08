package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;

import io.reactivex.Single;
import oliweb.nc.oliweb.DateConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.UtilisateurRepository;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MainActivityViewModel extends AndroidViewModel {

    private AnnonceRepository annonceRepository;
    private UtilisateurRepository utilisateurRepository;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        annonceRepository = AnnonceRepository.getInstance(application.getApplicationContext());
        utilisateurRepository = UtilisateurRepository.getInstance(application.getApplicationContext());
    }

    public Single<AnnonceEntity> findSingleAnnonceById(long idAnnonce) {
        return annonceRepository.findSingleById(idAnnonce);
    }

    public void createUtilisateur(FirebaseUser user, AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute){
        UtilisateurEntity utilisateurEntity = new UtilisateurEntity();
        utilisateurEntity.setDateCreation(DateConverter.getNowEntity());
        utilisateurEntity.setEmail(user.getEmail());
        utilisateurEntity.setUuidUtilisateur(user.getUid());
        utilisateurEntity.setTelephone(user.getPhoneNumber());
        utilisateurRepository.save(utilisateurEntity, onRespositoryPostExecute);
    }
}
