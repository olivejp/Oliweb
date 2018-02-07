package oliweb.nc.oliweb.ui.adapter;

/**
 * Created by 2761oli on 21/12/2017.
 */

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.glide.GlideApp;

public class AnnonceViewPagerAdapter extends PagerAdapter {

    private ArrayList<String> images;
    private LayoutInflater inflater;
    private Context mContext;

    AnnonceViewPagerAdapter(Context context, List<String> images) {
        this.images = (ArrayList<String>) images;
        this.mContext = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup view, int position) {
        View myImageLayout = inflater.inflate(R.layout.annonce_slide_view_pager, view, false);
        ImageView myImage = myImageLayout.findViewById(R.id.image);

        GlideApp.with(mContext)
                .load(images.get(position))
                .into(myImage);

        view.addView(myImageLayout, 0);
        return myImageLayout;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view.equals(object);
    }
}