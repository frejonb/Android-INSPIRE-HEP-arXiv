package com.chochitos.inspirehep_citations

import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import retrofit2.http.GET
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Query

interface InspireHEPApiService {

    @GET("search")
    fun getResults(@Query("p") p: String,
                     @Query("rg") rg: String,
                     @Query("of") of: String,
                      @Query("ot") ot: String): Observable<List<InspireModel.ResultEntry>>

    companion object {
        fun create(): InspireHEPApiService {

            val retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl("http://inspirehep.net/")
                    .build()

            return retrofit.create(InspireHEPApiService::class.java)
        }
    }

}


object InspireModel {

    data class ResultEntry(val abstract: Any,
                           val authors: List<Author>,
                           val creation_date: String,
                           val title: Title,
                           val number_of_citations: Int,
                           val recid: Int)


    data class Author(val full_name: String)
    data class Title(val title: String)


}

fun inspireSearch(service: InspireHEPApiService, searchString: String, formatInspire: (List<InspireModel.ResultEntry>) -> Unit): Disposable? {
    return service.getResults("refersto:author:$searchString","1000", "recjson", "recid,creation_date,authors,title,abstract,number_of_citations")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    { result -> formatInspire(result) },
                    { error -> Log.wtf("mi",error.message) }
            )

}