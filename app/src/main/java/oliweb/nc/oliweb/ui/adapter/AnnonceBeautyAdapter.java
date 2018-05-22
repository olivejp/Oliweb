package oliweb.nc.oliweb.ui.adapter;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by orlanth23 on 07/02/2018.
 */
public class AnnonceBeautyAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = AnnonceBeautyAdapter.class.getName();

    private List<AnnoncePhotos> listAnnonces;
    private int backGroundColor;
    private View.OnClickListener onClickListener;
    private View.OnClickListener onClickListenerShare;
    private View.OnClickListener onClickListenerFavorite;

    public AnnonceBeautyAdapter(@ColorInt int backGroundColor,
                                View.OnClickListener onClickListener,
                                View.OnClickListener onClickListenerShare,
                                View.OnClickListener onClickListenerFavorite) {
        this.listAnnonces = new ArrayList<>();
        this.backGroundColor = backGroundColor;
        this.onClickListener = onClickListener;
        this.onClickListenerShare = onClickListenerShare;
        this.onClickListenerFavorite = onClickListenerFavorite;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemLayoutView = inflater.inflate(R.layout.adapter_annonce_beauty, parent, false);
        RecyclerView.ViewHolder viewHolderResult = new ViewHolderBeauty(itemLayoutView);
        ((ViewHolderBeauty) viewHolderResult).parent = parent;
        return viewHolderResult;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        AnnoncePhotos annoncePhotos = listAnnonces.get(position);
        bindViewHolderBeauty(viewHolder, annoncePhotos);
    }

    @Override
    public int getItemCount() {
        return listAnnonces.size();
    }

    @Override
    public int getItemViewType(int position) {
        AnnoncePhotos annoncePhotos = listAnnonces.get(position);
        if (annoncePhotos.getPhotos() == null || annoncePhotos.getPhotos().isEmpty() || (annoncePhotos.getPhotos().size() == 1)) {
            return 1;
        } else {
            return 2;
        }
    }

    private void bindViewHolderBeauty(RecyclerView.ViewHolder viewHolder, AnnoncePhotos annoncePhotos) {
        ViewHolderBeauty viewHolderBeauty = (ViewHolderBeauty) viewHolder;

        AnnonceEntity annonce = annoncePhotos.getAnnonceEntity();
        viewHolderBeauty.annoncePhotos = annoncePhotos;

        // Transition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            viewHolderBeauty.imageView.setTransitionName(annonce.getUid());
            viewHolderBeauty.textTitreAnnonce.setTransitionName(annonce.getTitre());
        }

        viewHolderBeauty.cardView.setTag(viewHolderBeauty);
        viewHolderBeauty.imageFavorite.setTag(viewHolderBeauty);
        viewHolderBeauty.imageShare.setTag(viewHolderBeauty);

        viewHolderBeauty.cardView.setOnClickListener(onClickListener);
        viewHolderBeauty.imageFavorite.setOnClickListener(onClickListenerFavorite);
        viewHolderBeauty.imageShare.setOnClickListener(onClickListenerShare);

        if (viewHolderBeauty.annoncePhotos.getAnnonceEntity().isFavorite()) {
            viewHolderBeauty.imageFavorite.setImageResource(R.drawable.ic_favorite_red_700_48dp);
        }

        // Récupération de la date de publication
        viewHolderBeauty.textDatePublicationAnnonce.setText(Utility.howLongFromNow(viewHolderBeauty.annoncePhotos.getAnnonceEntity().getDatePublication()));

        viewHolderBeauty.textTitreAnnonce.setText(annonce.getTitre());
        viewHolderBeauty.textPrixAnnonce.setText(String.valueOf(String.format(Locale.FRANCE, "%,d", annonce.getPrix()) + " XPF"));

        if (annoncePhotos.getPhotos() != null && !annoncePhotos.getPhotos().isEmpty()) {
            viewHolderBeauty.imageView.setBackground(null);
            viewHolderBeauty.imageView.setVisibility(View.INVISIBLE);
            GlideApp.with(viewHolderBeauty.imageView)
                    .load(annoncePhotos.getPhotos().get(0).getFirebasePath())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            viewHolderBeauty.imageView.setVisibility(View.VISIBLE);
                            viewHolderBeauty.progressBar.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            viewHolderBeauty.imageView.setVisibility(View.VISIBLE);
                            viewHolderBeauty.progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .error(R.drawable.ic_error_white_48dp)
                    .centerCrop()
                    .into(viewHolderBeauty.imageView);
        } else {
            viewHolderBeauty.imageView.setBackgroundColor(backGroundColor);
            GlideApp.with(viewHolderBeauty.imageView).clear(viewHolderBeauty.imageView);
        }
    }

    public void setListAnnonces(final List<AnnoncePhotos> newListAnnonces) {
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
                    return listAnnonces.get(oldItemPosition).getAnnonceEntity().getUid().equals(newListAnnonces.get(newItemPosition).getAnnonceEntity().getUid());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    AnnonceEntity newAnnonce = newListAnnonces.get(newItemPosition).getAnnonceEntity();
                    AnnonceEntity oldAnnonce = listAnnonces.get(oldItemPosition).getAnnonceEntity();
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

    public class ViewHolderBeauty extends RecyclerView.ViewHolder {

        @BindView(R.id.text_titre_annonce)
        TextView textTitreAnnonce;

        @BindView(R.id.text_prix_annonce)
        TextView textPrixAnnonce;

        @BindView(R.id.text_date_publication_annonce)
        TextView textDatePublicationAnnonce;

        @BindView(R.id.card_view)
        CardView cardView;

        @BindView(R.id.image_favorite)
        ImageView imageFavorite;

        @BindView(R.id.image_share)
        ImageView imageShare;

        @BindView(R.id.image_view_beauty)
        ImageView imageView;

        @BindView(R.id.loading_progress)
        ProgressBar progressBar;


        AnnoncePhotos annoncePhotos;

        ViewGroup parent;

        ViewHolderBeauty(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
        }

        public ImageView getImageView() {
            return imageView;
        }

        public AnnoncePhotos getAnnoncePhotos() {
            return annoncePhotos;
        }

        public ViewGroup getParent() {
            return parent;
        }
    }
}
