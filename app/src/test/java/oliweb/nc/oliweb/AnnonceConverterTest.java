package oliweb.nc.oliweb;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.dto.elasticsearch.AnnonceDto;
import oliweb.nc.oliweb.dto.elasticsearch.UtilisateurDto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class AnnonceConverterTest {

    private static final String UID_USER = "123";
    private static final String UID_ANNONCE = "456";
    private static final String MON_TITRE = "Mon titre";
    private static final String MA_DESCRIPTION = "Ma description";
    private static final int PRIX = 7000;
    private static final long ID_CATEGORIE = 1L;
    private static final String LIBELLE_CATEGORIE = "AUTOMOBILE";
    private static final String PROFILE_USER = "PROFILE";
    private static final String EMAIL = "EMAIL";
    private static final String PHOTO_URL_USER = "PHOTO_URL";
    private static final String TELEPHONE_USER = "790723";
    private static final String TOKEN_DEVICE_USER = "TOKEN_DEVICE";
    private static final String PHOTO_FIREBASE_URL = "MY_FIREBASE_URL";
    private static final String PHOTO_FIREBASE_URL2 = "MY_FIREBASE_URL_2";
    private static final long ID_ANNONCE = 1L;
    private static final long ID_PHOTO = 10L;
    private static final String URI_LOCAL_PHOTO = "MY_URI_LOCAL";

    @Before
    public void setUp() {
    }

    @Test
    public void testConvertDtoToEntity() {

        UtilisateurDto utilisateurDto = new UtilisateurDto();
        utilisateurDto.setEmail(EMAIL);
        utilisateurDto.setPhotoUrl(PHOTO_URL_USER);
        utilisateurDto.setProfile(PROFILE_USER);
        utilisateurDto.setTelephone(TELEPHONE_USER);
        utilisateurDto.setUuid(UID_USER);

        AnnonceDto annonceDto = new AnnonceDto();
        annonceDto.setUuid(UID_ANNONCE);
        annonceDto.setTitre(MON_TITRE);
        annonceDto.setDescription(MA_DESCRIPTION);
        annonceDto.setContactEmail(true);
        annonceDto.setContactTel(false);
        annonceDto.setContactMsg(true);
        annonceDto.setPrix(PRIX);
        annonceDto.setUtilisateur(utilisateurDto);
        annonceDto.setPhotos(Arrays.asList(PHOTO_FIREBASE_URL, PHOTO_FIREBASE_URL2));

        AnnonceFull annonceFull = AnnonceConverter.convertDtoToAnnonceFull(annonceDto);

        assertNotNull(annonceFull);
        assertNotNull(annonceFull.getAnnonce());
        assertNotNull(annonceFull.getUtilisateur());
        assertNotNull(annonceFull.getPhotos());

        assertEquals(2, annonceFull.getPhotos().size());
        assertEquals(1, annonceFull.getUtilisateur().size());

        assertEquals(UID_ANNONCE, annonceFull.getAnnonce().getUid());
        assertEquals(MON_TITRE, annonceFull.getAnnonce().getTitre());
        assertEquals(MA_DESCRIPTION, annonceFull.getAnnonce().getDescription());
        assertEquals(PRIX, annonceFull.getAnnonce().getPrix().intValue());
        assertEquals("O", annonceFull.getAnnonce().getContactByEmail());
        assertEquals("N", annonceFull.getAnnonce().getContactByTel());
        assertEquals("O", annonceFull.getAnnonce().getContactByMsg());

        assertEquals(EMAIL, annonceFull.getUtilisateur().get(0).getEmail());
        assertEquals(PROFILE_USER, annonceFull.getUtilisateur().get(0).getProfile());
        assertEquals(TELEPHONE_USER, annonceFull.getUtilisateur().get(0).getTelephone());
        assertEquals(UID_USER, annonceFull.getUtilisateur().get(0).getUid());
        assertEquals(PHOTO_URL_USER, annonceFull.getUtilisateur().get(0).getPhotoUrl());

        assertEquals(PHOTO_FIREBASE_URL, annonceFull.getPhotos().get(0).getFirebasePath());
        assertEquals(PHOTO_FIREBASE_URL2, annonceFull.getPhotos().get(1).getFirebasePath());
    }

    @Test
    public void testConvertFullToDto() {

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


        // Lancement du test
        AnnonceDto annonceDto = AnnonceConverter.convertFullEntityToDto(annonceFull);

        assertNotNull(annonceDto);
        assertNotNull(annonceDto.getCategorie());
        assertNotNull(annonceDto.getUtilisateur());
        assertNotNull(annonceDto.getPhotos());

        assertEquals(UID_ANNONCE, annonceDto.getUuid());
        assertEquals(MON_TITRE, annonceDto.getTitre());
        assertEquals(MA_DESCRIPTION, annonceDto.getDescription());
        assertEquals(PRIX, annonceDto.getPrix());

        assertEquals(UID_USER, annonceDto.getUtilisateur().getUuid());
        assertEquals(EMAIL, annonceDto.getUtilisateur().getEmail());
        assertEquals(PHOTO_URL_USER, annonceDto.getUtilisateur().getPhotoUrl());
        assertEquals(PROFILE_USER, annonceDto.getUtilisateur().getProfile());
        assertEquals(TELEPHONE_USER, annonceDto.getUtilisateur().getTelephone());

        assertEquals(1, annonceDto.getPhotos().size());
        assertEquals(PHOTO_FIREBASE_URL, annonceDto.getPhotos().get(0));

        assertTrue(annonceDto.isContactEmail());
        assertTrue(annonceDto.isContactMsg());
        assertFalse(annonceDto.isContactTel());
    }
}
