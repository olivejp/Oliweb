package oliweb.nc.oliweb.dto.elasticsearch;

import java.util.List;

import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;

/**
 * Created by orlanth23 on 18/11/2018.
 */

public class ElasticsearchHitsResult {
    private Long total;
    private Long max_score;
    private List<ElasticsearchResult<AnnonceFirebase>> hits;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getMax_score() {
        return max_score;
    }

    public void setMax_score(Long max_score) {
        this.max_score = max_score;
    }

    public List<ElasticsearchResult<AnnonceFirebase>> getHits() {
        return hits;
    }

    public void setHits(List<ElasticsearchResult<AnnonceFirebase>> hits) {
        this.hits = hits;
    }
}
