package oliweb.nc.oliweb.ui.task;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.bumptech.glide.request.RequestOptions;

import java.util.concurrent.ExecutionException;

import oliweb.nc.oliweb.ui.glide.GlideApp;

/**
 * Created by orlanth23 on 01/02/2018.
 */

public class CatchPhotoFromUrlTask extends AsyncTask<Uri, Void, Drawable> {

    private static final String TAG = CatchPhotoFromUrlTask.class.getName();

    private TaskListener<Drawable> listener;

    private Context context;

    public CatchPhotoFromUrlTask() {
    }

    public void setListener(TaskListener<Drawable> listener) {
        this.listener = listener;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    protected Drawable doInBackground(Uri... uris) {
        try {
            if (context != null) {
                return GlideApp.with(context)
                        .asDrawable()
                        .load(uris[0])
                        .apply(RequestOptions.circleCropTransform())
                        .submit()
                        .get();
            }
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Drawable drawable) {
        if (context != null && listener != null) {
            listener.onSuccess(drawable);
        }
    }
}
