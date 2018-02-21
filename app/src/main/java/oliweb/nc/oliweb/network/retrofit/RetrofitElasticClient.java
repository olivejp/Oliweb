package oliweb.nc.oliweb.network.retrofit;


import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import oliweb.nc.oliweb.Utility;
import oliweb.nc.oliweb.database.entity.AnnonceWithPhotos;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceSearchDto;
import oliweb.nc.oliweb.network.elasticsearchDto.Element;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import static oliweb.nc.oliweb.Constants.URL_AFTERSHIP_BASE_URL;

/**
 * Created by orlanth23 on 12/12/2017.
 */
public class RetrofitElasticClient {

    private static final String TAG = RetrofitElasticClient.class.getName();

    private static Retrofit retrofitJson = null;


    private RetrofitElasticClient() {
    }

    /**
     * Va créer un client http qui enverra toujours les mêmes headers
     * A savoir : Authorization et Content-Type
     * Puis va attacher ce client dans un retrofit.
     * Ainsi toutes les requêtes passées avec ce retrofit porteront les mêmes headers.
     *
     * @return {@link Retrofit}
     */
    private static Retrofit getJsonClient() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .addHeader("Authorization", "Basic dXNlcjpmVldmSDhMTllUMjE=")
                            .addHeader("Content-Type", "application/json")
                            .build();
                    return chain.proceed(request);
                })
                .build();


        if (retrofitJson == null) {
            retrofitJson = new Retrofit.Builder()
                    .baseUrl(URL_AFTERSHIP_BASE_URL)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient)
                    .build();
        }
        return retrofitJson;
    }

    /**
     * @param textToFind
     * @return Observable<List<AnnonceSearchDto>>
     */
    public static Observable<List<AnnonceWithPhotos>> searchText(String textToFind) {
        RetrofitElasticCall retrofitElasticCall = RetrofitElasticClient.getJsonClient().create(RetrofitElasticCall.class);

        String query = "{" +
                "\"query\": {" +
                "\"multi_match\": {" +
                "\"query\":\"" + textToFind + "\"," +
                "\"fields\":[\"titre^3\",\"description\"]" +
                "}" +
                "}" +
                "}";
        Log.d(TAG, query);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),query);
        return retrofitElasticCall.searchAnnonceByFullText(requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(annonceSearchDtoResult -> {
                            List<AnnonceWithPhotos> listAnnonceSearchDto = new ArrayList<>();
                            for (Element<AnnonceSearchDto> element : annonceSearchDtoResult.getHits().getHits()) {
                                listAnnonceSearchDto.add(Utility.convertDtoToEntity(element.getSource()));
                            }
                            return Observable.just(listAnnonceSearchDto);
                        }
                );
    }
}