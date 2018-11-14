package oliweb.nc.oliweb.ui.adapter;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.ui.glide.GlideApp;

/**
 * Created by orlanth23 on 14/11/2018.
 */
public class AnnonceMiniAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = AnnonceMiniAdapter.class.getName();

    private List<AnnonceFull> listAnnonces;
    private View.OnClickListener onClickListener;

    public AnnonceMiniAdapter(View.OnClickListener onClickListener) {
        this.listAnnonces = new ArrayList<>();
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemLayoutView = inflater.inflate(R.layout.adapter_annonce_mini, parent, false);
        return new ViewHolderMini(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        AnnonceFull annonceFull = listAnnonces.get(position);
        bindViewHolderBeauty(viewHolder, annonceFull);
    }

    @Override
    public int getItemCount() {
        return listAnnonces.size();
    }

    private void bindViewHolderBeauty(RecyclerView.ViewHolder viewHolder, AnnonceFull annonceFull) {
        ViewHolderMini viewHolderMini = (ViewHolderMini) viewHolder;

        AnnonceEntity annonce = annonceFull.getAnnonce();
        viewHolderMini.annonceFull = annonceFull;
        viewHolderMini.cardView.setTag(annonceFull);
        viewHolderMini.cardView.setOnClickListener(onClickListener);

        if (onClickListener != null) viewHolderMini.cardView.setOnClickListener(onClickListener);

        viewHolderMini.textTitreAnnonce.setText(annonce.getTitre());
        viewHolderMini.textPrixAnnonce.setText(String.valueOf(String.format(Locale.FRANCE, "%,d", annonce.getPrix()) + " xpf").trim());

        if (annonceFull.getPhotos() != null && !annonceFull.getPhotos().isEmpty()) {
            viewHolderMini.imageView.setBackground(null);
            viewHolderMini.imageView.setVisibility(View.INVISIBLE);
            GlideApp.with(viewHolderMini.imageView)
                    .load(annonceFull.getPhotos().get(0).getFirebasePath())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            viewHolderMini.imageView.setVisibility(View.VISIBLE);
                            viewHolderMini.progressBar.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            viewHolderMini.imageView.setVisibility(View.VISIBLE);
                            viewHolderMini.progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .error(R.mipmap.ic_banana_launcher_foreground)
                    .centerCrop()
                    .into(viewHolderMini.imageView);
        } else {
            viewHolderMini.progressBar.setVisibility(View.GONE);
            GlideApp.with(viewHolderMini.imageView)
                    .load(R.mipmap.ic_banana_launcher_foreground)
                    .centerInside()
                    .into(viewHolderMini.imageView);
        }
    }

    public void setListAnnonces(final List<AnnonceFull> newListAnnonces) {
        if (listAnnonces == null) {
            listAnnonces = newListAnnonces;
            notifyItemRangeInserted(0, newListAnnonces.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return listAnnonces.size();
                }

                @Override
                public int getNewListSize() {
                    return newListAnnonces.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return listAnnonces.get(oldItemPosition).getAnnonce().getUid().equals(newListAnnonces.get(newItemPosition).getAnnonce().getUid());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    AnnonceEntity newAnnonce = newListAnnonces.get(newItemPosition).getAnnonce();
                    AnnonceEntity oldAnnonce = listAnnonces.get(oldItemPosition).getAnnonce();
                    return newAnnonce.getUid().equals(oldAnnonce.getUid())
                            && newAnnonce.getTitre().equals(oldAnnonce.getTitre())
                            && newAnnonce.getDescription().equals(oldAnnonce.getDescription())
                            && (newAnnonce.isFavorite() == oldAnnonce.isFavorite())
                            && newAnnonce.getPrix().equals(oldAnnonce.getPrix());
                }
            });
            this.listAnnonces = newListAnnonces;
            result.dispatchUpdatesTo(this);
        }
    }

    public class ViewHolderMini extends RecyclerView.ViewHolder {

        @BindView(R.id.text_titre_annonce)
        TextView textTitreAnnonce;

        @BindView(R.id.text_prix_annonce)
        TextView textPrixAnnonce;

        @BindView(R.id.card_view)
        FrameLayout cardView;

        @BindView(R.id.image_view_beauty)
        ImageView imageView;

        @BindView(R.id.loading_progress)
        ProgressBar progressBar;

        AnnonceFull annonceFull;

        ViewHolderMini(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
        }

        public AnnonceFull getAnnonceFull() {
            return annonceFull;
        }
    }
}
