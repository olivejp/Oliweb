package oliweb.nc.oliweb;

import java.util.Collections;

import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;

public class Utility {

    public static final String UID_USER = "123";
    public static final String UID_ANNONCE = "456";
    public static final String MON_TITRE = "Mon titre";
    public static final String MA_DESCRIPTION = "Ma description";
    public static final int PRIX = 7000;
    public static final long ID_CATEGORIE = 1L;
    public static final String LIBELLE_CATEGORIE = "AUTOMOBILE";
    public static final String PROFILE_USER = "PROFILE";
    public static final String EMAIL = "EMAIL";
    public static final String PHOTO_URL_USER = "PHOTO_URL";
    public static final String TELEPHONE_USER = "790723";
    public static final String TOKEN_DEVICE_USER = "TOKEN_DEVICE";
    public static final String PHOTO_FIREBASE_URL = "MY_FIREBASE_URL";
    public static final long ID_ANNONCE = 1L;
    public static final long ID_PHOTO = 10L;
    public static final String URI_LOCAL_PHOTO = "MY_URI_LOCAL";

    public static AnnonceFull createAnnonceFull() {
        // Préparation des données du test
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annonceEntity.setIdAnnonce(ID_ANNONCE);
        annonceEntity.setUidUser(UID_USER);
        annonceEntity.setUid(UID_ANNONCE);
        annonceEntity.setTitre(MON_TITRE);
        annonceEntity.setDescription(MA_DESCRIPTION);
        annonceEntity.setPrix(PRIX);
        annonceEntity.setIdCategorie(ID_CATEGORIE);
        annonceEntity.setContactByEmail("O");
        annonceEntity.setContactByTel("N");
        annonceEntity.setContactByMsg("O");
        annonceEntity.setStatut(StatusRemote.NOT_TO_SEND);

        CategorieEntity categorieEntity = new CategorieEntity();
        categorieEntity.setCouleur("couleur");
        categorieEntity.setIdCategorie(1L);
        categorieEntity.setName(LIBELLE_CATEGORIE);

        UserEntity userEntity = new UserEntity();
        userEntity.setUid(UID_USER);
        userEntity.setProfile(PROFILE_USER);
        userEntity.setEmail(EMAIL);
        userEntity.setPhotoUrl(PHOTO_URL_USER);
        userEntity.setTelephone(TELEPHONE_USER);
        userEntity.setTokenDevice(TOKEN_DEVICE_USER);

        PhotoEntity photoEntity = new PhotoEntity();
        photoEntity.setFirebasePath(PHOTO_FIREBASE_URL);
        photoEntity.setIdAnnonce(ID_ANNONCE);
        photoEntity.setIdPhoto(ID_PHOTO);
        photoEntity.setStatut(StatusRemote.NOT_TO_SEND);
        photoEntity.setUriLocal(URI_LOCAL_PHOTO);

        AnnonceFull annonceFull = new AnnonceFull();
        annonceFull.setAnnonce(annonceEntity);
        annonceFull.setCategorie(Collections.singletonList(categorieEntity));
        annonceFull.setPhotos(Collections.singletonList(photoEntity));
        annonceFull.setUtilisateur(Collections.singletonList(userEntity));

        return annonceFull;
    }
}
