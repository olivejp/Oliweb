package oliweb.nc.oliweb.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.CategorieEntity;

/**
 * Created by orlanth23 on 14/11/2018.
 */
public class CategorieMiniAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = CategorieMiniAdapter.class.getName();

    private List<CategorieEntity> categorieEntities;
    private View.OnClickListener onClickListener;

    public CategorieMiniAdapter(View.OnClickListener onClickListener) {
        this.categorieEntities = new ArrayList<>();
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemLayoutView = inflater.inflate(R.layout.adapter_categorie_mini, parent, false);
        return new ViewHolderCategorieMini(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        CategorieEntity categorieEntity = categorieEntities.get(position);
        bindViewHolderBeauty(viewHolder, categorieEntity);
    }

    @Override
    public int getItemCount() {
        return categorieEntities.size();
    }

    private void bindViewHolderBeauty(RecyclerView.ViewHolder viewHolder, CategorieEntity categorieEntity) {
        ViewHolderCategorieMini viewHolderCategorieMini = (ViewHolderCategorieMini) viewHolder;

        viewHolderCategorieMini.categorieEntity = categorieEntity;
        viewHolderCategorieMini.cardView.setTag(categorieEntity);

        if (onClickListener != null) {
            viewHolderCategorieMini.cardView.setOnClickListener(onClickListener);
        }

        viewHolderCategorieMini.textTitreAnnonce.setText(categorieEntity.getName());
    }

    public void setCategorieEntities(final List<CategorieEntity> newListAnnonces) {
        this.categorieEntities = newListAnnonces;
        notifyDataSetChanged();
    }

    public class ViewHolderCategorieMini extends RecyclerView.ViewHolder {

        @BindView(R.id.text_titre_categorie)
        TextView textTitreAnnonce;

        @BindView(R.id.card_view_categorie)
        CardView cardView;

        CategorieEntity categorieEntity;

        ViewHolderCategorieMini(View itemLayoutView) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
        }

        public CategorieEntity getCategorie() {
            return categorieEntity;
        }
    }
}
