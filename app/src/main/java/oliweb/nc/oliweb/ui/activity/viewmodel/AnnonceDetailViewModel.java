package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;
import android.util.Log;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseRepositoriesComponent;

public class AnnonceDetailViewModel extends ViewModel {

    private static final String TAG = AnnonceDetailViewModel.class.getCanonicalName();

    private FirebaseUserRepository firebaseUserRepository;

    public AnnonceDetailViewModel() {
        super();
        FirebaseRepositoriesComponent firebaseRepositoriesComponent = DaggerFirebaseRepositoriesComponent.builder().build();
        firebaseUserRepository = firebaseRepositoriesComponent.getFirebaseUserRepository();
    }

    public LiveData<UserEntity> getFirebaseSeller(String uidSeller) {
        return new LiveData<UserEntity>() {
            @Override
            public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<UserEntity> observer) {
                super.observe(owner, observer);
                firebaseUserRepository.getUtilisateurByUid(uidSeller)
                        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess(observer::onChanged)
                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                        .subscribe();
            }
        };
    }
}
