package oliweb.nc.oliweb.ui.task;

import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;

import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.ASC;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_DATE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_PRICE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_TITLE;

/**
 * Created by 2761oli on 08/03/2018.
 */

public class LoadMostRecentAnnonceTask extends AsyncTask<LoadMoreTaskBundle, Void, ArrayList<AnnoncePhotos>> {

    private static final String TAG = LoadMostRecentAnnonceTask.class.getName();

    public LoadMostRecentAnnonceTask(TaskListener<ArrayList<AnnoncePhotos>> listener) {
        this.listener = listener;
    }

    private TaskListener<ArrayList<AnnoncePhotos>> listener;

    public void setListener(TaskListener<ArrayList<AnnoncePhotos>> listener) {
        this.listener = listener;
    }

    private static Comparator<AnnoncePhotos> compareDateAsc = (o1, o2) -> {
        if (o1.getAnnonceEntity().getDatePublication() < o2.getAnnonceEntity().getDatePublication()) {
            return 1;
        }
        if (o1.getAnnonceEntity().getDatePublication() > o2.getAnnonceEntity().getDatePublication()) {
            return -1;
        }
        if (o1.getAnnonceEntity().getDatePublication().equals(o2.getAnnonceEntity().getDatePublication())) {
            return 0;
        }
        return 0;
    };

    private static Comparator<AnnoncePhotos> compareDateDesc = (o1, o2) -> {
        if (o1.getAnnonceEntity().getDatePublication() > o2.getAnnonceEntity().getDatePublication()) {
            return 1;
        }
        if (o1.getAnnonceEntity().getDatePublication() < o2.getAnnonceEntity().getDatePublication()) {
            return -1;
        }
        if (o1.getAnnonceEntity().getDatePublication().equals(o2.getAnnonceEntity().getDatePublication())) {
            return 0;
        }
        return 0;
    };

    private static Comparator<AnnoncePhotos> comparePrixAsc = (o1, o2) -> {
        if (o1.getAnnonceEntity().getPrix() > o2.getAnnonceEntity().getPrix()) {
            return 1;
        }
        if (o1.getAnnonceEntity().getPrix() < o2.getAnnonceEntity().getPrix()) {
            return -1;
        }
        if (o1.getAnnonceEntity().getPrix().equals(o2.getAnnonceEntity().getPrix())) {
            return 0;
        }
        return 0;
    };

    private static Comparator<AnnoncePhotos> comparePrixDesc = (o1, o2) -> {
        if (o1.getAnnonceEntity().getPrix() < o2.getAnnonceEntity().getPrix()) {
            return 1;
        }
        if (o1.getAnnonceEntity().getPrix() > o2.getAnnonceEntity().getPrix()) {
            return -1;
        }
        if (o1.getAnnonceEntity().getPrix().equals(o2.getAnnonceEntity().getPrix())) {
            return 0;
        }
        return 0;
    };

    public static void sortList(List<AnnoncePhotos> listAnnoncePhotos, int tri, int direction) {
        switch (tri) {
            case SORT_DATE:
                Collections.sort(listAnnoncePhotos, (direction == ASC) ? compareDateAsc : compareDateDesc);
                break;
            case SORT_TITLE:
                Collections.sort(listAnnoncePhotos, (o1, o2) -> o1.getAnnonceEntity().getTitre().compareTo(o2.getAnnonceEntity().getTitre()));
                break;
            case SORT_PRICE:
                Collections.sort(listAnnoncePhotos, (direction == ASC) ? comparePrixAsc : comparePrixDesc);
                break;
        }
    }

    @Override
    protected ArrayList<AnnoncePhotos> doInBackground(LoadMoreTaskBundle[] bundles) {
        ArrayList<AnnoncePhotos> listPhotosResult = new ArrayList<>();

        LoadMoreTaskBundle bundle = bundles[0];
        List<AnnoncePhotos> oldList = bundle.getListPhotosResult();
        DataSnapshot dataSnapshot = bundle.getDataSnapshot();

        if (oldList != null && dataSnapshot != null) {
            listPhotosResult.addAll(oldList);
            for (DataSnapshot child : dataSnapshot.getChildren()) {
                AnnonceDto annonceDto = child.getValue(AnnonceDto.class);
                if (annonceDto != null) {
                    boolean trouve = false;
                    for (AnnoncePhotos anno : oldList) {
                        if (anno.getAnnonceEntity().getUuid().equals(annonceDto.getUuid())) {
                            trouve = true;
                            break;
                        }
                    }
                    if (!trouve) {
                        AnnoncePhotos annoncePhotos = AnnonceConverter.convertDtoToAnnoncePhotos(annonceDto);
                        Log.d(TAG, "Annonce récupérée => " + annonceDto.toString());
                        listPhotosResult.add(annoncePhotos);
                    }
                }
            }
        }

        sortList(listPhotosResult, bundle.getTri(), bundle.getDirection());

        return listPhotosResult;
    }

    @Override
    protected void onPostExecute(ArrayList<AnnoncePhotos> listResult) {
        listener.onTaskSuccess(listResult);
    }
}
