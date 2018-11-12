package oliweb.nc.oliweb.ui.adapter;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.glide.GlideApp;

/**
 * Created by 2761oli on 12/11/2018.
 */
public class ShootingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Pair<Uri, File>> listPairs;
    private View.OnLongClickListener onLongClickListener;

    public ShootingAdapter(View.OnLongClickListener onLongClickListener) {
        this.listPairs = new ArrayList<>();
        this.onLongClickListener = onLongClickListener;
    }

    @NonNull
    @Override
    public ShootingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View rootView = inflater.inflate(R.layout.adapter_shooting, parent, false);
        return new ShootingViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        ShootingViewHolder holder = (ShootingViewHolder) viewHolder;
        Pair<Uri, File> model = listPairs.get(position);

        // Ajout d'un long click pour supprimer une image
        holder.constraintShooting.setTag(model.first);
        holder.constraintShooting.setOnLongClickListener(onLongClickListener);

        // Tentative de téléchargement de la photo.
        GlideApp.with(holder.imageShooting)
                .load(model.first)
                .centerCrop()
                .listener(getListener(holder))
                .error(R.drawable.ic_photo_camera_grey_900_48dp)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imageShooting);
    }

    // Permet de faire apparaitre la progress bar tant que l'image n'est pas disponible.
    private RequestListener<Drawable> getListener(ShootingViewHolder holder) {
        return new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                holder.imageShooting.setVisibility(View.VISIBLE);
                holder.shootingProgress.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                holder.imageShooting.setVisibility(View.VISIBLE);
                holder.shootingProgress.setVisibility(View.GONE);
                return false;
            }
        };
    }

    public void setListPairs(final List<Pair<Uri, File>> list) {
        listPairs = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return listPairs.size();
    }

    public static class ShootingViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.shooting_image)
        ImageView imageShooting;

        @BindView(R.id.shooting_progress)
        ProgressBar shootingProgress;

        @BindView(R.id.constraint_shooting)
        ConstraintLayout constraintShooting;

        ShootingViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
