package oliweb.nc.oliweb.database.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.dto.firebase.CategorieFirebase;
import oliweb.nc.oliweb.dto.firebase.UserFirebase;


public class AnnonceConverter {

    private AnnonceConverter() {
    }

    /**
     * @param annonceFirebase
     * @return
     */
    public static AnnonceFull convertDtoToAnnonceFull(AnnonceFirebase annonceFirebase) {
        AnnonceEntity annonceEntity = convertDtoToEntity(annonceFirebase);
        if (annonceEntity != null) {
            AnnonceFull annonceFull = new AnnonceFull();

            // Récupération de l'utilisateur
            UserFirebase userFirebase = annonceFirebase.getUtilisateur();
            UserEntity userEntity = UserConverter.convertDtoToEntity(userFirebase);
            annonceFull.setUtilisateur(Collections.singletonList(userEntity));

            // Récupération des photos
            annonceFull.setPhotos(new ArrayList<>());
            if (annonceFirebase.getPhotos() != null && !annonceFirebase.getPhotos().isEmpty()) {
                for (String photoUrl : annonceFirebase.getPhotos()) {
                    PhotoEntity photoEntity = new PhotoEntity();
                    photoEntity.setFirebasePath(photoUrl);
                    annonceFull.getPhotos().add(photoEntity);
                }
            }

            // Récupération de l'annonce
            annonceFull.setAnnonce(annonceEntity);

            // Récupération de la catégorie
            CategorieEntity categorieEntity = new CategorieEntity();
            categorieEntity.setIdCategorie(annonceFirebase.getCategorie().getId());
            categorieEntity.setName(annonceFirebase.getCategorie().getLibelle());
            annonceFull.setCategorie(Collections.singletonList(categorieEntity));

            // Récupération du statut
            annonceEntity.setStatut(StatusRemote.NOT_TO_SEND);
            return annonceFull;
        }
        return null;
    }

    /**
     * @param annonceFull
     * @return
     */
    public static AnnonceFirebase convertFullEntityToDto(AnnonceFull annonceFull) {
        AnnonceFirebase annonceFirebase = new AnnonceFirebase();
        UserEntity userEntity = annonceFull.getUtilisateur().get(0);
        UserFirebase userFirebase = new UserFirebase(userEntity.getProfile(), userEntity.getUid(), userEntity.getTelephone(), userEntity.getEmail(), userEntity.getPhotoUrl());
        annonceFirebase.setUtilisateur(userFirebase);

        CategorieEntity categorieEntity = annonceFull.getCategorie().get(0);
        annonceFirebase.setCategorie(new CategorieFirebase(categorieEntity.getId(), categorieEntity.getName()));

        if (annonceFull.getAnnonce().getDatePublication() != null) {
            annonceFirebase.setDatePublication(annonceFull.getAnnonce().getDatePublication());
        }

        annonceFirebase.setDescription(annonceFull.getAnnonce().getDescription());
        annonceFirebase.setTitre(annonceFull.getAnnonce().getTitre());
        annonceFirebase.setPrix(annonceFull.getAnnonce().getPrix());
        annonceFirebase.setUuid(annonceFull.getAnnonce().getUid());

        annonceFirebase.setContactEmail(annonceFull.getAnnonce().getContactByEmail() != null && annonceFull.getAnnonce().getContactByEmail().equals("O"));
        annonceFirebase.setContactTel(annonceFull.getAnnonce().getContactByTel() != null && annonceFull.getAnnonce().getContactByTel().equals("O"));
        annonceFirebase.setContactMsg(annonceFull.getAnnonce().getContactByMsg() != null && annonceFull.getAnnonce().getContactByMsg().equals("O"));

        List<String> listPhotoDto = new ArrayList<>();
        for (PhotoEntity photo : annonceFull.getPhotos()) {
            if (photo.getFirebasePath() != null && !photo.getFirebasePath().isEmpty()) {
                listPhotoDto.add(photo.getFirebasePath());
            }
        }
        annonceFirebase.setPhotos(listPhotoDto);

        return annonceFirebase;
    }

    public static AnnonceEntity convertDtoToEntity(AnnonceFirebase annonceFirebase) {
        if (annonceFirebase != null) {
            AnnonceEntity annonceEntity = new AnnonceEntity();
            annonceEntity.setUid(annonceFirebase.getUuid());
            annonceEntity.setTitre(annonceFirebase.getTitre());
            annonceEntity.setDescription(annonceFirebase.getDescription());
            annonceEntity.setDatePublication(annonceFirebase.getDatePublication());
            annonceEntity.setPrix(annonceFirebase.getPrix());
            annonceEntity.setStatut(StatusRemote.SEND);
            annonceEntity.setContactByMsg(annonceFirebase.isContactMsg() ? "O" : "N");
            annonceEntity.setContactByTel(annonceFirebase.isContactTel() ? "O" : "N");
            annonceEntity.setContactByEmail(annonceFirebase.isContactEmail() ? "O" : "N");
            annonceEntity.setFavorite(0);

            if (annonceFirebase.getCategorie() != null) {
                annonceEntity.setIdCategorie(annonceFirebase.getCategorie().getId());
            }

            if (annonceFirebase.getUtilisateur() != null) {
                annonceEntity.setUidUser(annonceFirebase.getUtilisateur().getUuid());
            }

            return annonceEntity;
        }
        return null;
    }
}
