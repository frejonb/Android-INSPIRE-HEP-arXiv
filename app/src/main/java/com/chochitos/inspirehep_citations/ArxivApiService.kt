package com.chochitos.inspirehep_citations

import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.simpleframework.xml.*
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*


interface ArxivApiService {

    @GET("query")
    fun getResults(@Query("search_query") search_query: String,
                   @Query("sortBy") sortBy: String,
                   @Query("sortOrder") sortOrder: String,
                   @Query("max_results") max_results: Int): Observable<ArxivModel.Feed>


    companion object {
        fun create(): ArxivApiService {

            val retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(SimpleXmlConverterFactory.create())
                    .baseUrl("http://export.arxiv.org/api/")
                    .build()

            return retrofit.create(ArxivApiService::class.java)
        }
    }


}


object ArxivModel {

    @Root(name = "feed", strict = false)
    data class Feed(@field:ElementList(name = "entry", inline = true) internal var entry: List<Entry>? = null)

    @Root(name = "entry", strict = false)
    data class Entry(@field:Element(name = "id") internal var id: String = "",
                     @field:Element(name = "published") internal var published: String = "",
                     @field:Element(name = "title") internal var title: String = "",
                     @field:Element(name = "summary") internal var summary: String = "",
                     @field:ElementList(name = "author", inline = true) internal var authors: List<Author>? = null,
                     @field:ElementList(name = "link", inline = true) internal var links: List<Link>? = null,
                     @field:Element(name = "primary_category") @field:Namespace(prefix = "arxiv", reference = "http://arxiv.org/schemas/atom") internal var primary_category: PCategory? = null,
                     @field:ElementList(name = "category", inline = true) internal var categories: List<Category>? = null
                     )


    @Root(name = "author", strict = false)
    data class Author(@field:Element(name = "name") internal var name: String = "")

    @Root(name = "primary_category", strict = false)
    data class PCategory(@field:Attribute(name = "term") internal var category: String = "")

    @Root(name = "category", strict = false)
    data class Category(@field:Attribute(name = "term") internal var category: String = "")


    @Root(name = "link", strict = false)
    data class Link(@field:Attribute(name = "href") internal var link: String = "",
                    @field:Attribute(name = "rel") internal var rel: String = "",
                    @field:Attribute(name = "title",required = false) internal var title: String = "")


}


fun arxivSearch(service: ArxivApiService,category: String, formatArxiv: (ArxivModel.Feed) -> Unit): Disposable? {
    val calendar = Calendar.getInstance()
    val dayToday = SimpleDateFormat("EEEE").format(calendar.time)
    var yesterday = Date()
    var twodaysago = Date()

    when(dayToday){
        "Saturday" -> {
            calendar.add(Calendar.DATE,-2)
            yesterday = calendar.time
            calendar.add(Calendar.DATE,-1)
            twodaysago = calendar.time
        }
          "Sunday" -> {
            calendar.add(Calendar.DATE,-3)
             yesterday = calendar.time
            calendar.add(Calendar.DATE,-1)
             twodaysago = calendar.time
        }
          "Monday" -> {
            calendar.add(Calendar.DATE,-3)
             yesterday = calendar.time
            calendar.add(Calendar.DATE,-1)
             twodaysago = calendar.time
        }
        "Tuesday" -> {
            calendar.add(Calendar.DATE,-1)
            yesterday = calendar.time
            calendar.add(Calendar.DATE,-3)
            twodaysago = calendar.time
        }
        else -> {
            calendar.add(Calendar.DATE,-1)
             yesterday = calendar.time
            calendar.add(Calendar.DATE,-1)
             twodaysago = calendar.time
        }
    }

    val sdf = SimpleDateFormat("yyyyMMdd" , Locale.US)


    return service.getResults("cat:$category AND submittedDate:[${sdf.format(twodaysago)}1800 TO ${sdf.format(yesterday)}1800]", "submittedDate", "ascending",1000)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    { result -> formatArxiv(result) },
                    { error -> Log.wtf("mi",error.message) }
            )

}

