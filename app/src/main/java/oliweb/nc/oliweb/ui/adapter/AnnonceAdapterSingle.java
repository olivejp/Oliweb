package oliweb.nc.oliweb.ui.adapter;

import android.support.v4.view.ViewPager;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.relex.circleindicator.CircleIndicator;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceWithPhotos;

/**
 * Created by orlanth23 on 07/02/2018.
 */

public class AnnonceAdapterSingle extends RecyclerView.Adapter<AnnonceAdapterSingle.AnnonceAdapterSingleViewHolder> {

    private static final String TAG = AnnonceAdapterSingle.class.getName();

    private List<AnnonceWithPhotos> listAnnonces;
    private View.OnClickListener onClickListener;

    public AnnonceAdapterSingle(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
        this.listAnnonces = new ArrayList<>();
    }

    @Override
    public AnnonceAdapterSingleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemLayoutView = inflater.inflate(R.layout.adapter_annonce_beauty, parent, false);
        return new AnnonceAdapterSingleViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(AnnonceAdapterSingleViewHolder viewHolder, int position) {
        AnnonceEntity annonce = listAnnonces.get(position).getAnnonceEntity();
        viewHolder.singleAnnonce = listAnnonces.get(position);

        viewHolder.cardView.setTag(viewHolder.singleAnnonce);
        viewHolder.cardView.setOnClickListener(this.onClickListener);

        viewHolder.textTitreAnnonce.setText(annonce.getTitre());
        viewHolder.textDescriptionAnnonce.setText(annonce.getDescription());
        viewHolder.textPrixAnnonce.setText(String.valueOf(annonce.getPrix() + " XPF"));
    }

    @Override
    public int getItemCount() {
        return listAnnonces.size();
    }

    public void setListAnnonces(final List<AnnonceWithPhotos> newListAnnonces) {
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
                    return listAnnonces.get(oldItemPosition).getAnnonceEntity().getIdAnnonce().equals(newListAnnonces.get(newItemPosition).getAnnonceEntity().getIdAnnonce());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    AnnonceEntity newAnnonce = newListAnnonces.get(newItemPosition).getAnnonceEntity();
                    AnnonceEntity oldAnnonce = listAnnonces.get(oldItemPosition).getAnnonceEntity();
                    return newAnnonce.getIdAnnonce().equals(oldAnnonce.getIdAnnonce())
                            && newAnnonce.getDescription().equals(oldAnnonce.getDescription())
                            && newAnnonce.getPrix().equals(oldAnnonce.getPrix());
                }
            });
            this.listAnnonces = newListAnnonces;
            result.dispatchUpdatesTo(this);
        }
    }

    class AnnonceAdapterSingleViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.text_titre_annonce)
        TextView textTitreAnnonce;

        @BindView(R.id.text_description_annonce)
        TextView textDescriptionAnnonce;

        @BindView(R.id.text_prix_annonce)
        TextView textPrixAnnonce;

        @BindView(R.id.text_date_publication_annonce)
        TextView textDatePublicationAnnonce;

        @BindView(R.id.card_view)
        CardView cardView;

        @BindView(R.id.view_pager)
        ViewPager viewPager;

        @BindView(R.id.indicator)
        CircleIndicator indicator;

        AnnonceWithPhotos singleAnnonce;

        AnnonceAdapterSingleViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
        }
    }
}
