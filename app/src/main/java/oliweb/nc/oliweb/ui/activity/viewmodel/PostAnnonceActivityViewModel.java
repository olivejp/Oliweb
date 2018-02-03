package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import io.reactivex.Maybe;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.CategorieRepository;
import oliweb.nc.oliweb.database.repository.PhotoRepository;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;

/**
 * Created by orlanth23 on 31/01/2018.
 */

public class PostAnnonceActivityViewModel extends AndroidViewModel {

    private CategorieRepository categorieRepository;
    private AnnonceRepository annonceRepository;
    private PhotoRepository photoRepository;
    private MutableLiveData<AnnonceEntity> annonceEntityMutableLiveData;


    public PostAnnonceActivityViewModel(@NonNull Application application) {
        super(application);
        categorieRepository = CategorieRepository.getInstance(application);
        photoRepository = PhotoRepository.getInstance(application);
        annonceRepository = AnnonceRepository.getInstance(application);
    }

    public Maybe<List<CategorieEntity>> maybeListCategorie() {
        return categorieRepository.getListCategorie();
    }

    public void savePhoto(PhotoEntity photoEntity, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        this.photoRepository.save(photoEntity, onRespositoryPostExecute);
    }

    public void saveAnnonce(AnnonceEntity annonceEntity, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        this.annonceRepository.save(annonceEntity, onRespositoryPostExecute);
    }

    public void setCurrentAnnonce(@NonNull AnnonceEntity annonceEntity){
        if (annonceEntityMutableLiveData == null) {
            annonceEntityMutableLiveData = new MutableLiveData<>();
        }
        annonceEntityMutableLiveData.setValue(annonceEntity);
    }

    public LiveData<AnnonceEntity> getCurrentAnnonce(){
        if (annonceEntityMutableLiveData == null) {
            annonceEntityMutableLiveData = new MutableLiveData<>();
        }
        return annonceEntityMutableLiveData;
    }
}
