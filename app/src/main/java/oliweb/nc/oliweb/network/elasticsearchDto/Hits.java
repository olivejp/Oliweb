package oliweb.nc.oliweb.network.elasticsearchDto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Hits represent the results of the query (the items matching the search)
 * @param <T> The domain object being queried (results will be serialized to this type)
 */
public class Hits<T> {

    @SerializedName("total")
    private long total;

    @SerializedName("max_score")
    private Float maxScore;

    @SerializedName("hits")
    private List<Element<T>> hits = new ArrayList<>();

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public Float getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Float maxScore) {
        this.maxScore = maxScore;
    }

    public List<Element<T>> getHits() {
        return hits;
    }

    public void setHits(List<Element<T>> hits) {
        this.hits = hits;
    }
}
