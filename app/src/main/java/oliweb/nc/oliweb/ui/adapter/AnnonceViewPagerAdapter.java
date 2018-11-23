package oliweb.nc.oliweb.ui.adapter;

/**
 * Created by 2761oli on 21/12/2017.
 */

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.ui.glide.GlideApp;

public class AnnonceViewPagerAdapter extends PagerAdapter {

    private List<PhotoEntity> photos;
    private LayoutInflater inflater;
    private View.OnClickListener onClickListener;
    private Context context;

    public AnnonceViewPagerAdapter(Context context, List<PhotoEntity> photos, @Nullable View.OnClickListener onClickListener) {
        this.context = context;
        this.photos = photos;
        this.inflater = LayoutInflater.from(context);
        this.onClickListener = onClickListener;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return photos.size();
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup view, int position) {
        View myImageLayout = inflater.inflate(R.layout.annonce_slide_view_pager, view, false);

        FrameLayout frameLayout = myImageLayout.findViewById(R.id.frame_slide_view_pager);
        ImageView myImage = myImageLayout.findViewById(R.id.image);
        ProgressBar progressBar = myImageLayout.findViewById(R.id.viewpager_loading_progress);

        String pathImage = null;

        if (photos.get(position).getUriLocal() != null) {
            pathImage = photos.get(position).getUriLocal();
        } else if (photos.get(position).getFirebasePath() != null) {
            pathImage = photos.get(position).getFirebasePath();
        }
        GlideApp.with(context)
                .load(pathImage)
                .error(R.drawable.ic_error_grey_900_48dp)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        myImage.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        myImage.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(myImage);

        frameLayout.setTag(pathImage);
        frameLayout.setOnClickListener(onClickListener);

        view.addView(myImageLayout, 0);
        return myImageLayout;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view.equals(object);
    }
}