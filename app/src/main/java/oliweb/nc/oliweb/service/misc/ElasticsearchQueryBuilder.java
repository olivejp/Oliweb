package oliweb.nc.oliweb.service.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Created by orlanth23 on 10/03/2018.
 */
public class ElasticsearchQueryBuilder {
    private JsonObject jsonRequest;

    private void checkJsonObjectExist() {
        if (jsonRequest == null) {
            jsonRequest = new JsonObject();
        }
    }

    private void checkJsonSortArray() {
        checkJsonObjectExist();
        if (!jsonRequest.has("sort")) {
            JsonArray sort = new JsonArray();
            jsonRequest.add("sort", sort);
        }
    }

    public ElasticsearchQueryBuilder setTimestamp(Long timestamp) {
        checkJsonObjectExist();
        jsonRequest.addProperty("timestamp", timestamp);
        return this;
    }

    public ElasticsearchQueryBuilder setFrom(int from) {
        checkJsonObjectExist();
        jsonRequest.addProperty("from", from);
        return this;
    }

    public ElasticsearchQueryBuilder setSize(int size) {
        checkJsonObjectExist();
        jsonRequest.addProperty("size", size);
        return this;
    }

    public ElasticsearchQueryBuilder setMultiMatchQuery(List<String> fields, String query) {
        checkJsonObjectExist();

        JsonArray jsonFieldArray = new JsonArray();
        for (String field : fields) {
            jsonFieldArray.add(field);
        }

        JsonObject jsonMultiMatch = new JsonObject();
        jsonMultiMatch.addProperty("query", query);
        jsonMultiMatch.add("fields", jsonFieldArray);

        JsonObject jsonQuery = new JsonObject();
        jsonQuery.add("multi_match", jsonMultiMatch);
        jsonRequest.add("query", jsonQuery);
        return this;
    }

    public ElasticsearchQueryBuilder setCategorie(String libelleCategorie) {
        checkJsonObjectExist();
        jsonRequest.addProperty("categorie", libelleCategorie);
        return this;
    }

    public ElasticsearchQueryBuilder setRangePrice(int lowerPrice, int higherPrice) {
        checkJsonObjectExist();
        jsonRequest.addProperty("lower", lowerPrice);
        jsonRequest.addProperty("higher", higherPrice);
        return this;
    }

    public ElasticsearchQueryBuilder setWithPhotoOnly(boolean withPhotoOnly) {
        checkJsonObjectExist();
        jsonRequest.addProperty("withPhotoOnly", withPhotoOnly);
        return this;
    }

    public ElasticsearchQueryBuilder addSortingFields(String field, String direction) {
        checkJsonObjectExist();
        checkJsonSortArray();
        JsonArray sortArray = jsonRequest.getAsJsonArray("sort");

        JsonObject jsonSortDirection = new JsonObject();
        jsonSortDirection.addProperty("order", direction);

        JsonObject jsonSortElement = new JsonObject();
        jsonSortElement.add(field, jsonSortDirection);

        sortArray.add(jsonSortElement);

        jsonRequest.add("sort", sortArray);
        return this;
    }

    public String build() {
        return jsonRequest.toString();
    }
}
