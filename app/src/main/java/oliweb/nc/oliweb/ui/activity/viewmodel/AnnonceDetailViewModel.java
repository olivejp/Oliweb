package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;
import android.util.Log;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;
import oliweb.nc.oliweb.utility.CustomLiveData;
import oliweb.nc.oliweb.utility.LiveDataOnce;

public class AnnonceDetailViewModel extends AndroidViewModel {

    private static final String TAG = AnnonceDetailViewModel.class.getCanonicalName();

    private FirebaseUserRepository userRepository;

    public AnnonceDetailViewModel(@NonNull Application application) {
        super(application);
        ContextModule contextModule = new ContextModule(application);
        FirebaseRepositoriesComponent firebaseRepositoriesComponent = DaggerFirebaseRepositoriesComponent.builder().contextModule(contextModule).build();
        userRepository = firebaseRepositoriesComponent.getFirebaseUserRepository();
    }

    public LiveDataOnce<UserEntity> getFirebaseSeller(String uidSeller) {
        CustomLiveData<UserEntity> customLiveData = new CustomLiveData<>();
        userRepository.getUtilisateurByUid(uidSeller)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(customLiveData::postValue)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
        return customLiveData;
    }
}
