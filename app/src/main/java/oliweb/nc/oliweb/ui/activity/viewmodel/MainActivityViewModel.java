package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;

import oliweb.nc.oliweb.DateConverter;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.UtilisateurRepository;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MainActivityViewModel extends AndroidViewModel {


    private UtilisateurRepository utilisateurRepository;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        utilisateurRepository = UtilisateurRepository.getInstance(application.getApplicationContext());
    }

    public void createUtilisateur(FirebaseUser user, AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        UtilisateurEntity utilisateurEntity = new UtilisateurEntity();
        utilisateurEntity.setDateCreation(DateConverter.getNowEntity());
        utilisateurEntity.setEmail(user.getEmail());
        utilisateurEntity.setUuidUtilisateur(user.getUid());
        utilisateurEntity.setTelephone(user.getPhoneNumber());
        utilisateurRepository.save(utilisateurEntity, onRespositoryPostExecute);
    }


}
