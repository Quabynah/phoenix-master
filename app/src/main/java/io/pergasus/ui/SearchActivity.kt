/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.app.SharedElementCallback
import android.content.Intent
import android.graphics.Point
import android.graphics.Typeface
import android.os.Bundle
import android.support.annotation.TransitionRes
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.StyleSpan
import android.transition.Transition
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import io.pergasus.BuildConfig
import io.pergasus.R
import io.pergasus.api.PhoenixClient
import io.pergasus.api.ProductItem
import io.pergasus.api.SearchDataManager
import io.pergasus.data.Product
import io.pergasus.ui.recyclerview.InfiniteScrollListener
import io.pergasus.ui.recyclerview.SlideInItemAnimator
import io.pergasus.ui.transitions.CircularReveal
import io.pergasus.util.ImeUtils
import io.pergasus.util.ShortcutHelper
import io.pergasus.util.TransitionUtils
import io.pergasus.util.bindView


@SuppressLint("LogConditional")
class SearchActivity : Activity() {
    private val searchBack: ImageButton by bindView(R.id.searchback)
    private val searchBackContainer: ViewGroup by bindView(R.id.searchback_container)
    private val searchView: SearchView by bindView(R.id.search_view)
    private val searchBackground: View by bindView(R.id.search_background)
    private val progress: ProgressBar by bindView(android.R.id.empty)
    private val results: RecyclerView by bindView(R.id.search_results)
    private val container: ViewGroup by bindView(R.id.container)
    private val searchToolbar: ViewGroup by bindView(R.id.search_toolbar)
    private val resultsContainer: ViewGroup by bindView(R.id.results_container)
    private val fab: ImageButton by bindView(R.id.fab)
    private val saveConfirm: Button by bindView(R.id.save_confirmed)
    private val confirmSaveContainer: ViewGroup by bindView(R.id.confirm_save_container)
    private val scrim: View by bindView(R.id.scrim)
    private val resultsScrim: View by bindView(R.id.results_scrim)

    private var columns: Int = 0
    private var appBarElevation: Float = 0.0f
    private var noResults: TextView? = null
    private val transitions = SparseArray<Transition>()
    private var focusQuery = true

    private lateinit var adapter: SearchDataAdapter
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var dataManager: SearchDataManager
    private lateinit var client: PhoenixClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        client = PhoenixClient(this@SearchActivity)
        columns = resources.getInteger(R.integer.num_columns)
        appBarElevation = resources.getDimension(R.dimen.z_app_bar)
        setupSearchView()

        resultsScrim.setOnClickListener({ hideSaveConfirmation() })
        fab.setOnClickListener({ save() })
        saveConfirm.setOnClickListener({ doSave() })

        searchBack.setOnClickListener({ dismiss() })

