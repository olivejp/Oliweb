package oliweb.nc.oliweb.database.converter;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.dto.firebase.UserFirebase;
import oliweb.nc.oliweb.utility.Utility;


public class UserConverter {

    private UserConverter() {
    }

    public static UserEntity convertDtoToEntity(UserFirebase userFirebase) {
        UserEntity userEntity = new UserEntity();
        userEntity.setTelephone(userFirebase.getTelephone());
        userEntity.setEmail(userFirebase.getEmail());
        userEntity.setPhotoUrl(userFirebase.getPhotoUrl());
        userEntity.setProfile(userFirebase.getProfile());
        userEntity.setUid(userFirebase.getUuid());
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