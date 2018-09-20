package oliweb.nc.oliweb;

import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.service.misc.ElasticsearchQueryBuilder;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;

@RunWith(JUnit4.class)
public class ElasticsearchUnitTest {

    @Test
    public void test_elasticsearch_query_builder() {
        ElasticsearchQueryBuilder elasticsearchQueryBuilder = new ElasticsearchQueryBuilder();
        elasticsearchQueryBuilder.setFrom(2);
        elasticsearchQueryBuilder.setSize(10);

        List<String> listFields = new ArrayList<>();
        listFields.add("titre");
        listFields.add("description");

        elasticsearchQueryBuilder.setMultiMatchQuery(listFields, "recherche");

        elasticsearchQueryBuilder.addSortingFields("datePublication", "ASC");

        JsonObject valueReturned = elasticsearchQueryBuilder.build();
        assertNotNull(valueReturned);
        assertEquals("{\"from\":2,\"size\":10,\"query\":{\"multi_match\":{\"query\":\"recherche\",\"fields\":[\"titre\",\"description\"]}},\"sort\":[{\"datePublication\":{\"order\":\"ASC\"}}]}", valueReturned.getAsString());
    }

    @Test
    public void test_bidon_3() {
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");

        TestObserver<String> subscriber = new TestObserver<>();

        Observable.fromIterable(list)
                .doOnNext(System.out::println)
                .doOnComplete(() -> System.out.println("FIN"))
                .subscribe(subscriber);

        if (!subscriber.awaitTerminalEvent(5, TimeUnit.SECONDS)) {
            fail();
        } else {
            subscriber.assertNoErrors();
            subscriber.assertComplete();
        }
    }
}