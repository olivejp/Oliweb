package oliweb.nc.oliweb.database.converter;

import java.util.ArrayList;
import java.util.List;

import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.network.elasticsearchDto.CategorieDto;
import oliweb.nc.oliweb.network.elasticsearchDto.UtilisateurDto;


public class AnnonceConverter {

    private AnnonceConverter() {
    }

    /**
     * @param annonceDto
     * @return
     */
    public static AnnoncePhotos convertDtoToEntity(AnnonceDto annonceDto) {
        AnnoncePhotos annoncePhotos = new AnnoncePhotos();
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annoncePhotos.setPhotos(new ArrayList<>());

        annonceEntity.setUUID(annonceDto.getUuid());
        annonceEntity.setTitre(annonceDto.getTitre());
        annonceEntity.setDescription(annonceDto.getDescription());
        annonceEntity.setDatePublication(annonceDto.getDatePublication());
        annonceEntity.setPrix(annonceDto.getPrix());
        annonceEntity.setIdCategorie(annonceDto.getCategorie().getId());
        annonceEntity.setUuidUtilisateur(annonceDto.getUtilisateur().getUuid());
        annonceEntity.setStatut(StatusRemote.NOT_TO_SEND);
        if (annonceDto.getPhotos() != null && !annonceDto.getPhotos().isEmpty()) {
            for (String photoUrl : annonceDto.getPhotos()) {
                PhotoEntity photoEntity = new PhotoEntity();
                photoEntity.setFirebasePath(photoUrl);
                annoncePhotos.getPhotos().add(photoEntity);
            }
        }
        annoncePhotos.setAnnonceEntity(annonceEntity);

        return annoncePhotos;
    }

    /**
     * @param annonceFull
     * @return
     */
    public static AnnonceDto convertEntityToDto(AnnonceFull annonceFull) {
        AnnonceDto annonceDto = new AnnonceDto();
        UtilisateurEntity utilisateurEntity = annonceFull.getUtilisateur().get(0);
        UtilisateurDto utilisateurDto = new UtilisateurDto(utilisateurEntity.getProfile(), utilisateurEntity.getUuidUtilisateur(), utilisateurEntity.getTelephone(), utilisateurEntity.getEmail());
        annonceDto.setUtilisateur(utilisateurDto);

        CategorieEntity categorieEntity = annonceFull.getCategorie().get(0);
        annonceDto.setCategorie(new CategorieDto(categorieEntity.getIdCategorie(), categorieEntity.getName()));

        annonceDto.setDatePublication(annonceFull.getAnnonce().getDatePublication());
        annonceDto.setDescription(annonceFull.getAnnonce().getDescription());
        annonceDto.setTitre(annonceFull.getAnnonce().getTitre());
        annonceDto.setPrix(annonceFull.getAnnonce().getPrix());
        annonceDto.setUuid(annonceFull.getAnnonce().getUUID());

        List<String> listPhotoDto = new ArrayList<>();
        for (PhotoEntity photo : annonceFull.getPhotos()) {
            listPhotoDto.add(photo.getFirebasePath());
        }
        annonceDto.setPhotos(listPhotoDto);

        return annonceDto;
    }
}