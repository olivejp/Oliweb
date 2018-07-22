package oliweb.nc.oliweb.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Created by orlanth23 on 10/03/2018.
 */
public class ElasticsearchQueryBuilder {
    private JsonObject jsonRequest;

    private void checkJsonObject() {
        if (jsonRequest == null) {
            jsonRequest = new JsonObject();
        }
    }

    private void checkJsonSortArray() {
        checkJsonObject();
        if (!jsonRequest.has("sort")) {
            JsonArray sort = new JsonArray();
            jsonRequest.add("sort", sort);
        }
    }

    public ElasticsearchQueryBuilder setTimestamp(Long timestamp) {
        checkJsonObject();
        jsonRequest.addProperty("timestamp", timestamp);
        return this;
    }

    public ElasticsearchQueryBuilder setFrom(int from) {
        checkJsonObject();
        jsonRequest.addProperty("from", from);
        return this;
    }

    public ElasticsearchQueryBuilder setSize(int size) {
        checkJsonObject();
        jsonRequest.addProperty("size", size);
        return this;
    }

    public ElasticsearchQueryBuilder setMultiMatchQuery(List<String> fields, String query) {
        checkJsonObject();

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

    public ElasticsearchQueryBuilder addSortingFields(String field, String direction) {
        checkJsonObject();
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
