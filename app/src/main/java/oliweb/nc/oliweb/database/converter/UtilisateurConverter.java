package oliweb.nc.oliweb.database.converter;

import com.google.firebase.auth.FirebaseUser;

import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.dto.firebase.UtilisateurFirebase;
import oliweb.nc.oliweb.utility.Utility;


public class UtilisateurConverter {

    private UtilisateurConverter() {
    }

    public static UtilisateurFirebase convertEntityToFb(UserEntity userEntity) {
        UtilisateurFirebase utilisateurFirebase = new UtilisateurFirebase();
        utilisateurFirebase.setTelephone(userEntity.getTelephone());
        utilisateurFirebase.setEmail(userEntity.getEmail());
        utilisateurFirebase.setPhotoPath(userEntity.getPhotoUrl());
        utilisateurFirebase.setProfileName(userEntity.getProfile());
        utilisateurFirebase.setTokenDevice(userEntity.getTokenDevice());
        return utilisateurFirebase;
    }

    public static UserEntity convertFbToEntity(FirebaseUser firebaseUser) {
        UserEntity userEntity = new UserEntity();
        userEntity.setUid(firebaseUser.getUid());
        userEntity.setProfile(firebaseUser.getDisplayName());
        userEntity.setDateCreation(Utility.getNowInEntityFormat());
        userEntity.setEmail(firebaseUser.getEmail());
        userEntity.setTelephone(firebaseUser.getPhoneNumber());
        userEntity.setPhotoUrl((firebaseUser.getPhotoUrl() != null) ? firebaseUser.getPhotoUrl().toString() : null);
        return userEntity;
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