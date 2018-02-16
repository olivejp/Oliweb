package oliweb.nc.oliweb;

public class Constants {
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    public static final int ID_ALL_CAT = 999;
    public static final String PARAM_VIS = "VIS";
    public static final String PARAM_MAJ = "MAJ";
    public static final String PARAM_CRE = "CRE";
    public static final String PROTOCOL_HTTP = "http://";
    public static final String PROTOCOL_HTTPS = "https://";
    public static final int MAX_SIZE = 1024;
    static final String CURRENCY = "xpf";

    /**
     * Minutes we will wait before launch the sync
     */
    public static final long PERIODIC_SYNC_JOB_MINS = 15;

    /**
     * How close to the end of the period the job should run
     */
    public static final long INTERVAL_SYNC_JOB_MINS = 5;

}