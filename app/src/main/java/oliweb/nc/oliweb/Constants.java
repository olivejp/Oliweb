package oliweb.nc.oliweb;

public class Constants {

    public static final String PARAM_VIS = "VIS";
    public static final String PARAM_MAJ = "MAJ";
    public static final String PARAM_CRE = "CRE";
    public static final int MAX_SIZE = 512;

    public static final int PER_PAGE_REQUEST = 20;

    public static final String FIREBASE_DB_ANNONCE_REF = "annonces";
    public static final String FIREBASE_DB_USER_REF = "users";
    public static final String FIREBASE_DB_REQUEST_REF = "requests";
    public static final String FIREBASE_DB_CHATS_REF = "chats";
    public static final String FIREBASE_DB_MESSAGES_REF = "messages";
    public static final String FIREBASE_DB_PHOTO_REF = "photos";
    public static final String FIREBASE_STORAGE_PHOTO = "photos";
    public static final String IMAGE_DIRECTORY_NAME = "OliwebNcImageUpload";
    public static final String CHANNEL_ID = "OLIWEB_CHANNEL_ID";
    public static final String DANCING_FONT = "fonts/dancing-script.regular.ttf";

    public static int notificationSyncPhotoId = 123456;
    public static int notificationSyncAnnonceId = 654321;

    /**
     * Minutes we will wait before launch the sync
     */
    public static final long PERIODIC_SYNC_JOB_MINS = 15;

    /**
     * How close to the end of the period the job should run
     */
    public static final long INTERVAL_SYNC_JOB_MINS = 5;

}