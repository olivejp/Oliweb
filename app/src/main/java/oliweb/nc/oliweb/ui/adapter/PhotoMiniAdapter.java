package oliweb.nc.oliweb.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.ui.glide.GlideApp;

/**
 * Created by orlanth23 on 26/11/2018.
 */
public class PhotoMiniAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = PhotoMiniAdapter.class.getName();

    private Context context;
    private List<PhotoEntity> listPhotos;

    PhotoMiniAdapter(Context context) {
        this.context = context;
        this.listPhotos = new ArrayList<>();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemLayoutView = inflater.inflate(R.layout.adapter_photo_mini, parent, false);
        return new VHPhotoMini(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        PhotoEntity photoEntity = listPhotos.get(position);

        VHPhotoMini vhPhotoMini = (VHPhotoMini) viewHolder;

        boolean isSending = photoEntity.getStatut().equals(StatusRemote.SENDING);
        vhPhotoMini.imageView.setVisibility((isSending)? View.GONE : View.VISIBLE);

        if (!isSending) {
            String pathImage = null;
            if (photoEntity.getUriLocal() != null) {
                pathImage = photoEntity.getUriLocal();
            } else if (photoEntity.getFirebasePath() != null) {
                pathImage = photoEntity.getFirebasePath();
            }

            if (pathImage != null) {
                GlideApp.with(context)
                        .load(pathImage)
                        .override(80)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(vhPhotoMini.imageView);
            }
        }
    }

    @Override
    public int getItemCount() {
        return listPhotos.size();
    }

    void setListPhotos(final List<PhotoEntity> newListAnnonces) {
        if (listPhotos == null) {
            listPhotos = newListAnnonces;
            notifyItemRangeInserted(0, newListAnnonces.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return listPhotos.size();
                }

                @Override
                public int getNewListSize() {
                    return newListAnnonces.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return listPhotos.get(oldItemPosition).getId().equals(newListAnnonces.get(newItemPosition).getId());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    PhotoEntity newPhoto = newListAnnonces.get(newItemPosition);
                    PhotoEntity oldPhoto = listPhotos.get(oldItemPosition);
                    return newPhoto.getFirebasePath().equals(oldPhoto.getFirebasePath())
                            && newPhoto.getUriLocal().equals(oldPhoto.getUriLocal())
                            && newPhoto.getId().equals(oldPhoto.getId())
                            && (newPhoto.getIdAnnonce().equals(oldPhoto.getIdAnnonce()))
                            && newPhoto.getStatut().equals(oldPhoto.getStatut());
                }
            });
            this.listPhotos = newListAnnonces;
            result.dispatchUpdatesTo(this);
        }
    }

    class VHPhotoMini extends RecyclerView.ViewHolder {

        @BindView(R.id.image_photo_mini)
        ImageView imageView;

        VHPhotoMini(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
        }
    }
}
