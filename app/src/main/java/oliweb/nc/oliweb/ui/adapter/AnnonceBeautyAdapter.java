package oliweb.nc.oliweb.ui.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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

    private static final int VIEW_TYPE_NO_PHOTO = 0;
    private static final int VIEW_TYPE_PHOTO = 1;

    private List<AnnonceFull> listAnnonces;
    private View.OnClickListener onClickListener;
    private View.OnClickListener onClickListenerMore;
    private GlideRequests glide;
    private Context context;

    private AnnonceBeautyAdapter(View.OnClickListener onClickListener,
                                 View.OnClickListener onClickListenerMore) {
        this.listAnnonces = new ArrayList<>();
        this.onClickListener = onClickListener;
        this.onClickListenerMore = onClickListenerMore;
    }

    public AnnonceBeautyAdapter(View.OnClickListener onClickListener,
                                View.OnClickListener onClickListenerMore,
                                Fragment fragment) {
        this(onClickListener, onClickListenerMore);
        if (fragment != null) {
            this.context = fragment.getContext();
            this.glide = GlideApp.with(fragment);
        }
    }

    public AnnonceBeautyAdapter(View.OnClickListener onClickListener,
                                View.OnClickListener onClickListenerMore,
                                AppCompatActivity appCompatActivity) {
        this(onClickListener, onClickListenerMore);
        if (appCompatActivity != null) {
            this.context = appCompatActivity.getApplicationContext();
            this.glide = GlideApp.with(appCompatActivity);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_PHOTO) {
            View itemLayoutView = inflater.inflate(R.layout.adapter_annonce_beauty, parent, false);
            ViewHolderBeauty viewHolderResult = new ViewHolderBeauty(itemLayoutView);
            viewHolderResult.parent = parent;
            return viewHolderResult;
        } else {
            View itemLayoutView = inflater.inflate(R.layout.adapter_annonce_without_photo, parent, false);
            ViewHolderBeautyWithoutPhoto viewHolderWithouPhotoResult = new ViewHolderBeautyWithoutPhoto(itemLayoutView);
            viewHolderWithouPhotoResult.parent = parent;
            return viewHolderWithouPhotoResult;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        AnnonceFull annoncePhotos = listAnnonces.get(position);
        if (getItemViewType(position) == VIEW_TYPE_PHOTO) {
            bindViewHolderBeauty(viewHolder, annoncePhotos);
        } else {
            bindViewHolderBeautyWithoutPhoto(viewHolder, annoncePhotos);
        }
    }

    @Override
    public int getItemCount() {
        return listAnnonces.size();
    }

    @Override
    public int getItemViewType(int position) {
        AnnonceFull annoncePhotos = listAnnonces.get(position);
        if (annoncePhotos.getPhotos() == null || annoncePhotos.getPhotos().isEmpty()) {
            return VIEW_TYPE_NO_PHOTO;
        } else {
            return VIEW_TYPE_PHOTO;
        }
    }

    private void bindCommonViewHolder(CommonViewHolder commonViewHolder, AnnonceFull annoncePhotos) {
        // Chargement de l'image de l'auteur de l'annonce
        if (annoncePhotos.getUtilisateur() != null
                && annoncePhotos.getUtilisateur().get(0) != null
                && StringUtils.isNotBlank(annoncePhotos.getUtilisateur().get(0).getPhotoUrl())) {

            String urlPhoto = annoncePhotos.getUtilisateur().get(0).getPhotoUrl();
            glide.load(urlPhoto)
                    .override(100)
                    .error(R.drawable.ic_account_circle_grey_900_48dp)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .circleCrop()
                    .into(commonViewHolder.imageUserBeauty);
        } else {
            glide.load(R.drawable.ic_account_circle_grey_900_48dp).into(commonViewHolder.imageUserBeauty);
        }

        AnnonceEntity annonce = annoncePhotos.getAnnonce();
        commonViewHolder.annonceFull = annoncePhotos;
        commonViewHolder.cardView.setTag(commonViewHolder);
        commonViewHolder.imageMore.setTag(commonViewHolder);
        commonViewHolder.textUserProfile.setText(annoncePhotos.getUtilisateur().get(0).getProfile());

        if (onClickListener != null) commonViewHolder.cardView.setOnClickListener(onClickListener);
        if (onClickListenerMore != null)
            commonViewHolder.imageMore.setOnClickListener(onClickListenerMore);

        commonViewHolder.textDatePublicationAnnonce.setText(Utility.howLongFromNow(commonViewHolder.annonceFull.getAnnonce().getDatePublication()));
        commonViewHolder.textTitreAnnonce.setText(annonce.getTitre());
        commonViewHolder.textPrixAnnonce.setText(String.valueOf(String.format(Locale.FRANCE, "%,d", annonce.getPrix()) + " xpf").trim());
        commonViewHolder.textTitreCategorie.setText(annoncePhotos.getCategorie().get(0).getName());
    }

    private void bindViewHolderBeautyWithoutPhoto(RecyclerView.ViewHolder viewHolder, AnnonceFull annoncePhotos) {
        ViewHolderBeautyWithoutPhoto holderBeautyWithoutPhoto = (ViewHolderBeautyWithoutPhoto) viewHolder;
        bindCommonViewHolder(holderBeautyWithoutPhoto, annoncePhotos);
        holderBeautyWithoutPhoto.textDescriptionAnnonce.setText(annoncePhotos.getAnnonce().getDescription());
    }

    private void bindViewHolderBeauty(RecyclerView.ViewHolder viewHolder, AnnonceFull annoncePhotos) {
        ViewHolderBeauty viewHolderBeauty = (ViewHolderBeauty) viewHolder;
        bindCommonViewHolder(viewHolderBeauty, annoncePhotos);

        viewHolderBeauty.textNumberPhoto.setText(String.valueOf(annoncePhotos.photos.size()));
        viewHolderBeauty.imageView.setBackground(null);
        viewHolderBeauty.imageView.setVisibility(View.INVISIBLE);

        String urlPhoto = annoncePhotos.getPhotos().get(0).getFirebasePath();

        // Création de la requête pour télécharger l'image
        glide.load(urlPhoto)
                // .thumbnail(requestThumbnail)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
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

    public abstract class CommonViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.text_categorie_annonce)
        TextView textTitreCategorie;

        @BindView(R.id.text_titre_annonce)
        TextView textTitreAnnonce;

        @BindView(R.id.text_prix_annonce)
        TextView textPrixAnnonce;

        @BindView(R.id.text_date_publication_annonce)
        TextView textDatePublicationAnnonce;

        @BindView(R.id.card_view)
        FrameLayout cardView;

        @BindView(R.id.image_more)
        ImageView imageMore;

        @BindView(R.id.image_user_beauty)
        ImageView imageUserBeauty;

        @BindView(R.id.text_user_profile)
        TextView textUserProfile;

        AnnonceFull annonceFull;

        ViewGroup parent;

        CommonViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
        }

        public ImageView getImageUserBeauty() {
            return imageUserBeauty;
        }

        public AnnonceFull getAnnonceFull() {
            return annonceFull;
        }

        protected ViewGroup getParent() {
            return parent;
        }
    }

    public class ViewHolderBeauty extends CommonViewHolder {

        @BindView(R.id.image_photo_number_text)
        TextView textNumberPhoto;

        @BindView(R.id.image_view_beauty)
        ImageView imageView;

        @BindView(R.id.loading_progress)
        ProgressBar progressBar;

        ViewHolderBeauty(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
        }

        public ImageView getImageView() {
            return imageView;
        }

    }

    class ViewHolderBeautyWithoutPhoto extends CommonViewHolder {

        @BindView(R.id.text_description_annonce)
        TextView textDescriptionAnnonce;

        ViewHolderBeautyWithoutPhoto(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
        }
    }
}
