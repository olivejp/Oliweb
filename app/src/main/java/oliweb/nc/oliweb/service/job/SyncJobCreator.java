package oliweb.nc.oliweb.service.job;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Created by 2761oli on 10/10/2017.
 */

public class SyncJobCreator implements JobCreator {
    @Override
    @Nullable
    public Job create(@NonNull String tag) {
        if (tag.equals(SyncJob.SYNC_JOB)) {
            return new SyncJob();
        } else {
            return null;
        }
    }
}
