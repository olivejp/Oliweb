package oliweb.nc.oliweb.service.job;

import androidx.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import java.util.concurrent.TimeUnit;

import oliweb.nc.oliweb.service.sync.SyncService;

import static oliweb.nc.oliweb.utility.Constants.INTERVAL_SYNC_JOB_MINS;
import static oliweb.nc.oliweb.utility.Constants.PERIODIC_SYNC_JOB_MINS;


/**
 * Created by 2761oli on 10/10/2017.
 */
public class SyncJob extends Job {

    static final String SYNC_JOB = "SYNC_JOB";

    @Override
    @NonNull
    protected Result onRunJob(@NonNull Params params) {
        // A job run on the background thread, so no need to call SyncTask here
        SyncService.launchSynchroFromScheduler(getContext());
        return Result.SUCCESS;
    }

    /**
     * Schedule a periodic job which will be launch every 30 minutes.
     */
    public static void scheduleJob() {
        new JobRequest.Builder(SyncJob.SYNC_JOB)
                .setRequiresDeviceIdle(false)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .setPeriodic(TimeUnit.MINUTES.toMillis(PERIODIC_SYNC_JOB_MINS), TimeUnit.MINUTES.toMillis(INTERVAL_SYNC_JOB_MINS))
                .build()
                .schedule();
    }

    /**
     * Launch immediately the sync job
     */
    public static void launchImmediateJob() {
        new JobRequest.Builder(SyncJob.SYNC_JOB)
                .startNow();
    }
}
