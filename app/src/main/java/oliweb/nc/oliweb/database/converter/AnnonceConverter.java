package oliweb.nc.oliweb.database.converter;

import java.util.ArrayList;
import java.util.List;

import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceSearchDto;
import oliweb.nc.oliweb.network.elasticsearchDto.CategorieSearchDto;
import oliweb.nc.oliweb.network.elasticsearchDto.UtilisateurSearchDto;


public class AnnonceConverter {

    private AnnonceConverter() {
    }

    /**
     * @param annonceSearchDto
     * @return
     */
    public static AnnoncePhotos convertDtoToEntity(AnnonceSearchDto annonceSearchDto) {
        AnnoncePhotos annoncePhotos = new AnnoncePhotos();
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annoncePhotos.setPhotos(new ArrayList<>());

        annonceEntity.setUUID(annonceSearchDto.getUuid());
        annonceEntity.setTitre(annonceSearchDto.getTitre());
        annonceEntity.setDescription(annonceSearchDto.getDescription());
        // annonceEntity.setIdCategorie(annonceSearchDto.getCategorie().getId());
        annonceEntity.setDatePublication(annonceSearchDto.getDatePublication());
        annonceEntity.setPrix(annonceSearchDto.getPrix());

        if (annonceSearchDto.getPhotos() != null && !annonceSearchDto.getPhotos().isEmpty()) {
            for (String photoUrl : annonceSearchDto.getPhotos()) {
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
    public static AnnonceSearchDto convertEntityToDto(AnnonceFull annonceFull) {
        AnnonceSearchDto annonceSearchDto = new AnnonceSearchDto();
        UtilisateurEntity utilisateurEntity = annonceFull.getUtilisateur().get(0);
        UtilisateurSearchDto utilisateurSearchDto = new UtilisateurSearchDto(utilisateurEntity.getProfile(), utilisateurEntity.getUuidUtilisateur(), utilisateurEntity.getTelephone(), utilisateurEntity.getEmail());
        annonceSearchDto.setUtilisateur(utilisateurSearchDto);

        CategorieEntity categorieEntity = annonceFull.getCategorie().get(0);
        annonceSearchDto.setCategorie(new CategorieSearchDto(categorieEntity.getIdCategorie(), categorieEntity.getName()));

        annonceSearchDto.setDatePublication(annonceFull.getAnnonce().getDatePublication());
        annonceSearchDto.setDescription(annonceFull.getAnnonce().getDescription());
        annonceSearchDto.setTitre(annonceFull.getAnnonce().getTitre());
        annonceSearchDto.setPrix(annonceFull.getAnnonce().getPrix());
        annonceSearchDto.setUuid(annonceFull.getAnnonce().getUUID());

        List<String> listPhotoDto = new ArrayList<>();
        for (PhotoEntity photo : annonceFull.getPhotos()) {
            listPhotoDto.add(photo.getFirebasePath());
        }
        annonceSearchDto.setPhotos(listPhotoDto);

        return annonceSearchDto;
    }
}
