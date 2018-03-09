package oliweb.nc.oliweb.network.elasticsearchDto;

import java.util.List;

/**
 * Created by 2761oli on 22/02/2018.
 */

public class ElasticsearchRequest {
    private int page;
    private int perPage;
    private String searchQuery;
    private List<ElasticsearchSort> sorts;

    public ElasticsearchRequest(int page, int perPage, String searchQuery, List<ElasticsearchSort> sorts) {
        this.page = page;
        this.perPage = perPage;
        this.searchQuery = searchQuery;
        this.sorts = sorts;
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

    public List<ElasticsearchSort> getSorts() {
        return sorts;
    }

    public void setSorts(List<ElasticsearchSort> sorts) {
        this.sorts = sorts;
    }
}