        dataManager = object : SearchDataManager(this@SearchActivity) {
            override fun onDataLoaded(data: List<ProductItem>) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Data from search is $data")
                if (data.isNotEmpty()) {
                    if (results.visibility != View.VISIBLE) {
                        TransitionManager.beginDelayedTransition(container,
                                getTransition(R.transition.search_show_results))
                        progress.visibility = View.GONE
                        results.visibility = View.VISIBLE
                        fab.visibility = View.VISIBLE
                    }
                    adapter.addAndResort(data)
                } else {
                    TransitionManager.beginDelayedTransition(
                            container, getTransition(R.transition.auto))
                    progress.visibility = View.GONE
                    setNoResultsVisibility(View.VISIBLE)
                }
            }
        }

        //RecyclerView
        val preloadSizeProvider: ViewPreloadSizeProvider<Product> = ViewPreloadSizeProvider()
        adapter = SearchDataAdapter(this, dataManager, columns, preloadSizeProvider)
        setExitSharedElementCallback(SearchDataAdapter.createSharedElementReenterCallback(this))
        results.adapter = adapter
        results.itemAnimator = SlideInItemAnimator()
        layoutManager = GridLayoutManager(this, columns)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return adapter.getItemColumnSpan(position)
            }
        }
        results.layoutManager = layoutManager
        results.addOnScrollListener(object : InfiniteScrollListener(layoutManager, dataManager) {
            override fun onLoadMore() {
                dataManager.loadMore()
            }
        })
        results.setHasFixedSize(true)
        val shotPreloader: RecyclerViewPreloader<Product> = RecyclerViewPreloader<Product>(this,
                adapter, preloadSizeProvider, 4)
        results.addOnScrollListener(shotPreloader)

        scrim.setOnClickListener { dismiss() }
        setupTransitions()
        onNewIntent(intent)
        ShortcutHelper.reportSearchUsed(this@SearchActivity)
    }

    override fun onNewIntent(intent: Intent?) {
        if (intent != null && intent.hasExtra(SearchManager.QUERY)) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            if (!TextUtils.isEmpty(query)) {
                searchView.setQuery(query, false)
                searchFor(query)
            }
        }
    }

    override fun onBackPressed() {
        if (confirmSaveContainer.visibility == View.VISIBLE) {
            hideSaveConfirmation()
        } else {
            dismiss()
        }
    }

    override fun onPause() {
        // needed to suppress the default window animation when closing the activity
        overridePendingTransition(0, 0)
        super.onPause()
    }

    override fun onDestroy() {
        dataManager.cancelLoading()
        super.onDestroy()
    }


    private fun save() {
        // show the save confirmation bubble
        TransitionManager.beginDelayedTransition(
                resultsContainer, getTransition(R.transition.search_show_confirm))
        fab.visibility = View.INVISIBLE
        confirmSaveContainer.visibility = View.VISIBLE
        resultsScrim.visibility = View.VISIBLE
    }

    private fun doSave() {
        val saveData = Intent()
        saveData.putExtra(EXTRA_QUERY, dataManager.getQuery())
        setResult(RESULT_CODE_SAVE, saveData)
        dismiss()
    }

    private fun dismiss() {
        // clear the background else the touch ripple moves with the translation which looks bad
        searchBack.background = null
        finishAfterTransition()
    }

    private fun hideSaveConfirmation() {
        if (confirmSaveContainer.visibility == View.VISIBLE) {
            TransitionManager.beginDelayedTransition(
                    resultsContainer, getTransition(R.transition.search_hide_confirm))
            confirmSaveContainer.visibility = View.GONE
            resultsScrim.visibility = View.GONE
            fab.visibility = results.visibility
        }
    }

    private fun clearResults() {
        TransitionManager.beginDelayedTransition(container, getTransition(R.transition.auto))
        adapter.clear()
        dataManager.clear()
        results.visibility = View.GONE
        progress.visibility = View.GONE
        fab.visibility = View.GONE
        confirmSaveContainer.visibility = View.GONE
        resultsScrim.visibility = View.GONE
        setNoResultsVisibility(View.GONE)
    }

    private fun setNoResultsVisibility(visibility: Int) {
        if (visibility == View.VISIBLE) {
            if (noResults == null) {
                noResults = (findViewById<View>(R.id.stub_no_search_results) as ViewStub).inflate() as TextView
                noResults?.setOnClickListener({
                    searchView.setQuery("", false)
                    searchView.requestFocus()
                    ImeUtils.showIme(searchView)
                })
            }
            val message = String.format(
                    getString(R.string.no_search_results), searchView.query.toString())
            val ssb = SpannableStringBuilder(message)
            ssb.setSpan(StyleSpan(Typeface.ITALIC),
                    message.indexOf('“') + 1,
                    message.length - 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            noResults?.text = ssb
        }
        if (noResults != null) {
            noResults?.visibility = visibility
        }
    }

    private fun getTransition(@TransitionRes transitionId: Int): Transition {
        var transition = transitions.get(transitionId)
        if (transition == null) {
            transition = TransitionInflater.from(this).inflateTransition(transitionId)
            transitions.put(transitionId, transition)
        }
        return transition
    }

    private fun setupSearchView() {
        val searchManager: SearchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        // hint, inputType & ime options seem to be ignored from XML! Set in code
        searchView.queryHint = getString(R.string.search_hint)
        searchView.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
        searchView.imeOptions = searchView.imeOptions or EditorInfo.IME_ACTION_SEARCH
        EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                if (p0 != null) {
                    searchFor(p0)
                }
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                if (TextUtils.isEmpty(p0)) {
                    clearResults()
                }
                return true
            }
        })
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus && confirmSaveContainer.visibility == View.VISIBLE) {
                hideSaveConfirmation()
            }
        }
    }

    private fun setupTransitions() {
        // grab the position that the search icon transitions in *from*
        // & use it to configure the return transition
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onSharedElementStart(sharedElementNames: MutableList<String>?, sharedElements: MutableList<View>?, sharedElementSnapshots: MutableList<View>?) {
                if (sharedElements != null && !sharedElements.isEmpty()) {
                    val searchIcon: View = sharedElements[0]
                    if (searchIcon.id != R.id.searchback) return
                    val centerX: Int = (searchIcon.left + searchIcon.right) / 2
                    (TransitionUtils.findTransition(
                            window.returnTransition as TransitionSet,
                            CircularReveal::class.java, R.id.results_container) as CircularReveal).setCenter(Point(centerX, 0))
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SearchDataAdapter.REQUEST_CODE_VIEW_PRODUCT -> {
                // by default we focus the search filed when entering this screen. Don't do that
                // when returning from viewing a search result.
                focusQuery = false
            }
        }
    }

    private fun searchFor(query: String) {
        clearResults()
        progress.visibility = View.VISIBLE
        ImeUtils.hideIme(searchView)
        searchView.clearFocus()
        if (BuildConfig.DEBUG) Log.d(TAG, "Search started with query: $query")
        dataManager.searchFor(query)
    }

    companion object {
        private val TAG = SearchActivity::class.java.simpleName
        val RESULT_CODE_SAVE: Int = 234
        val EXTRA_QUERY: String = "EXTRA_QUERY"
        val EXTRA_SAVE: String = "EXTRA_SAVE"
    }
}
