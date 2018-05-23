package com.chochitos.inspirehep_citations

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var swipeContainer: SwipeRefreshLayout

    companion object {
        var totalCitationsOld = 0
        var totalCitationsDiff = 0
        var isInspireUpdateFinished = false
        var isArxivUpdateFinished = false
    }

    var disposable: Disposable? = null

    val inspireHEPApiService by lazy {
        InspireHEPApiService.create()
    }

    val arxivApiService by lazy {
        ArxivApiService.create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)


        val settings = getPreferences(0)
        totalCitationsOld = settings.getInt("totalCitations",0)

        val SP = PreferenceManager.getDefaultSharedPreferences(baseContext)



        swipeContainer = main_swipe_refresh
        swipeContainer.setOnRefreshListener {  attemptStartActivity(SP) }
        swipeContainer.setColorSchemeColors(resources.getColor(android.R.color.holo_purple))


        attemptStartActivity(SP)



    }


    private fun attemptStartActivity(SP: SharedPreferences){

        val author = SP.getString("inspireAuthor", "")
        val keywords = SP.getString("arxivKeywords", "")
        val tag = SP.getString("arxivTag", "")
        val keywordsActive = SP.getBoolean("filterArxivKeywords", false)

        isInspireUpdateFinished = false
        isArxivUpdateFinished = false

        if (author == ""){
            if (swipeContainer.isRefreshing) swipeContainer.isRefreshing = false else main_progressBar.visibility = View.INVISIBLE
            main_new_citation.text = "No Inspire author selected"
            isInspireUpdateFinished = true
        }else{
            if (!swipeContainer.isRefreshing) main_progressBar.visibility = View.VISIBLE
            disposable = inspireSearch(inspireHEPApiService,author, {getInspireTotalCitations(it)})
        }
        if (tag == ""){
            main_new_papers.text = "No arXiv tag selected"
        }else{
            disposable = arxivSearch(arxivApiService,tag, {getArxivFeed(it, keywords, keywordsActive)})
        }



    }

    private fun getArxivFeed(result: ArxivModel.Feed, keywords: String, keywordsActive: Boolean) {

        isArxivUpdateFinished = true
        if (isArxivUpdateFinished && isInspireUpdateFinished ) {
            if (swipeContainer.isRefreshing) swipeContainer.isRefreshing = false else main_progressBar.visibility = View.INVISIBLE
        }

        val filteredfeedsize = ArxivFeed.filterByKeywords(result.entry!!, keywords, keywordsActive).size

        when{
            filteredfeedsize== 1 ->  main_new_papers.text = "1 new paper"
            filteredfeedsize > 1 -> main_new_papers.text = "$filteredfeedsize new papers"
            else ->  main_new_papers.text ="no new papers"
        }

        main_new_papers.setOnClickListener {  startActivity(Intent(this,ArxivFeed::class.java))
        }

    }


    private fun getInspireTotalCitations(result:List<InspireModel.ResultEntry>){

        isInspireUpdateFinished = true
        if (isArxivUpdateFinished && isInspireUpdateFinished ) {
            if (swipeContainer.isRefreshing) swipeContainer.isRefreshing = false else main_progressBar.visibility = View.INVISIBLE
        }

        totalCitationsDiff = result.size - totalCitationsOld

        when{
            totalCitationsDiff== 1 ->  main_new_citation.text = "1 new citation!!"
            totalCitationsDiff > 1 -> main_new_citation.text = "$totalCitationsDiff new citations!!"
            else -> main_new_citation.text ="no new citations"
        }

        main_new_citation.setOnClickListener {  startActivity(Intent(this,InspireCitations::class.java))
        }



    }

    override fun onStop() {
        super.onStop()

        val settings = getPreferences(0)
        val editor = settings.edit()
        editor.putInt("totalCitations", totalCitationsOld + totalCitationsDiff )
        editor.apply()
    }


    override fun onPause() {
        super.onPause()
        disposable?.dispose()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this,SettingsActivity::class.java))
                return true}
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.inspire_citations -> {
                startActivity(Intent(this,InspireCitations::class.java))
            }
            R.id.arxiv_feed -> {
                startActivity(Intent(this,ArxivFeed::class.java))
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
