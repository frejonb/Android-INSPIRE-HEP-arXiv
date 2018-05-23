package com.chochitos.inspirehep_citations

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.MenuItem
import android.view.View
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_inspire_citations.*
import kotlinx.android.synthetic.main.inspire_row_view.view.*
import android.preference.PreferenceManager




class InspireCitations : AppCompatActivity() {

    private lateinit var swipeContainer: SwipeRefreshLayout

    var disposable: Disposable? = null

    val inspireHEPApiService by lazy {
        InspireHEPApiService.create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inspire_citations)

        val SP = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val author = SP.getString("inspireAuthor", "")


        swipeContainer = inspire_swipe_refresh
        swipeContainer.setOnRefreshListener {  attemptStartActivity(author) }
        swipeContainer.setColorSchemeColors(resources.getColor(android.R.color.holo_purple))


        attemptStartActivity(author)

    }

    private fun attemptStartActivity(author: String){

        if (author == ""){
            inspire_nouser.visibility = View.VISIBLE
        }else{
            if (!swipeContainer.isRefreshing) inspire_progressBar.visibility = View.VISIBLE
            disposable = inspireSearch(inspireHEPApiService,author, {formatInspire(it)})
        }



    }


    private fun formatInspire(result:List<InspireModel.ResultEntry>){

        if (swipeContainer.isRefreshing) swipeContainer.isRefreshing = false else inspire_progressBar.visibility = View.INVISIBLE
        inspire_total_citations_layout.visibility = View.VISIBLE

        inspire_total_citations.text = result.size.toString()

        inspire_listView.adapter = ViewAdapter(this,
                result,
                {rowType, view, entryData ->
                    val viewToBind = view
                    val data = entryData as InspireModel.ResultEntry
                    val abs = if (data.abstract is List<*>) (data.abstract as List<Map<String,String>>)[0]["summary"] else (data.abstract as Map<String,String>)["summary"]
                    viewToBind.inspire_row_view_title.text = data.title.title
                    viewToBind.inspire_row_view_date.text = data.creation_date
                    viewToBind.inspire_row_view_citations.text = data.number_of_citations.toString()
                    viewToBind.inspire_row_view_authors.text = data.authors.joinToString("; ") { it.full_name }
                    viewToBind.inspire_row_view_abstract.text = abs.toString()
                    viewToBind.inspire_row_view_abstract.visibility = View.GONE

                    viewToBind
                },{_ -> R.layout.inspire_row_view}
        )

        inspire_listView.setOnItemClickListener{adapterView, view, position, l ->
            if( view.inspire_row_view_abstract.visibility == View.GONE) {
                view.inspire_row_view_abstract.visibility = View.VISIBLE
            }
            else {
                view.inspire_row_view_abstract.visibility = View.GONE
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
