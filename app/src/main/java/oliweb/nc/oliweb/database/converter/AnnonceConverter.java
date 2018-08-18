package oliweb.nc.oliweb.database.converter;

import java.util.ArrayList;
import java.util.List;

import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.network.elasticsearchDto.CategorieDto;
import oliweb.nc.oliweb.network.elasticsearchDto.UtilisateurDto;


public class AnnonceConverter {

    private static final String TAG = AnnonceConverter.class.getName();

    private AnnonceConverter() {
    }

    /**
     * @param annonceDto
     * @return
     */
    public static AnnoncePhotos convertDtoToAnnoncePhotos(AnnonceDto annonceDto) {
        AnnoncePhotos annoncePhotos = new AnnoncePhotos();
        annoncePhotos.setPhotos(new ArrayList<>());
        AnnonceEntity annonceEntity = convertDtoToEntity(annonceDto);
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
    public static AnnonceDto convertFullEntityToDto(AnnonceFull annonceFull) {
        AnnonceDto annonceDto = new AnnonceDto();
        UserEntity userEntity = annonceFull.getUtilisateur().get(0);
        UtilisateurDto utilisateurDto = new UtilisateurDto(userEntity.getProfile(), userEntity.getUid(), userEntity.getTelephone(), userEntity.getEmail());
        annonceDto.setUtilisateur(utilisateurDto);

        CategorieEntity categorieEntity = annonceFull.getCategorie().get(0);
        annonceDto.setCategorie(new CategorieDto(categorieEntity.getId(), categorieEntity.getName()));

        if (annonceFull.getAnnonce().getDatePublication() != null) {
            annonceDto.setDatePublication(annonceFull.getAnnonce().getDatePublication());
        }

        annonceDto.setDescription(annonceFull.getAnnonce().getDescription());
        annonceDto.setTitre(annonceFull.getAnnonce().getTitre());
        annonceDto.setPrix(annonceFull.getAnnonce().getPrix());
        annonceDto.setUuid(annonceFull.getAnnonce().getUid());

        annonceDto.setContactEmail(annonceFull.getAnnonce().getContactByEmail() != null && annonceFull.getAnnonce().getContactByEmail().equals("O"));
        annonceDto.setContactTel(annonceFull.getAnnonce().getContactByTel() != null && annonceFull.getAnnonce().getContactByTel().equals("O"));
        annonceDto.setContactMsg(annonceFull.getAnnonce().getContactByMsg() != null && annonceFull.getAnnonce().getContactByMsg().equals("O"));

        List<String> listPhotoDto = new ArrayList<>();
        for (PhotoEntity photo : annonceFull.getPhotos()) {
            if (photo.getFirebasePath() != null && !photo.getFirebasePath().isEmpty()) {
                listPhotoDto.add(photo.getFirebasePath());
            }
        }
        annonceDto.setPhotos(listPhotoDto);

        return annonceDto;
    }

    public static AnnonceEntity convertDtoToEntity(AnnonceDto annonceDto) {
        if (annonceDto != null) {
            AnnonceEntity annonceEntity = new AnnonceEntity();
            annonceEntity.setUid(annonceDto.getUuid());
            annonceEntity.setTitre(annonceDto.getTitre());
            annonceEntity.setDescription(annonceDto.getDescription());
            annonceEntity.setDatePublication(annonceDto.getDatePublication());
            annonceEntity.setPrix(annonceDto.getPrix());
            annonceEntity.setStatut(StatusRemote.SEND);
            annonceEntity.setContactByMsg(annonceDto.isContactMsg() ? "O" : "N");
            annonceEntity.setContactByTel(annonceDto.isContactTel() ? "O" : "N");
            annonceEntity.setContactByEmail(annonceDto.isContactEmail() ? "O" : "N");
            annonceEntity.setFavorite(0);

            if (annonceDto.getCategorie() != null) {
                annonceEntity.setIdCategorie(annonceDto.getCategorie().getId());
            }

            if (annonceDto.getUtilisateur() != null) {
                annonceEntity.setUidUser(annonceDto.getUtilisateur().getUuid());
            }

            return annonceEntity;
        }
        return null;
    }
}
