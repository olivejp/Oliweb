package oliweb.nc.oliweb.dto.elasticsearch;

public class ElasticsearchRequest {
    private Long timestamp;
    private String request;
    private int version;

    public ElasticsearchRequest() {
    }

    public ElasticsearchRequest(Long timestamp, String request, int version) {
        this.timestamp = timestamp;
        this.request = request;
        this.version = version;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
