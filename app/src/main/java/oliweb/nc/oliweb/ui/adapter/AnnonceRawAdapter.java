package oliweb.nc.oliweb.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;

/**
 * Created by orlanth23 on 07/02/2018.
 */
public class AnnonceRawAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = AnnonceRawAdapter.class.getName();

    private List<AnnoncePhotos> listAnnonces;
    private View.OnClickListener onClickListener;
    private View.OnClickListener popupClickListener;
    private Context context;

    public AnnonceRawAdapter(Context context, View.OnClickListener onClickListener, View.OnClickListener popupClickListener) {
        this.onClickListener = onClickListener;
        this.popupClickListener = popupClickListener;
        this.listAnnonces = new ArrayList<>();
        this.context = context;
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
        } else if (viewHolderRaw.singleAnnonce.getStatut() == StatusRemote.TO_SEND) {
            viewHolderRaw.textDatePublicationAnnonce.setText("Pas encore envoyée");
        }

        viewHolderRaw.imgPopup.setOnClickListener(popupClickListener);
        viewHolderRaw.imgPopup.setTag(annoncePhotos);

        // Affichage des photos
        PhotoMiniAdapter photoMiniAdapter = new PhotoMiniAdapter(context);
        viewHolderRaw.recyclerViewPhoto.setAdapter(photoMiniAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        viewHolderRaw.recyclerViewPhoto.setLayoutManager(linearLayoutManager);
        photoMiniAdapter.setListPhotos(annoncePhotos.getPhotos());
        photoMiniAdapter.notifyDataSetChanged();

        // On regarde s'il y a un envoi en cours sur les photos
        boolean sendingInProgress = false;
        for (PhotoEntity photo : annoncePhotos.getPhotos()) {
            if (photo.getStatut() == StatusRemote.SENDING) {
                sendingInProgress = true;
                break;
            }
        }

        sendingInProgress = (!sendingInProgress && viewHolderRaw.singleAnnonce.getStatut() == StatusRemote.SENDING);

        if (sendingInProgress) {
            viewHolderRaw.textDatePublicationAnnonce.setText("En cours d'envoi");
            viewHolderRaw.progressBar.setVisibility(View.VISIBLE);
        } else {
            viewHolderRaw.progressBar.setVisibility(View.INVISIBLE);
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
                    List<PhotoEntity> oldP = oldAnnonce.getPhotos();
                    List<PhotoEntity> newP = newAnnonce.getPhotos();
                    return newA.getUid().equals(oldA.getUid())
                            && newA.getTitre().equals(oldA.getTitre())
                            && newA.getDescription().equals(oldA.getDescription())
                            && newA.isFavorite() == oldA.isFavorite()
                            && newA.getStatut() == oldA.getStatut()
                            && newAnnonce.getPhotos().size() == oldAnnonce.getPhotos().size()
                            && newA.getPrix().equals(oldA.getPrix())
                            && checkPhotoIsTheSame(newP, oldP)   // Vérification que la liste des photos n'a pas changé
                            && ((newA.getDatePublication() == null && oldA.getDatePublication() == null)
                            || (newA.getDatePublication() != null && oldA.getDatePublication() != null
                            && newA.getDatePublication().equals(oldA.getDatePublication())));
                }
            });
            this.listAnnonces = newListAnnonces;
            result.dispatchUpdatesTo(this);
        }
    }

    private boolean checkPhotoIsTheSame(List<PhotoEntity> newList, List<PhotoEntity> oldList) {
        if (newList == null && oldList == null) return true;
        if (newList == null || oldList == null) return false;
        if (newList.isEmpty() && oldList.isEmpty()) return true;
        if (newList.size() != oldList.size()) return false;
        if (newList.containsAll(oldList)) return true;

        for (PhotoEntity photo : newList) {
            boolean idFound = false;
            Long idToFound = photo.getId();
            for (PhotoEntity oldPhoto : oldList) {
                if (idToFound.equals(oldPhoto.getId())) {
                    idFound = true;
                    if (!photo.getFirebasePath().equals(oldPhoto.getFirebasePath()) || !photo.getUriLocal().equals(oldPhoto.getUriLocal()) || !photo.getStatut().equals(oldPhoto.getStatut())) {
                        return false;
                    }
                }
            }
            if (!idFound) return false;
        }

        return true;
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

        @BindView(R.id.annonce_popup_menu)
        ImageView imgPopup;

        @BindView(R.id.normal_layout_raw)
        ConstraintLayout normalLayoutRaw;

        @BindView(R.id.sending_progress)
        ProgressBar progressBar;

        @BindView(R.id.recycler_photos_annonce)
        RecyclerView recyclerViewPhoto;

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
