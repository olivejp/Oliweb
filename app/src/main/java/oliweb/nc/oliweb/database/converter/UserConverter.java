package oliweb.nc.oliweb.database.converter;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.dto.elasticsearch.UtilisateurDto;
import oliweb.nc.oliweb.dto.firebase.UtilisateurFirebase;
import oliweb.nc.oliweb.utility.Utility;


public class UserConverter {

    private UserConverter() {
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

    public static UserEntity convertDtoToEntity(UtilisateurDto utilisateurDto) {
        UserEntity userEntity = new UserEntity();
        userEntity.setTelephone(utilisateurDto.getTelephone());
        userEntity.setEmail(utilisateurDto.getEmail());
        userEntity.setPhotoUrl(utilisateurDto.getPhotoUrl());
        userEntity.setProfile(utilisateurDto.getProfile());
        userEntity.setUid(utilisateurDto.getUuid());
        return userEntity;
    }

    public static UserEntity convertFbToEntity(FirebaseUser firebaseUser, String token, boolean isCreation) {
        Long now = Utility.getNowInEntityFormat();
        UserEntity userEntity = new UserEntity();
        userEntity.setUid(firebaseUser.getUid());
        userEntity.setProfile(firebaseUser.getDisplayName());
        userEntity.setEmail(firebaseUser.getEmail());
        userEntity.setTokenDevice(token);
        userEntity.setDateLastConnexion(now);
        userEntity.setStatut(StatusRemote.TO_SEND);
        retrievePhotoUrl(firebaseUser, userEntity);
        if (isCreation) {
            userEntity.setDateCreation(now);
            userEntity.setTelephone(firebaseUser.getPhoneNumber());
        }
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

    /**
     * Authentication with Facebook force us to retrieve the profile photo differently
     *
     * @param firebaseUser
     * @param userEntity
     */
    private static void retrievePhotoUrl(FirebaseUser firebaseUser, UserEntity userEntity) {
        userEntity.setPhotoUrl((firebaseUser.getPhotoUrl() != null) ? firebaseUser.getPhotoUrl().toString() : null);
        for (UserInfo profile : firebaseUser.getProviderData()) {
            if (profile.getProviderId().equals("facebook.com")) {
                userEntity.setPhotoUrl("https://graph.facebook.com/" + profile.getUid() + "/picture?type=normal");
            }
        }
    }
}