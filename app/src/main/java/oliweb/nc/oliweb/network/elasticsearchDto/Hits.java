package oliweb.nc.oliweb.network.elasticsearchDto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Hits represent the results of the query (the items matching the search)
 * @param <T> The domain object being queried (results will be serialized to this type)
 */
public class Hits<T> {

    private long total = 0;
    private Float maxScore = 0F;
    private List<Element<T>> rawResults = new ArrayList<>();

    /**
     * @return The total number of results (and not on this single page)
     */
    @SerializedName("total")
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * @return The maximum score reached by the results
     */
    @SerializedName("max_score")
    public Float getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Float maxScore) {
        this.maxScore = maxScore;
    }

    /**
     * @return The ES raw results of the query (with their ES properties)
     */
    @SerializedName("hits")
    public List<Element<T>> getRawResults() {
        return rawResults;
    }

    public void setRawResults(List<Element<T>> results) {
        this.rawResults = results;
    }

    @Override
    public String toString() {
        return "Hits{" +
            "total=" + total +
            ", maxScore=" + maxScore +
            ", rawResults=" + rawResults +
            '}';
    }
}
