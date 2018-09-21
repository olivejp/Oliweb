package oliweb.nc.oliweb.dto.elasticsearch;

public class ElasticsearchRequest {
    private Long timestamp;
    private String request;

    public ElasticsearchRequest() {
    }

    public ElasticsearchRequest(Long timestamp, String request) {
        this.timestamp = timestamp;
        this.request = request;
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
}
