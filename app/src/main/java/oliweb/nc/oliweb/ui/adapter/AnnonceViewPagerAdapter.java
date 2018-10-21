package oliweb.nc.oliweb.ui.adapter;

/**
 * Created by 2761oli on 21/12/2017.
 */

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.ui.glide.GlideApp;

public class AnnonceViewPagerAdapter extends PagerAdapter {

    private List<PhotoEntity> photos;
    private LayoutInflater inflater;

    public AnnonceViewPagerAdapter(Context context, List<PhotoEntity> photos) {
        this.photos = photos;
        this.inflater = LayoutInflater.from(context);
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
        ImageView myImage = myImageLayout.findViewById(R.id.image);

        if (photos.get(position).getUriLocal() != null) {
            myImage.setImageURI(Uri.parse(photos.get(position).getUriLocal()));
        } else {
            if (photos.get(position).getFirebasePath() != null) {
                GlideApp.with(myImage.getContext())
                        .load(photos.get(position).getFirebasePath())
                        .error(R.drawable.ic_error_grey_900_48dp)
                        .centerCrop()
                        .into(myImage);
            }
        }

        view.addView(myImageLayout, 0);
        return myImageLayout;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view.equals(object);
    }
}