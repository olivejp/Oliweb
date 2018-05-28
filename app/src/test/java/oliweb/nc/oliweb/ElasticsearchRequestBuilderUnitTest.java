package oliweb.nc.oliweb;

import android.util.Log;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oliweb.nc.oliweb.network.ElasticsearchQueryBuilder;

@RunWith(JUnit4.class)
public class ElasticsearchRequestBuilderUnitTest {

    @Test
    public void test_elasticsearch_query_builder() throws Exception {
        ElasticsearchQueryBuilder elasticsearchQueryBuilder = new ElasticsearchQueryBuilder();
        elasticsearchQueryBuilder.setFrom(2);
        elasticsearchQueryBuilder.setSize(10);

        List<String> listFields = new ArrayList<>();
        listFields.add("titre");
        listFields.add("description");

        elasticsearchQueryBuilder.setMultiMatchQuery(listFields, "recherche");

        elasticsearchQueryBuilder.addSortingFields("datePublication", "ASC");

        String valueReturned = elasticsearchQueryBuilder.build();
        Assert.assertNotNull(valueReturned);
        Assert.assertEquals("{\"from\":2,\"size\":10,\"query\":{\"multi_match\":{\"query\":\"recherche\",\"fields\":[\"titre\",\"description\"]}},\"sort\":[{\"datePublication\":{\"order\":\"ASC\"}}]}", valueReturned);
    }

    @Test
    public void test_bidon() throws Exception {
        List<String> list = null;
        if (list != null && !list.isEmpty()) {
            for (String sdf : list) {
                Log.d(";sdf", sdf);
            }
        }
    }


    @Test
    public void test_bidon_2() throws Exception {
        Map<String, String> urlParam = new HashMap<>();
        urlParam.put("1","xcfgdgsdf");
        urlParam.put("2","sdkdfgdfgjf");
        urlParam.put("3","dfgdsfg dfgs dddsf g");
        String value = urlParam.toString();
        Log.d("tag", value);
    }
}