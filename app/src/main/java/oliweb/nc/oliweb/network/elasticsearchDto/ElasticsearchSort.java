package oliweb.nc.oliweb.network.elasticsearchDto;

/**
 * Created by 2761oli on 22/02/2018.
 */

public class ElasticsearchSort {
    private String direction;
    private String sort;

    public ElasticsearchSort(String sort, String direction) {
        this.direction = direction;
        this.sort = sort;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
