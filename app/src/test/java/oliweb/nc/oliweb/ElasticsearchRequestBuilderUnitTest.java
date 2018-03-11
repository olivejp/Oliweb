package oliweb.nc.oliweb;

import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import oliweb.nc.oliweb.network.ElasticsearchQueryBuilder;

public class ElasticsearchRequestBuilderUnitTest {

    @Test
    public void test_elasticsearch_query_builder() throws Exception {
        ElasticsearchQueryBuilder elasticsearchQueryBuilder = new ElasticsearchQueryBuilder();
        elasticsearchQueryBuilder.setFrom(2);
        elasticsearchQueryBuilder.setSize(10);

        List<String > listFields = new ArrayList<>();
        listFields.add("titre");
        listFields.add("description");

        elasticsearchQueryBuilder.setMultiMatchQuery(listFields, "recherche");

        elasticsearchQueryBuilder.addSortingFields("datePublication", "ASC");

        String valueReturned = elasticsearchQueryBuilder.build();
        Assert.assertNotNull(valueReturned);
        Assert.assertEquals("{\"from\":2,\"size\":10,\"query\":{\"multi_match\":{\"query\":\"recherche\",\"fields\":[\"titre\",\"description\"]}},\"sort\":[{\"datePublication\":{\"order\":\"ASC\"}}]}", valueReturned);
    }
}