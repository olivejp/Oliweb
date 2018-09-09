package oliweb.nc.oliweb;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.dto.elasticsearch.AnnonceDto;
import oliweb.nc.oliweb.dto.elasticsearch.UtilisateurDto;

import static oliweb.nc.oliweb.Utility.EMAIL;
import static oliweb.nc.oliweb.Utility.MA_DESCRIPTION;
import static oliweb.nc.oliweb.Utility.MON_TITRE;
import static oliweb.nc.oliweb.Utility.PHOTO_FIREBASE_URL;
import static oliweb.nc.oliweb.Utility.PHOTO_URL_USER;
import static oliweb.nc.oliweb.Utility.PRIX;
import static oliweb.nc.oliweb.Utility.PROFILE_USER;
import static oliweb.nc.oliweb.Utility.TELEPHONE_USER;
import static oliweb.nc.oliweb.Utility.UID_ANNONCE;
import static oliweb.nc.oliweb.Utility.UID_USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class AnnonceConverterTest {

   private static final String PHOTO_FIREBASE_URL2 = "PHOTO_URL_2";

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
        AnnonceFull annonceFull = Utility.createAnnonceFull();

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