package oliweb.nc.oliweb.ui.adapter;

import android.support.constraint.ConstraintLayout;
import android.support.v7.util.DiffUtil;
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
import oliweb.nc.oliweb.DateConverter;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceWithPhotos;

public class AnnonceAdapterRaw extends
        RecyclerView.Adapter<AnnonceAdapterRaw.ViewHolder> {

    public static final String TAG = AnnonceAdapterRaw.class.getName();

    private List<AnnonceWithPhotos> listAnnonces;
    private View.OnClickListener onClickListener;

    public AnnonceAdapterRaw(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
        this.listAnnonces = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemLayoutView = inflater.inflate(R.layout.adapter_annonce_raw, parent, false);
        return new ViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        AnnonceWithPhotos annonceWithPhotos = listAnnonces.get(position);
        viewHolder.singleAnnonce = annonceWithPhotos.getAnnonceEntity();

        viewHolder.normalLayoutRaw.setTag(viewHolder.singleAnnonce);
        viewHolder.normalLayoutRaw.setOnClickListener(this.onClickListener);

        // Attribution des données au valeurs graphiques
        viewHolder.textIdAnnonce.setText(String.valueOf(viewHolder.singleAnnonce.getUUID()));
        viewHolder.textTitreAnnonce.setText(viewHolder.singleAnnonce.getTitre());
        viewHolder.textPrixAnnonce.setText(String.valueOf(viewHolder.singleAnnonce.getPrix()));
        String description = viewHolder.singleAnnonce.getDescription();
        int nb_caractere = (150 > description.length()) ? description.length() : 150;
        viewHolder.textDescriptionAnnonce.setText(description.substring(0, nb_caractere).concat("..."));

        // Récupération de la date de publication
        viewHolder.textDatePublicationAnnonce.setText(DateConverter.convertDateEntityToUi(viewHolder.singleAnnonce.getDatePublication()));

        // On fait apparaitre une petite photo seulement si l'annonceWithPhotos a une photo
        if (!annonceWithPhotos.getPhotos().isEmpty()) {
            viewHolder.imgPhoto.setVisibility(View.VISIBLE);
        } else {
            viewHolder.imgPhoto.setVisibility(View.GONE);
        }
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

    @Override
    public int getItemCount() {
        return listAnnonces.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.text_id_annonce_raw)
        TextView textIdAnnonce;

        @BindView(R.id.text_titre_annonce_raw)
        TextView textTitreAnnonce;

        @BindView(R.id.text_description_annonce_raw)
        TextView textDescriptionAnnonce;

        @BindView(R.id.text_prix_annonce_raw)
        TextView textPrixAnnonce;

        @BindView(R.id.text_date_publication_annonce_raw)
        TextView textDatePublicationAnnonce;

        @BindView(R.id.img_photo_raw)
        ImageView imgPhoto;

        @BindView(R.id.normal_layout_raw)
        ConstraintLayout normalLayoutRaw;

        @BindView(R.id.deleted_layout_raw)
        ConstraintLayout deletedLayoutRaw;

        AnnonceEntity singleAnnonce;

        ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
        }

        public ConstraintLayout getNormalConstraint() {
            return this.normalLayoutRaw;
        }

        public ConstraintLayout getDeleteConstraint() {
            return this.deletedLayoutRaw;
        }

        public AnnonceEntity getSingleAnnonce() {
            return this.singleAnnonce;
        }
    }
}
