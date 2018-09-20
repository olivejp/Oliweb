package oliweb.nc.oliweb.service.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Created by orlanth23 on 10/03/2018.
 */
public class ElasticsearchQueryBuilder {
    private JsonObject jsonRequest;

    private JsonObject checkJsonObjectExist() {
        if (jsonRequest == null) {
            jsonRequest = new JsonObject();
        }
        return jsonRequest;
    }

    private void checkJsonSortArray() {
        checkJsonObjectExist();
        if (!jsonRequest.has("sort")) {
            JsonArray sort = new JsonArray();
            jsonRequest.add("sort", sort);
        }
    }

    private JsonArray initMustFromQuery() {
        checkJsonObjectExist();

        if (!jsonRequest.has("query")) {
            JsonObject bool = new JsonObject();
            bool.add("must", new JsonArray());
            JsonObject query = new JsonObject();
            query.add("bool", bool);
            jsonRequest.add("query", query);
        }

        return jsonRequest.get("query").getAsJsonObject().get("bool").getAsJsonObject().get("must").getAsJsonArray();
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


    /**
     * "multi_match":   {
     * "query":"query",
     * "fields":["field1", "field2",...]
     * }
     *
     * @param fields
     * @param query
     * @return
     */
    public ElasticsearchQueryBuilder setMultiMatchQuery(List<String> fields, String query) {
        JsonArray must = initMustFromQuery();

        JsonArray jsonFieldArray = new JsonArray();
        for (String field : fields) {
            jsonFieldArray.add(field);
        }

        JsonObject jsonMultiMatch = new JsonObject();
        jsonMultiMatch.addProperty("query", query);
        jsonMultiMatch.add("fields", jsonFieldArray);

        JsonObject multiMatch = new JsonObject();
        multiMatch.add("multi_match", jsonMultiMatch);

        must.add(multiMatch);
        return this;
    }

    /**
     * "bool":{
     * "should":[
     * {
     * "match":{
     * "categorie.libelle":"Fleur"
     * }
     * },
     * {
     * "match":{
     * "categorie.libelle":"Automobile"
     * }
     * }
     * ]
     * }
     *
     * @param libelleCategorie
     * @return
     */
    public ElasticsearchQueryBuilder setCategorie(List<String> libelleCategorie) {
        JsonArray must = initMustFromQuery();

        JsonArray should = new JsonArray();

        for (String labelCategory : libelleCategorie) {
            JsonObject categorie = new JsonObject();
            categorie.addProperty("categorie.libelle", labelCategory);

            JsonObject match = new JsonObject();
            match.add("match", categorie);

            should.add(match);
        }

        JsonObject bool = new JsonObject();
        bool.add("should", should);

        JsonObject categorieCondition = new JsonObject();
        categorieCondition.add("bool", bool);

        must.add(categorieCondition);
        return this;
    }

    /**
     * Create a JsonObject in the current jsonRequest
     * <p>
     * "range":{
     * "prix":{
     * "gte":higherPrice,
     * "lte":lowerPrice
     * }
     * }
     * </p>
     *
     * @param lowerPrice
     * @param higherPrice
     * @return
     */
    public ElasticsearchQueryBuilder setRangePrice(Integer lowerPrice, Integer higherPrice) {
        JsonArray must = initMustFromQuery();

        JsonObject priceElement = new JsonObject();
        priceElement.addProperty("lte", higherPrice.toString());
        priceElement.addProperty("gte", lowerPrice.toString());

        JsonObject rangeElement = new JsonObject();
        rangeElement.add("prix", priceElement);

        JsonObject priceCondition = new JsonObject();
        priceCondition.add("range", rangeElement);

        must.add(priceCondition);
        return this;
    }

    /**
     * "exists": {
     * "field":"photos"
     * }
     *
     * @return
     */
    public ElasticsearchQueryBuilder setWithPhotoOnly() {
        JsonArray must = initMustFromQuery();

        JsonObject field = new JsonObject();
        field.addProperty("field", "photos");

        JsonObject photoCondition = new JsonObject();
        photoCondition.add("exists", field);

        must.add(photoCondition);
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

    public JsonObject build() {
        return jsonRequest;
    }
}
