package oliweb.nc.oliweb.network.elasticsearchDto;

/**
 * Created by 2761oli on 22/02/2018.
 */

public class ElasticsearchRequest {
    private int page;
    private int perPage;
    private String searchQuery;

    public ElasticsearchRequest(int page, int perPage, String searchQuery) {
        this.page = page;
        this.perPage = perPage;
        this.searchQuery = searchQuery;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPerPage() {
        return perPage;
    }

    public void setPerPage(int perPage) {
        this.perPage = perPage;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }
}
