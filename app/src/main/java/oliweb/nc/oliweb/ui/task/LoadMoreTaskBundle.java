package oliweb.nc.oliweb.ui.task;

import com.google.firebase.database.DataSnapshot;

import java.util.List;

import oliweb.nc.oliweb.database.entity.AnnonceFull;

/**
 * Created by 2761oli on 09/03/2018.
 */

public class LoadMoreTaskBundle {
    private List<AnnonceFull> listPhotosResult;
    private DataSnapshot dataSnapshot;
    private int tri;
    private int direction;

    public LoadMoreTaskBundle() {
    }

    public LoadMoreTaskBundle(List<AnnonceFull> listPhotosResult, DataSnapshot dataSnapshot, int tri, int direction) {
        this.listPhotosResult = listPhotosResult;
        this.dataSnapshot = dataSnapshot;
        this.tri = tri;
        this.direction = direction;
    }

    public List<AnnonceFull> getListPhotosResult() {
        return listPhotosResult;
    }

    public void setListPhotosResult(List<AnnonceFull> listPhotosResult) {
        this.listPhotosResult = listPhotosResult;
    }

    public DataSnapshot getDataSnapshot() {
        return dataSnapshot;
    }

    public void setDataSnapshot(DataSnapshot dataSnapshot) {
        this.dataSnapshot = dataSnapshot;
    }

    public int getTri() {
        return tri;
    }

    public void setTri(int tri) {
        this.tri = tri;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }
}
