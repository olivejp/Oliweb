package oliweb.nc.oliweb.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.CategorieEntity;

public class SpinnerAdapter extends BaseAdapter {

    private AppCompatActivity appCompatActivity;
    private List<CategorieEntity> navCategorieItems;

    @BindView(R.id.idCategory)
    TextView txtidCategory;

    @BindView(R.id.titleCategory)
    TextView txtTitle;

    public SpinnerAdapter(AppCompatActivity appCompatActivity) {
        super();
        this.appCompatActivity = appCompatActivity;
    }

    public void setNavCategorieItems(List<CategorieEntity> navCategorieItems) {
        this.navCategorieItems = navCategorieItems;
    }

    @Override
    public int getCount() {
        return (navCategorieItems != null) ? navCategorieItems.size() : 0;
    }

    @Override
    public CategorieEntity getItem(int position) {
        return (navCategorieItems != null) ? navCategorieItems.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return (navCategorieItems != null) ? navCategorieItems.get(position).getId() : 0L;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = appCompatActivity.getLayoutInflater();
            convertView = mInflater.inflate(R.layout.drawer_list_categorie, null);
        }

        ButterKnife.bind(this, convertView);
        txtidCategory.setText(String.valueOf(navCategorieItems.get(position).getId()));
        txtTitle.setText(navCategorieItems.get(position).getName());
        return convertView;
    }

}
