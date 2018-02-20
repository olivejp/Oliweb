package oliweb.nc.oliweb.network.elasticsearchDto;

import com.google.gson.annotations.SerializedName;

public class Result<T> {

    @SerializedName("took")
    private long millisecondResponse;

    @SerializedName("time_out")
    private boolean timeOut;

    @SerializedName("hits")
    private Hits<T> hits;

    public long getMillisecondResponse() {
        return millisecondResponse;
    }

    public void setMillisecondResponse(long millisecondResponse) {
        this.millisecondResponse = millisecondResponse;
    }

    public boolean isTimeOut() {
        return timeOut;
    }

    public void setTimeOut(boolean timeOut) {
        this.timeOut = timeOut;
    }

    public Hits<T> getHits() {
        return hits;
    }

    public void setHits(Hits<T> hits) {
        this.hits = hits;
    }
}
