package oliweb.nc.oliweb.network.retrofit;

import io.reactivex.Observable;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceSearchDto;
import oliweb.nc.oliweb.network.elasticsearchDto.Result;
import retrofit2.http.Body;
import retrofit2.http.GET;

/**
 * Created by orlanth23 on 03/10/2017.
 */

public interface RetrofitElasticCall {
    @GET("/_search")
    Observable<Result<AnnonceSearchDto>> searchAnnonceByFullText(@Body String query);
}
