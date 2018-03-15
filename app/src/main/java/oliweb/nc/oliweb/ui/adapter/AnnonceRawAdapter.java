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
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;

/**
 * Created by orlanth23 on 07/02/2018.
 */
public class AnnonceRawAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = AnnonceRawAdapter.class.getName();

    private List<AnnoncePhotos> listAnnonces;
    private View.OnClickListener onClickListener;

    public AnnonceRawAdapter(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
        this.listAnnonces = new ArrayList<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolderResult;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View itemLayoutView;
        itemLayoutView = inflater.inflate(R.layout.adapter_annonce_raw, parent, false);
        viewHolderResult = new ViewHolderRaw(itemLayoutView);
        return viewHolderResult;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        AnnoncePhotos annoncePhotos = listAnnonces.get(position);
        bindViewHolderRaw(viewHolder, annoncePhotos);
    }

    @Override
    public int getItemCount() {
        return listAnnonces.size();
    }

    private void bindViewHolderRaw(RecyclerView.ViewHolder viewHolder, AnnoncePhotos annoncePhotos) {
        ViewHolderRaw viewHolderRaw = (ViewHolderRaw) viewHolder;
        viewHolderRaw.singleAnnonce = annoncePhotos.getAnnonceEntity();

        viewHolderRaw.normalLayoutRaw.setTag(annoncePhotos);
        viewHolderRaw.normalLayoutRaw.setOnClickListener(this.onClickListener);

        // Attribution des données au valeurs graphiques
        viewHolderRaw.textIdAnnonce.setText(String.valueOf(viewHolderRaw.singleAnnonce.getUUID()));
        viewHolderRaw.textTitreAnnonce.setText(viewHolderRaw.singleAnnonce.getTitre());
        viewHolderRaw.textPrixAnnonce.setText(String.valueOf(viewHolderRaw.singleAnnonce.getPrix()));

        // Récupération de la date de publication
        viewHolderRaw.textDatePublicationAnnonce.setText(DateConverter.convertDateEntityToUi(viewHolderRaw.singleAnnonce.getDatePublication()));

        // On fait apparaitre une petite photo seulement si l'annoncePhotos a une photo
        if (!annoncePhotos.getPhotos().isEmpty()) {
            viewHolderRaw.imgPhoto.setVisibility(View.VISIBLE);
        } else {
            viewHolderRaw.imgPhoto.setVisibility(View.GONE);
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

    public class ViewHolderRaw extends RecyclerView.ViewHolder {

        @BindView(R.id.text_id_annonce_raw)
        TextView textIdAnnonce;

        @BindView(R.id.text_titre_annonce_raw)
        TextView textTitreAnnonce;

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

        ViewHolderRaw(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
        }

        public ConstraintLayout getNormalConstraint() {
            return this.normalLayoutRaw;
        }

        public AnnonceEntity getSingleAnnonce() {
            return this.singleAnnonce;
        }
    }
}
