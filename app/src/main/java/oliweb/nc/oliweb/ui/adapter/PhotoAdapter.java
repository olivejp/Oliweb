package oliweb.nc.oliweb.ui.adapter;

import android.content.Context;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.ui.glide.GlideApp;

/**
 * Created by orlanth23 on 03/02/2018.
 */

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolderPhotoAdapter> {

    private List<PhotoEntity> listPhotos;
    private Context context;

    public PhotoAdapter(Context context, View.OnClickListener onClickListener) {
        this.context = context;
        this.listPhotos = new ArrayList<>();
    }

    @Override
    public ViewHolderPhotoAdapter onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.photo_adapter, parent, false);
        return new ViewHolderPhotoAdapter(rootView);
    }

    @Override
    public void onBindViewHolder(final ViewHolderPhotoAdapter holder, int position) {
        holder.photoEntity = listPhotos.get(position);
        holder.imagePhoto.setTag(holder.photoEntity);
        GlideApp.with(context)
                .asDrawable()
                .apply(RequestOptions.circleCropTransform())
                .load(holder.photoEntity.getCheminLocal())
                .into(holder.imagePhoto);
    }

    @Override
    public int getItemCount() {
        return listPhotos == null ? 0 : listPhotos.size();
    }

    public void setListPhotos(final List<PhotoEntity> newListPhotos) {
        if (listPhotos == null) {
            listPhotos = newListPhotos;
            notifyItemRangeInserted(0, newListPhotos.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return listPhotos.size();
                }

                @Override
                public int getNewListSize() {
                    return newListPhotos.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return listPhotos.get(oldItemPosition).getCheminLocal().equals(newListPhotos.get(newItemPosition).getCheminLocal());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    PhotoEntity newPhoto = newListPhotos.get(newItemPosition);
                    PhotoEntity oldPhoto = listPhotos.get(oldItemPosition);
                    return newPhoto.getCheminLocal().equals(oldPhoto.getCheminLocal())
                            && newPhoto.getStatut().equals(oldPhoto.getStatut());
                }
            });
            this.listPhotos = newListPhotos;
            result.dispatchUpdatesTo(this);
        }
    }

    public class ViewHolderPhotoAdapter extends RecyclerView.ViewHolder {
        PhotoEntity photoEntity;

        @BindView(R.id.image_photo)
        ImageView imagePhoto;
        ViewHolderPhotoAdapter(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
