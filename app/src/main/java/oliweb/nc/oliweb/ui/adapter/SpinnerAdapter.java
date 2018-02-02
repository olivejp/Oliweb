package oliweb.nc.oliweb.ui.adapter;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.CategorieEntity;

public class SpinnerAdapter extends BaseAdapter {

    private AppCompatActivity appCompatActivity;
    private ArrayList<CategorieEntity> navCategorieItems;

    @BindView(R.id.colorCategory)
    TextView txtColorCategory;

    @BindView(R.id.idCategory)
    TextView txtidCategory;

    @BindView(R.id.titleCategory)
    TextView txtTitle;

    public SpinnerAdapter(AppCompatActivity appCompatActivity, ArrayList<CategorieEntity> navCategorieItems) {
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
            convertView = mInflater.inflate(R.layout.drawer_list_categorie, parent);
        }

        ButterKnife.bind(this, convertView);

        // Récupération de la couleur
        int color = Color.parseColor(navCategorieItems.get(position).getCouleur());

        txtidCategory.setText(String.valueOf(navCategorieItems.get(position).getIdCategorie()));
        txtColorCategory.setBackgroundColor(color);
        txtTitle.setText(navCategorieItems.get(position).getName());

        return convertView;
    }

}
