package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;
import oliweb.nc.oliweb.firebase.FirebaseQueryLiveData;
import oliweb.nc.oliweb.utility.Constants;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_USER_REF;

public class ProfilViewModel extends AndroidViewModel {

    private FirebaseQueryLiveData fbSellerLiveData;
    private MutableLiveData<Long> nbAnnoncesByUser;
    private MutableLiveData<Long> nbChatsByUser;
    private MutableLiveData<Long> nbMessagesByUser;
    private UtilisateurRepository utilisateurRepository;

    public ProfilViewModel(@NonNull Application application) {
        super(application);
        utilisateurRepository = UtilisateurRepository.getInstance(application);
    }

    public LiveData<Long> getFirebaseUserNbMessagesCount(String uidUser) {
        if (nbMessagesByUser == null) {
            nbMessagesByUser = new MutableLiveData<>();
            nbMessagesByUser.setValue(0L);
        }
        FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_MESSAGES_REF).orderByChild("uidAuthor").equalTo(uidUser)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null) {
                            long count = dataSnapshot.getChildrenCount();
                            nbMessagesByUser.postValue(count);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Do nothing
                    }
                });
        return nbMessagesByUser;
    }

    public LiveData<Long> getFirebaseUserNbChatsCount(String uidUser) {
        if (nbChatsByUser == null) {
            nbChatsByUser = new MutableLiveData<>();
            nbChatsByUser.setValue(0L);
        }

        FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_CHATS_REF).orderByChild("members/" + uidUser).equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null) {
                            long count = dataSnapshot.getChildrenCount();
                            nbChatsByUser.postValue(count);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Do nothing
                    }
                });
        return nbChatsByUser;
    }

    public LiveData<Long> getFirebaseUserNbAnnoncesCount(String uidUser) {
        if (nbAnnoncesByUser == null) {
            nbAnnoncesByUser = new MutableLiveData<>();
            nbAnnoncesByUser.setValue(0L);
        }
        FirebaseDatabase.getInstance()
                .getReference(Constants.FIREBASE_DB_ANNONCE_REF)
                .orderByChild("utilisateur/uuid").equalTo(uidUser)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null) {
                            long count = dataSnapshot.getChildrenCount();
                            nbAnnoncesByUser.postValue(count);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Do nothing
                    }
                });
        return nbAnnoncesByUser;
    }

    public LiveData<DataSnapshot> getFirebaseUser(String uidUser) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_USER_REF).child(uidUser);
        if (fbSellerLiveData == null || fbSellerLiveData.getQuery() != ref) {
            fbSellerLiveData = new FirebaseQueryLiveData(ref, true);
        }
        return fbSellerLiveData;
    }

    public LiveData<UtilisateurEntity> getUtilisateurByUid(String uidUser) {
        return this.utilisateurRepository.findByUid(uidUser);
    }

    public Single<UtilisateurEntity> saveUtilisateur(UtilisateurEntity utilisateurEntity) {
        return this.utilisateurRepository.saveWithSingle(utilisateurEntity)
                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io());
    }
}
