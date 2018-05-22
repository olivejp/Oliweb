package oliweb.nc.oliweb.ui.adapter;

import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by orlanth23 on 07/02/2018.
 */
public class AnnonceRawAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = AnnonceRawAdapter.class.getName();

    private List<AnnoncePhotos> listAnnonces;
    private View.OnClickListener onClickListener;
    private View.OnClickListener popupClickListener;

    public AnnonceRawAdapter(View.OnClickListener onClickListener, View.OnClickListener popupClickListener) {
        this.onClickListener = onClickListener;
        this.popupClickListener = popupClickListener;
        this.listAnnonces = new ArrayList<>();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolderResult;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View itemLayoutView;
        itemLayoutView = inflater.inflate(R.layout.adapter_annonce_raw, parent, false);
        viewHolderResult = new ViewHolderRaw(itemLayoutView);
        return viewHolderResult;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
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
        viewHolderRaw.textIdAnnonce.setText(String.valueOf(viewHolderRaw.singleAnnonce.getUid()));
        viewHolderRaw.textTitreAnnonce.setText(viewHolderRaw.singleAnnonce.getTitre());
        viewHolderRaw.textPrixAnnonce.setText(String.valueOf(String.format(Locale.FRANCE, "%,d", viewHolderRaw.singleAnnonce.getPrix()) + " XPF"));

        // Récupération de la date de publication
        if (viewHolderRaw.singleAnnonce.getStatut() == StatusRemote.SEND) {
            if (viewHolderRaw.singleAnnonce.getDatePublication() != null) {
                viewHolderRaw.textDatePublicationAnnonce.setText(DateConverter.simpleUiDateFormat.format(new Date(viewHolderRaw.singleAnnonce.getDatePublication())));
            }
        } else if (viewHolderRaw.singleAnnonce.getStatut() == StatusRemote.TO_SEND){
            viewHolderRaw.textDatePublicationAnnonce.setText("Pas encore envoyée");
        }

        viewHolderRaw.imgPopup.setOnClickListener(popupClickListener);
        viewHolderRaw.imgPopup.setTag(annoncePhotos);

        // Calcul du nombre de photo actuellement correctes
        // On fait apparaitre une petite photo seulement si l'annoncePhotos a une photo
        if (!annoncePhotos.getPhotos().isEmpty()) {
            int photoAvailable = 0;
            for (PhotoEntity photo : annoncePhotos.getPhotos()) {
                if (!Utility.allStatusToAvoid().contains(photo.getStatut().toString())) {
                    photoAvailable++;
                }
            }
            viewHolderRaw.textNbPhotos.setText(String.valueOf(photoAvailable));
        } else {
            viewHolderRaw.textNbPhotos.setText("0");
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
                    AnnoncePhotos newAnnonce = newListAnnonces.get(newItemPosition);
                    AnnoncePhotos oldAnnonce = listAnnonces.get(oldItemPosition);
                    AnnonceEntity oldA = oldAnnonce.getAnnonceEntity();
                    AnnonceEntity newA = newAnnonce.getAnnonceEntity();
                    return newA.getUid().equals(oldA.getUid())
                            && newA.getTitre().equals(oldA.getTitre())
                            && newA.getDescription().equals(oldA.getDescription())
                            && newA.isFavorite() == oldA.isFavorite()
                            && newAnnonce.getPhotos().size() == oldAnnonce.getPhotos().size()
                            && newA.getPrix().equals(oldA.getPrix())
                            && ((newA.getDatePublication() == null && oldA.getDatePublication() == null)
                            || (newA.getDatePublication() != null && oldA.getDatePublication() != null
                            && newA.getDatePublication().equals(oldA.getDatePublication())));
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

        @BindView(R.id.annonce_popup_menu)
        ImageView imgPopup;

        @BindView(R.id.normal_layout_raw)
        ConstraintLayout normalLayoutRaw;

        @BindView(R.id.text_nb_photos)
        TextView textNbPhotos;

        @BindView(R.id.text_nb_conversations)
        TextView textNbConversations;

        AnnonceEntity singleAnnonce;

        ViewHolderRaw(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
        }

        public ConstraintLayout getNormalConstraint() {
            return this.normalLayoutRaw;
        }

    }
}
