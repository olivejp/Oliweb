package oliweb.nc.oliweb.service.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ChatSyncService extends Service {

    public static final String UID_USER = "PROFIL_ACTIVITY_UID_USER";

    public ChatSyncService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
}
