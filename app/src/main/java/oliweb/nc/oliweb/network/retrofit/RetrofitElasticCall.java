package oliweb.nc.oliweb.network.retrofit;

import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by orlanth23 on 03/10/2017.
 */

public interface RetrofitElasticCall {

    @POST("/v4/trackings")
    Observable<AnnonceSearchDto> searchAnnonceByFullText(String fullText);

}
