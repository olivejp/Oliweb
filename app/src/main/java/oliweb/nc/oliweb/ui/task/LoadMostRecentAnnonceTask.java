package oliweb.nc.oliweb.ui.task;

import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;

import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.ASC;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_DATE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_PRICE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_TITLE;

/**
 * Created by 2761oli on 08/03/2018.
 */

public class LoadMostRecentAnnonceTask extends AsyncTask<LoadMoreTaskBundle, Void, ArrayList<AnnonceFull>> {

    private static final String TAG = LoadMostRecentAnnonceTask.class.getName();

    public LoadMostRecentAnnonceTask(TaskListener<ArrayList<AnnonceFull>> listener) {
        this.listener = listener;
    }

    private TaskListener<ArrayList<AnnonceFull>> listener;

    public void setListener(TaskListener<ArrayList<AnnonceFull>> listener) {
        this.listener = listener;
    }

    private static Comparator<AnnonceFull> compareDateAsc = (o1, o2) -> {
        if (o1.getAnnonce().getDatePublication() < o2.getAnnonce().getDatePublication()) {
            return 1;
        }
        if (o1.getAnnonce().getDatePublication() > o2.getAnnonce().getDatePublication()) {
            return -1;
        }
        if (o1.getAnnonce().getDatePublication().equals(o2.getAnnonce().getDatePublication())) {
            return 0;
        }
        return 0;
    };

    private static Comparator<AnnonceFull> compareDateDesc = (o1, o2) -> {
        if (o1.getAnnonce().getDatePublication() > o2.getAnnonce().getDatePublication()) {
            return 1;
        }
        if (o1.getAnnonce().getDatePublication() < o2.getAnnonce().getDatePublication()) {
            return -1;
        }
        if (o1.getAnnonce().getDatePublication().equals(o2.getAnnonce().getDatePublication())) {
            return 0;
        }
        return 0;
    };

    private static Comparator<AnnonceFull> comparePrixAsc = (o1, o2) -> {
        if (o1.getAnnonce().getPrix() > o2.getAnnonce().getPrix()) {
            return 1;
        }
        if (o1.getAnnonce().getPrix() < o2.getAnnonce().getPrix()) {
            return -1;
        }
        if (o1.getAnnonce().getPrix().equals(o2.getAnnonce().getPrix())) {
            return 0;
        }
        return 0;
    };

    private static Comparator<AnnonceFull> comparePrixDesc = (o1, o2) -> {
        if (o1.getAnnonce().getPrix() < o2.getAnnonce().getPrix()) {
            return 1;
        }
        if (o1.getAnnonce().getPrix() > o2.getAnnonce().getPrix()) {
            return -1;
        }
        if (o1.getAnnonce().getPrix().equals(o2.getAnnonce().getPrix())) {
            return 0;
        }
        return 0;
    };

    private static void sortList(List<AnnonceFull> listAnnonce, int tri, int direction) {
        switch (tri) {
            case SORT_DATE:
                Collections.sort(listAnnonce, (direction == ASC) ? compareDateAsc : compareDateDesc);
                break;
            case SORT_TITLE:
                Collections.sort(listAnnonce, (o1, o2) -> o1.getAnnonce().getTitre().compareTo(o2.getAnnonce().getTitre()));
                break;
            case SORT_PRICE:
                Collections.sort(listAnnonce, (direction == ASC) ? comparePrixAsc : comparePrixDesc);
            default:
        }
    }

    @Override
    protected ArrayList<AnnonceFull> doInBackground(LoadMoreTaskBundle[] bundles) {
        ArrayList<AnnonceFull> annonceFulls = new ArrayList<>();

        LoadMoreTaskBundle bundle = bundles[0];
        List<AnnonceFull> oldList = bundle.getListPhotosResult();
        DataSnapshot dataSnapshot = bundle.getDataSnapshot();

        if (oldList != null && dataSnapshot != null) {
            annonceFulls.addAll(oldList);
            for (DataSnapshot child : dataSnapshot.getChildren()) {
                try {
                    AnnonceFirebase annonceFirebase = child.getValue(AnnonceFirebase.class);
                    if (annonceFirebase != null && !existInTheList(annonceFulls, annonceFirebase)) {
                        AnnonceFull annonceFull = AnnonceConverter.convertDtoToAnnonceFull(annonceFirebase);
                        annonceFulls.add(annonceFull);
                    }
                } catch (DatabaseException databaseException) {
                    Log.e(TAG, databaseException.getLocalizedMessage(), databaseException);
                }
            }
        }
        sortList(annonceFulls, bundle.getTri(), bundle.getDirection());
        return annonceFulls;
    }

    private boolean existInTheList(List<AnnonceFull> listAnnonceFull, AnnonceFirebase annonceFirebase) {
        for (AnnonceFull anno : listAnnonceFull) {
            if (anno.getAnnonce().getUid().equals(annonceFirebase.getUuid())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPostExecute(ArrayList<AnnonceFull> listResult) {
        listener.onTaskSuccess(listResult);
    }
}
