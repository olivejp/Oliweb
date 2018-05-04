package oliweb.nc.oliweb.database.converter;

import com.google.firebase.auth.FirebaseUser;

import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;
import oliweb.nc.oliweb.utility.Utility;


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

    public static UtilisateurEntity convertFbToEntity(FirebaseUser firebaseUser) {
        UtilisateurEntity utilisateurEntity = new UtilisateurEntity();
        utilisateurEntity.setUid(firebaseUser.getUid());
        utilisateurEntity.setProfile(firebaseUser.getDisplayName());
        utilisateurEntity.setDateCreation(Utility.getNowInEntityFormat());
        utilisateurEntity.setEmail(firebaseUser.getEmail());
        utilisateurEntity.setTelephone(firebaseUser.getPhoneNumber());
        utilisateurEntity.setPhotoUrl((firebaseUser.getPhotoUrl() != null) ? firebaseUser.getPhotoUrl().toString() : null);
        return utilisateurEntity;
    }

    public static UtilisateurFirebase convertFbUserToUtilisateurFirebase(FirebaseUser firebaseUser, String userToken) {
        UtilisateurFirebase utilisateurFirebase = new UtilisateurFirebase();
        if (firebaseUser.getPhotoUrl() != null && !firebaseUser.getPhotoUrl().toString().isEmpty()) {
            utilisateurFirebase.setPhotoPath(firebaseUser.getPhotoUrl().toString());
        }
        utilisateurFirebase.setProfileName(firebaseUser.getDisplayName());
        utilisateurFirebase.setEmail(firebaseUser.getEmail());
        utilisateurFirebase.setTokenDevice(userToken);
        return utilisateurFirebase;
    }
}