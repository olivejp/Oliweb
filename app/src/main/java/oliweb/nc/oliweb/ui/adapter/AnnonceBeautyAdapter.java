package oliweb.nc.oliweb.ui.adapter;

import android.support.v4.view.ViewPager;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.relex.circleindicator.CircleIndicator;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by orlanth23 on 07/02/2018.
 */
public class AnnonceBeautyAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = AnnonceBeautyAdapter.class.getName();

    private List<AnnoncePhotos> listAnnonces;
    private View.OnClickListener onClickListener;
    private View.OnClickListener onFavoriteClickListener;
    private View.OnClickListener onShareClickListener;

    public AnnonceBeautyAdapter(View.OnClickListener onClickListener, View.OnClickListener onFavoriteClickListener, View.OnClickListener onShareClickListener) {
        this.onClickListener = onClickListener;
        this.onFavoriteClickListener = onFavoriteClickListener;
        this.onShareClickListener = onShareClickListener;
        this.listAnnonces = new ArrayList<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolderResult;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemLayoutView;
        itemLayoutView = inflater.inflate(R.layout.adapter_annonce_beauty, parent, false);
        viewHolderResult = new ViewHolderBeauty(itemLayoutView);
        ((ViewHolderBeauty) viewHolderResult).parent = parent;
        return viewHolderResult;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
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
        viewHolderBeauty.singleAnnonce = annonce;

        viewHolderBeauty.cardView.setTag(annoncePhotos);
        viewHolderBeauty.imageFavorite.setTag(annoncePhotos);
        viewHolderBeauty.imageShare.setTag(annoncePhotos);

        viewHolderBeauty.cardView.setOnClickListener(this.onClickListener);
        viewHolderBeauty.imageFavorite.setOnClickListener(this.onFavoriteClickListener);
        viewHolderBeauty.imageShare.setOnClickListener(this.onShareClickListener);

        if (viewHolderBeauty.singleAnnonce.isFavorite()) {
            viewHolderBeauty.imageFavorite.setImageResource(R.drawable.ic_favorite_red_700_48dp);
        }

        // Récupération de la date de publication
        viewHolderBeauty.textDatePublicationAnnonce.setText(Utility.howLongFromNow(viewHolderBeauty.singleAnnonce.getDatePublication()));

        viewHolderBeauty.textTitreAnnonce.setText(annonce.getTitre());
        viewHolderBeauty.textPrixAnnonce.setText(String.valueOf(annonce.getPrix() + " XPF"));

        if (annoncePhotos.getPhotos() != null && !annoncePhotos.getPhotos().isEmpty()) {
            viewHolderBeauty.viewPager.setAdapter(new AnnonceViewPagerAdapter(viewHolderBeauty.parent.getContext(), annoncePhotos.getPhotos()));
            viewHolderBeauty.indicator.setViewPager(viewHolderBeauty.viewPager);
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
                    return listAnnonces.get(oldItemPosition).getAnnonceEntity().getUUID().equals(newListAnnonces.get(newItemPosition).getAnnonceEntity().getUUID());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    AnnonceEntity newAnnonce = newListAnnonces.get(newItemPosition).getAnnonceEntity();
                    AnnonceEntity oldAnnonce = listAnnonces.get(oldItemPosition).getAnnonceEntity();
                    return newAnnonce.getUUID().equals(oldAnnonce.getUUID())
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

    class ViewHolderBeauty extends RecyclerView.ViewHolder {

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

        @BindView(R.id.view_pager)
        ViewPager viewPager;

        @BindView(R.id.indicator)
        CircleIndicator indicator;

        AnnonceEntity singleAnnonce;

        ViewGroup parent;

        ViewHolderBeauty(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
        }
    }
}
