package oliweb.nc.oliweb.ui.adapter;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.support.v7.util.DiffUtil;
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

/**
 * Created by orlanth23 on 07/02/2018.
 */

public class AnnonceAdapterSingle extends RecyclerView.Adapter<AnnonceAdapterSingle.AnnonceAdapterSingleViewHolder> {

    private static final String TAG = AnnonceAdapterSingle.class.getName();

    private List<AnnonceEntity> listAnnonces;
    private Context context;
    private View.OnClickListener onClickListener;

    public AnnonceAdapterSingle(Context context, View.OnClickListener onClickListener) {
        this.context = context;
        this.onClickListener = onClickListener;
    }

    @Override
    public AnnonceAdapterSingleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemLayoutView = inflater.inflate(R.layout.annonce_adapter_single, parent, false);
        return new AnnonceAdapterSingleViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(AnnonceAdapterSingleViewHolder viewHolder, int position) {
        AnnonceEntity annonce = listAnnonces.get(position);
        viewHolder.singleAnnonce = annonce;

        viewHolder.textTitreAnnonce.setText(annonce.getTitre());
        viewHolder.textDescriptionAnnonce.setText(annonce.getDescription());
        viewHolder.textPrixAnnonce.setText(annonce.getPrix());

//        Categorie categorie = ListeCategories.getInstance(mContext).getCategorieById(annonce.getIdCategorieANO());
//        int color = Color.parseColor(categorie.getCouleurCAT());        // Récupération de la couleur
//
//        // Attribution des données au valeurs graphiques
//        viewHolder.textIdAnnonce.setText(String.valueOf(annonce.getUUIDANO()));
//
//        viewHolder.textTitreAnnonce.setText(annonce.getTitreANO());
//        String description = annonce.getDescriptionANO();
//
//        // Si la description fait moins que le nombre maximum de caractère, on prend la taille de la description
//        int nb_caractere = (Utility.getPrefNumberCar(mContext) > description.length()) ? description.length() : Utility.getPrefNumberCar(mContext);
//
//        viewHolder.textDescriptionAnnonce.setText(description.substring(0, nb_caractere).concat("..."));
//        viewHolder.textPrixAnnonce.setText(Utility.convertPrice(annonce.getPriceANO()));
//
//        // Récupération de la date de publication
//        String datePublished = annonce.getDatePublished().toString();
//        viewHolder.textDatePublicationAnnonce.setText(Utility.convertDate(datePublished));
//
//        // On fait apparaitre une petite photo seulement si l'annonce a une photo
//        if (!annonce.getPhotos().isEmpty()) {
//            viewHolder.imgPhoto.setVisibility(View.VISIBLE);
//        } else {
//            viewHolder.imgPhoto.setVisibility(View.GONE);
//        }
    }

    @Override
    public int getItemCount() {
        return listAnnonces.size();
    }

    public void setListAnnonces(final List<AnnonceEntity> newListAnnonces) {
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
                    return listAnnonces.get(oldItemPosition).equals(newListAnnonces.get(newItemPosition));
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    AnnonceEntity newAnnonce = newListAnnonces.get(newItemPosition);
                    AnnonceEntity oldAnnonce = listAnnonces.get(oldItemPosition);
                    return newAnnonce.getIdAnnonce().equals(oldAnnonce.getIdAnnonce())
                            && newAnnonce.getDescription().equals(oldAnnonce.getDescription());
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

        @BindView(R.id.view_pager)
        ViewPager viewPager;

        @BindView(R.id.indicator)
        CircleIndicator indicator;

        AnnonceEntity singleAnnonce;

        AnnonceAdapterSingleViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
            viewPager.setAdapter(new AnnonceViewPagerAdapter(context, new ArrayList<>()));
            indicator.setViewPager(viewPager);
        }
    }
}
