package com.chochitos.inspirehep_citations

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.widget.SwipeRefreshLayout
import android.view.MenuItem
import android.view.View
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_arxiv_feed.*
import kotlinx.android.synthetic.main.arxiv_row_view.view.*

class ArxivFeed : AppCompatActivity() {

    private lateinit var swipeContainer: SwipeRefreshLayout

    companion object {
        fun filterByKeywords(list: List<ArxivModel.Entry>, keywords: String, keywordsActive: Boolean): List<ArxivModel.Entry> {
            return if (keywordsActive) list.filter{
                entry -> entry.primary_category!!.category == "hep-th"
            }.filter {
                entry ->
                keywords.split(',').any { keyword ->
                    entry.summary.toLowerCase().contains(keyword,false)
                            || entry.title.toLowerCase().contains(keyword,false)
                            || entry.authors!!.any { author -> author.name.toLowerCase().contains(keyword,false) }}
            } else list.filter{
                entry -> entry.primary_category!!.category == "hep-th"
            }
        }
    }

    var disposable: Disposable? = null

    val arxivApiService by lazy {
        ArxivApiService.create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arxiv_feed)

        val SP = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val keywords = SP.getString("arxivKeywords", "")
        val keywordsActive = SP.getBoolean("filterArxivKeywords", false)


        swipeContainer = arxiv_swipe_refresh
        swipeContainer.setOnRefreshListener {  attemptStartActivity(keywords,keywordsActive) }

        swipeContainer.setColorSchemeColors(resources.getColor(android.R.color.holo_purple))


        attemptStartActivity(keywords,keywordsActive)


    }

    private fun attemptStartActivity(keywords: String, keywordsActive: Boolean){

        if (!swipeContainer.isRefreshing) arxiv_progressBar.visibility = View.VISIBLE

        disposable = arxivSearch(arxivApiService,"hep-th", {formatArxiv(it, keywords, keywordsActive)})

    }


    private fun formatArxiv(result:ArxivModel.Feed, keywords:String, keywordsActive: Boolean){

        if (swipeContainer.isRefreshing) swipeContainer.isRefreshing = false else arxiv_progressBar.visibility = View.INVISIBLE

        val filteredResults = filterByKeywords(result.entry!!, keywords, keywordsActive)

        arxiv_listView.adapter = ViewAdapter(this,
                filteredResults,
                {rowType, view, entryData  ->
                    val viewToBind = view
                    val data = entryData as ArxivModel.Entry
                    viewToBind.arxiv_row_view_title.text = data.title
                    viewToBind.arxiv_row_view_date.text = data.published
                    viewToBind.arxiv_id.text = data.id.split('/').last().split('v').first()
                    viewToBind.arxiv_row_view_authors.text = data.authors!!.joinToString("; ") { it.name }
                    viewToBind.arxiv_row_view_abstract.text = data.summary
                    viewToBind.arxiv_row_view_abstract.visibility = View.GONE

                    viewToBind
                },{_ -> R.layout.arxiv_row_view}
        )

        arxiv_listView.setOnItemClickListener{adapterView, view, position, l ->
                if (view.arxiv_row_view_abstract.visibility == View.GONE) {
                    view.arxiv_row_view_abstract.visibility = View.VISIBLE
                } else {
                    view.arxiv_row_view_abstract.visibility = View.GONE
                }
        }

    }




    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
            // Respond to the action bar's Up/Home button
                android.R.id.home -> {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }



    override fun onResume() {
        super.onResume()

    }


    override fun onPause() {
        super.onPause()
        disposable?.dispose()
    }
}
