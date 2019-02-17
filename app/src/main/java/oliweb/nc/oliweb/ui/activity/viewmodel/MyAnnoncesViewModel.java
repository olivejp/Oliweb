package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.App;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.service.firebase.DynamicLinkService;
import oliweb.nc.oliweb.service.firebase.FirebaseRetrieverService;
import oliweb.nc.oliweb.utility.CustomLiveData;
import oliweb.nc.oliweb.utility.FirebaseUtilityService;
import oliweb.nc.oliweb.utility.LiveDataOnce;
import oliweb.nc.oliweb.utility.MediaUtility;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MyAnnoncesViewModel extends AndroidViewModel {

    private static final String TAG = MyAnnoncesViewModel.class.getName();

    @Inject
    AnnonceWithPhotosRepository annonceWithPhotosRepository;

    @Inject
    AnnonceRepository annonceRepository;

    @Inject
    PhotoRepository photoRepository;

    @Inject
    FirebaseRetrieverService fbRetrieverService;

    @Inject
    MediaUtility mediaUtility;

    private MediatorLiveData<List<AnnoncePhotos>> liveListAnnonce;

    public MyAnnoncesViewModel(@NonNull Application application) {
        super(application);
        ((App) application).getDatabaseRepositoriesComponent().inject(this);
        ((App) application).getFirebaseServicesComponent().inject(this);
    }

    public MediaUtility getMediaUtility() {
        return mediaUtility;
    }

    public LiveData<List<AnnoncePhotos>> findAnnonceByUidUserAndPhotos(String uidUser) {
        if (liveListAnnonce == null) {
            liveListAnnonce = new MediatorLiveData<>();
        }
        LiveData<List<AnnoncePhotos>> liveDataPhotos = Transformations.switchMap(photoRepository.findAllByUidUser(uidUser), input -> annonceWithPhotosRepository.findActiveAnnonceByUidUser(uidUser));

        liveListAnnonce.addSource(annonceWithPhotosRepository.findActiveAnnonceByUidUser(uidUser), annoncePhotos -> liveListAnnonce.postValue(annoncePhotos));
        liveListAnnonce.addSource(liveDataPhotos, listAnnoncePhotos -> liveListAnnonce.postValue(listAnnoncePhotos));

        return liveListAnnonce;
    }

    public void shareAnnonce(Context context, AnnoncePhotos annoncePhotos, String uidUser) {
        DynamicLinkService.shareDynamicLink(context, annoncePhotos.getAnnonceEntity(), annoncePhotos.getPhotos(), uidUser, null, null);
    }

    public LiveData<List<AnnoncePhotos>> findAnnoncesByUidUser(String uuidUtilisateur) {
        return annonceWithPhotosRepository.findActiveAnnonceByUidUser(uuidUtilisateur);
    }

    public LiveDataOnce<AtomicBoolean> shouldIAskQuestionToRetreiveData(@Nullable String uidUtilisateur) {
        if (uidUtilisateur != null) {
            return fbRetrieverService.checkFirebaseRepository(uidUtilisateur);
        }
        return observer -> observer.onChanged(new AtomicBoolean(false));
    }

    public LiveDataOnce<AtomicBoolean> markToDelete(long idAnnonce) {
        CustomLiveData<AtomicBoolean> customLiveData = new CustomLiveData<>();
        this.annonceRepository.markAsToDelete(idAnnonce)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(customLiveData::postValue)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
        return customLiveData;
    }
}
