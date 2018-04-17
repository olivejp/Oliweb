package oliweb.nc.oliweb.database.converter;

import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;


public class UtilisateurConverter {

    private UtilisateurConverter() {
    }

    public static UtilisateurFirebase convertEntityToFb(UtilisateurEntity utilisateurEntity) {
        UtilisateurFirebase utilisateurFirebase = new UtilisateurFirebase();
        utilisateurFirebase.setTelephone(utilisateurEntity.getTelephone());
        utilisateurFirebase.setEmail(utilisateurEntity.getEmail());
        utilisateurFirebase.setPhotoPath(utilisateurEntity.getPhotoUrl());
        utilisateurFirebase.setProfileName(utilisateurEntity.getProfile());
        utilisateurFirebase.setTokenDevice(utilisateurEntity.getTokenDevice());
        return utilisateurFirebase;
    }
}