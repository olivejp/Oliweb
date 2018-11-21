package oliweb.nc.oliweb;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

    private Gson gson;

    @Before
    public void setUp(){
        gson = new Gson();
    }

    @Test
    public void test_elasticsearch_query_builder() {
        File json = new File(getClass().getResource("/elasticsearch.json").getPath());
        JsonElement jsonElement = null;
        try {
            JsonParser jsonParser = new JsonParser();
            jsonElement = jsonParser.parse(new FileReader(json));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        List<String> listCategorie = new ArrayList<>();
        listCategorie.add("Immobilier");
        listCategorie.add("Automobile");
        listCategorie.add("Meuble");

        List<String> listFields = new ArrayList<>();
        listFields.add("titre");
        listFields.add("description");

        ElasticsearchQueryBuilder elasticsearchQueryBuilder = new ElasticsearchQueryBuilder();
        elasticsearchQueryBuilder.setFrom(2);
        elasticsearchQueryBuilder.setSize(10);
        elasticsearchQueryBuilder.setListCategories(listCategorie);
        elasticsearchQueryBuilder.setRangePrice(1000, 2000);
        elasticsearchQueryBuilder.setWithPhotoOnly();
        elasticsearchQueryBuilder.setMultiMatchQuery(listFields, "recherche");
        elasticsearchQueryBuilder.addSortingFields("datePublication", "ASC");

        JsonObject valueReturned = elasticsearchQueryBuilder.build();
        String requestJson = gson.toJson(valueReturned);
        String correctJson = gson.toJson(jsonElement);
        assertNotNull(requestJson);
        assertEquals(correctJson, requestJson);
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