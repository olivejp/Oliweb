package oliweb.nc.oliweb.network.elasticsearchDto;

/**
 * Created by 2761oli on 22/02/2018.
 */

public class ElasticsearchSortingField {
    private String direction;
    private String field;

    public ElasticsearchSortingField() {
    }

    public ElasticsearchSortingField(String field, String direction) {
        this.direction = direction;
        this.field = field;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
