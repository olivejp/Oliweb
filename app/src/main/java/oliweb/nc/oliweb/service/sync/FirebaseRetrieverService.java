package oliweb.nc.oliweb.service.sync;

import android.content.Context;

import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;

/**
 * Created by orlanth23 on 03/03/2018.
 * This class allows to retreive {@link AnnonceDto} from Firebase corresponding to the given UID User.
 */
public class FirebaseRetrieverService {

    private static final String TAG = FirebaseRetrieverService.class.getName();

    private static FirebaseRetrieverService instance;

    private FirebaseAnnonceRepository firebaseAnnonceRepository;

    private FirebaseRetrieverService() {
    }

    public static FirebaseRetrieverService getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseRetrieverService();
        }
        instance.firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(context);
        return instance;
    }

    /**
     * Lecture de toutes les annonces présentes dans Firebase pour cet utilisateur
     * et récupération de ces annonces dans la base locale
     *
     * @param context
     * @param uidUser
     */
    public void synchronize(Context context, String uidUser) {
        firebaseAnnonceRepository.saveAnnoncesByUidUser(context, uidUser);
    }
}
