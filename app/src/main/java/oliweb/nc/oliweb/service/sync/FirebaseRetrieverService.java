package oliweb.nc.oliweb.service.sync;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;

/**
 * Created by orlanth23 on 03/03/2018.
 * This class allows to retrieve {@link AnnonceDto} from Firebase corresponding to the given UID User.
 */
@Singleton
public class FirebaseRetrieverService {

    private static final String TAG = FirebaseRetrieverService.class.getName();

    @Inject
    FirebaseAnnonceRepository firebaseAnnonceRepository;

    @Inject
    public FirebaseRetrieverService() {
    }

    /**
     * Lecture de toutes les annonces présentes dans Firebase pour cet utilisateur
     * et récupération de ces annonces dans la base locale
     */
    void synchronize(Context context, String uidUser) {
        firebaseAnnonceRepository.saveAnnoncesByUidUser(context, uidUser);
    }
}
