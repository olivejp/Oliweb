package oliweb.nc.oliweb.ui.adapter;

import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

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

    public SpinnerAdapter(AppCompatActivity appCompatActivity, List<CategorieEntity> navCategorieItems) {
        super();
        this.appCompatActivity = appCompatActivity;
        this.navCategorieItems = navCategorieItems;
    }

    @Override
    public int getCount() {
        return navCategorieItems.size();
    }

    @Override
    public CategorieEntity getItem(int position) {
        return navCategorieItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = appCompatActivity.getLayoutInflater();
            convertView = mInflater.inflate(R.layout.drawer_list_categorie, null);
        }

        ButterKnife.bind(this, convertView);
        txtidCategory.setText(String.valueOf(navCategorieItems.get(position).getIdCategorie()));
        txtTitle.setText(navCategorieItems.get(position).getName());
        return convertView;
    }

}
