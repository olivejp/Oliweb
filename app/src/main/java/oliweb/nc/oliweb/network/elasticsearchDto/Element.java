package oliweb.nc.oliweb.network.elasticsearchDto;


import com.google.gson.annotations.SerializedName;

public class Element<T> {

    @SerializedName("_index")
    private String index;

    @SerializedName("_type")
    private String type;

    @SerializedName("_score")
    private Float score;

    @SerializedName("_id")
    private String id;

    @SerializedName("_source")
    private T source;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public T getSource() {
        return source;
    }

    public void setSource(T source) {
        this.source = source;
    }
}
