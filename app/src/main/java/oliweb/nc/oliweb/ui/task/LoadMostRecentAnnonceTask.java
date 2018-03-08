package oliweb.nc.oliweb.ui.task;

import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;

/**
 * Created by 2761oli on 08/03/2018.
 */

public class LoadMostRecentAnnonceTask extends AsyncTask<Pair<List<AnnoncePhotos>, HashMap<String, AnnonceDto>>, Void, List<AnnoncePhotos>> {

    private static final String TAG = LoadMostRecentAnnonceTask.class.getName();

    private TaskListener<List<AnnoncePhotos>> listener;

    public void setListener(TaskListener<List<AnnoncePhotos>> listener) {
        this.listener = listener;
    }

    @Override
    protected List<AnnoncePhotos> doInBackground(Pair<List<AnnoncePhotos>, HashMap<String, AnnonceDto>>[] lists) {
        Pair<List<AnnoncePhotos>,HashMap<String, AnnonceDto>>  previousLists = lists[0];
        List<AnnoncePhotos> oldList = previousLists.first;
        HashMap<String, AnnonceDto> newList = previousLists.second;

        List<AnnoncePhotos> listPhotos = new ArrayList<>();
        listPhotos.addAll(oldList);
        for (Map.Entry<String, AnnonceDto> entry : newList.entrySet()) {
            boolean trouve = false;
            for (AnnoncePhotos anno : oldList) {
                if (anno.getAnnonceEntity().getUUID().equals(entry.getValue().getUuid())) {
                    trouve = true;
                    break;
                }
            }
            if (!trouve) {
                AnnoncePhotos annoncePhotos = AnnonceConverter.convertDtoToEntity(entry.getValue());
                Log.d(TAG, "Annonce récupérée => " + entry.toString());
                listPhotos.add(annoncePhotos);
            }
        }
        return listPhotos;
    }

    @Override
    protected void onPostExecute(List<AnnoncePhotos> listResult) {
        listener.onSuccess(listResult);
    }
}
