package oliweb.nc.oliweb.utility;

public class Constants {

    public static final String OLIWEB_DATABASE = "oliweb-database";

    public static final String PARAM_MAJ = "MAJ";
    public static final String PARAM_CRE = "CRE";
    public static final int DEFAULT_MAX_IMG_PIXEL_SIZE = 600;
    public static final Long DEFAULT_NUMBER_PICTURES = 10L;

    public static final int PER_PAGE_REQUEST = 10;
    public static final String EMAIL_ADMIN = "orlanth23@gmail.com";

    static final String FIREBASE_DB_TIME_REF = "timestamp";
    public static final String FIREBASE_DB_ANNONCE_REF = "annonces";
    public static final String FIREBASE_DB_USER_REF = "users";
    public static final String FIREBASE_DB_REQUEST_REF = "requests";
    public static final String FIREBASE_DB_CHATS_REF = "chats";
    public static final String FIREBASE_DB_MESSAGES_REF = "messages";
    public static final String IMAGE_DIRECTORY_NAME = "OliwebNcImageUpload";
    public static final String CHANNEL_ID = "OLIWEB_CHANNEL_ID";
    public static final String OLIWEB_SITE = "http://oliweb-ec245.firebaseapp.com";
    public static final String OLIWEB_ANNONCE_VIEW_PATH = "/annonces/view/";
    public static final String OLIWEB_DYNAMIC_LINK_DOMAIN = "g2gb6.app.goo.gl";
    public static final String MAIL_MESSAGE_TYPE = "message/rfc822";

    public static final int NOTIFICATION_SYNC_ANNONCE_ID = 654321;

    /**
     * Minutes we will wait before launch the sync
     */
    public static final long PERIODIC_SYNC_JOB_MINS = 15;

    /**
     * How close to the end of the period the job should run
     */
    public static final long INTERVAL_SYNC_JOB_MINS = 5;

    /**
     * All the remote parameters
     */
    public static final String REMOTE_COLUMN_NUMBER = "column_number";
    public static final String REMOTE_COLUMN_NUMBER_LANDSCAPE = "column_number_landscape";
    public static final String REMOTE_IMAGE_RESOLUTION_RESIZE = "image_resolution_resize";
    public static final String REMOTE_NUMBER_PICTURES = "remote_number_pictures";
    public static final String REMOTE_DELAY = "remote_delay";
    public static final Long REMOTE_DELAY_DEFAULT = 20L;


    public static final String OLIWEB_CHANNEL = "OLIWEB Channel";

    private Constants() {
    }
}