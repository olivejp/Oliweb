package oliweb.nc.oliweb.ui.adapter;

import android.graphics.drawable.Drawable;
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

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.ui.glide.GlideApp;

/**
 * Created by 2761oli on 12/11/2018.
 */
public class PostPhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<PhotoEntity> listPhotoEntity;
    private View.OnLongClickListener onLongClickListener;
    private View.OnClickListener onClickListener;

    public PostPhotoAdapter(View.OnClickListener onClickListener, View.OnLongClickListener onLongClickListener) {
        this.listPhotoEntity = new ArrayList<>();
        this.onClickListener = onClickListener;
        this.onLongClickListener = onLongClickListener;
    }

    @NonNull
    @Override
    public PostPhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View rootView = inflater.inflate(R.layout.post_photo_adapter, parent, false);
        return new PostPhotoViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        PostPhotoViewHolder holder = (PostPhotoViewHolder) viewHolder;
        PhotoEntity model = listPhotoEntity.get(position);

        // On ajoute comme Tag, l'uri de la photo au widget image.
        holder.constraintLayout.setTag(model);

        // Binding sur les événements, si les listeners ne sont pas nuls
        if (onClickListener != null) {
            holder.constraintLayout.setOnClickListener(onClickListener);
        }
        if (onLongClickListener != null) {
            holder.constraintLayout.setOnLongClickListener(onLongClickListener);
        }

        // Tentative de téléchargement de la photo.
        GlideApp.with(holder.imageShooting)
                .load(model.getUriLocal())
                .centerCrop()
                .listener(getListener(holder))
                .error(R.drawable.ic_photo_camera_grey_900_48dp)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imageShooting);
    }

    // Permet de faire apparaitre la progress bar tant que l'image n'est pas disponible.
    private RequestListener<Drawable> getListener(PostPhotoViewHolder holder) {
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

    public void setListPhotoEntity(final List<PhotoEntity> list) {
        listPhotoEntity = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return listPhotoEntity.size();
    }

    public static class PostPhotoViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.shooting_image)
        ImageView imageShooting;

        @BindView(R.id.shooting_progress)
        ProgressBar shootingProgress;

        @BindView(R.id.constraint_shooting)
        ConstraintLayout constraintLayout;

        PostPhotoViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
