package oliweb.nc.oliweb.ui.adapter;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.ui.glide.GlideRequests;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by orlanth23 on 07/02/2018.
 */
public class AnnonceBeautyAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = AnnonceBeautyAdapter.class.getName();

    private List<AnnonceFull> listAnnonces;
    private View.OnClickListener onClickListener;
    private View.OnClickListener onClickListenerShare;
    private View.OnClickListener onClickListenerFavorite;
    private GlideRequests glide;

    public AnnonceBeautyAdapter(View.OnClickListener onClickListener,
                                View.OnClickListener onClickListenerShare,
                                View.OnClickListener onClickListenerFavorite,
                                AppCompatActivity appCompatActivity,
                                Fragment fragment) {
        this.listAnnonces = new ArrayList<>();
        this.onClickListener = onClickListener;
        this.onClickListenerShare = onClickListenerShare;
        this.onClickListenerFavorite = onClickListenerFavorite;
        if (appCompatActivity != null) {
            glide = GlideApp.with(appCompatActivity);
        }
        if (fragment != null) {
            glide = GlideApp.with(fragment);
        }
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
        AnnonceFull annoncePhotos = listAnnonces.get(position);
        bindViewHolderBeauty(viewHolder, annoncePhotos);
    }

    @Override
    public int getItemCount() {
        return listAnnonces.size();
    }

    @Override
    public int getItemViewType(int position) {
        AnnonceFull annoncePhotos = listAnnonces.get(position);
        if (annoncePhotos.getPhotos() == null || annoncePhotos.getPhotos().isEmpty() || (annoncePhotos.getPhotos().size() == 1)) {
            return 1;
        } else {
            return 2;
        }
    }

    private void bindViewHolderBeauty(RecyclerView.ViewHolder viewHolder, AnnonceFull annoncePhotos) {
        ViewHolderBeauty viewHolderBeauty = (ViewHolderBeauty) viewHolder;

        // Chargement de l'image de l'auteur de l'annonce
        if (annoncePhotos.getUtilisateur() != null
                && annoncePhotos.getUtilisateur().get(0) != null
                && StringUtils.isNotBlank(annoncePhotos.getUtilisateur().get(0).getPhotoUrl())) {

            String urlPhoto = annoncePhotos.getUtilisateur().get(0).getPhotoUrl();

            glide.load(urlPhoto)
                    .override(100)
                    .error(R.drawable.ic_person_white_48dp)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .circleCrop()
                    .into(viewHolderBeauty.imageUserBeauty);
        } else {
            glide.load(R.drawable.ic_person_white_48dp).into(viewHolderBeauty.imageUserBeauty);
        }

        viewHolderBeauty.textNumberPhoto.setText(String.valueOf(annoncePhotos.photos.size()));

        AnnonceEntity annonce = annoncePhotos.getAnnonce();
        viewHolderBeauty.annonceFull = annoncePhotos;

        viewHolderBeauty.cardView.setTag(viewHolderBeauty);
        viewHolderBeauty.imageFavorite.setTag(viewHolderBeauty);
        viewHolderBeauty.imageShare.setTag(viewHolderBeauty);

        if (onClickListener != null) viewHolderBeauty.cardView.setOnClickListener(onClickListener);
        if (onClickListenerFavorite != null)
            viewHolderBeauty.imageFavorite.setOnClickListener(onClickListenerFavorite);
        if (onClickListenerShare != null)
            viewHolderBeauty.imageShare.setOnClickListener(onClickListenerShare);

        boolean isFavorite = viewHolderBeauty.annonceFull.getAnnonce().isFavorite();
        viewHolderBeauty.imageFavorite.setImageResource((isFavorite) ? R.drawable.ic_favorite_red_700_48dp : R.drawable.ic_favorite_border_white_48dp);

        viewHolderBeauty.textDatePublicationAnnonce.setText(Utility.howLongFromNow(viewHolderBeauty.annonceFull.getAnnonce().getDatePublication()));
        viewHolderBeauty.textTitreAnnonce.setText(annonce.getTitre());
        viewHolderBeauty.textPrixAnnonce.setText(String.valueOf(String.format(Locale.FRANCE, "%,d", annonce.getPrix()) + " xpf").trim());
        viewHolderBeauty.textTitreCategorie.setText(annoncePhotos.getCategorie().get(0).getName());

        if (annoncePhotos.getPhotos() != null && !annoncePhotos.getPhotos().isEmpty()) {
            viewHolderBeauty.imageView.setBackground(null);
            viewHolderBeauty.imageView.setVisibility(View.INVISIBLE);

            String urlPhoto = annoncePhotos.getPhotos().get(0).getFirebasePath();

            // Création d'une requestBuilder pour charger le thumbnail
            RequestBuilder<Drawable> requestThumbnail = glide.load(urlPhoto).override(100, 100).diskCacheStrategy(DiskCacheStrategy.RESOURCE);

            // Création de la requête pour télécharger l'image
            glide.load(urlPhoto)
                    .thumbnail(requestThumbnail)
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
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.mipmap.ic_banana_launcher_foreground)
                    .centerCrop()
                    .into(viewHolderBeauty.imageView);
        } else {
            viewHolderBeauty.progressBar.setVisibility(View.GONE);
            glide.load(R.mipmap.ic_banana_launcher_foreground)
                    .centerInside()
                    .into(viewHolderBeauty.imageView);
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

    public class ViewHolderBeauty extends RecyclerView.ViewHolder {

        @BindView(R.id.text_categorie_annonce)
        TextView textTitreCategorie;

        @BindView(R.id.text_titre_annonce)
        TextView textTitreAnnonce;

        @BindView(R.id.text_prix_annonce)
        TextView textPrixAnnonce;

        @BindView(R.id.text_date_publication_annonce)
        TextView textDatePublicationAnnonce;

        @BindView(R.id.image_photo_number_text)
        TextView textNumberPhoto;

        @BindView(R.id.card_view)
        FrameLayout cardView;

        @BindView(R.id.image_favorite)
        ImageView imageFavorite;

        @BindView(R.id.image_share)
        ImageView imageShare;

        @BindView(R.id.image_view_beauty)
        ImageView imageView;

        @BindView(R.id.loading_progress)
        ProgressBar progressBar;

        @BindView(R.id.image_user_beauty)
        ImageView imageUserBeauty;

        AnnonceFull annonceFull;

        ViewGroup parent;

        ViewHolderBeauty(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
        }

        public ImageView getImageUserBeauty() {
            return imageUserBeauty;
        }

        public ImageView getImageView() {
            return imageView;
        }

        public AnnonceFull getAnnonceFull() {
            return annonceFull;
        }

        public ViewGroup getParent() {
            return parent;
        }

        public ImageView getImageFavorite() {
            return imageFavorite;
        }

        public ImageView getImageShare() {
            return imageShare;
        }
    }
}
