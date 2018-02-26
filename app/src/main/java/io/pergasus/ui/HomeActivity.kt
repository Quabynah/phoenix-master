/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.*
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.*
import android.text.Annotation
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.transition.TransitionManager
import android.view.*
import android.widget.*
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.google.firebase.auth.FirebaseAuth
import io.pergasus.R
import io.pergasus.api.*
import io.pergasus.data.Product
import io.pergasus.ui.FilterAdapter.FilterAuthoriser
import io.pergasus.ui.recyclerview.FilterTouchHelperCallback
import io.pergasus.ui.recyclerview.GridItemDividerDecoration
import io.pergasus.ui.recyclerview.SlideInItemAnimator
import io.pergasus.ui.transitions.FabTransform
import io.pergasus.ui.transitions.MorphTransform
import io.pergasus.util.AnimUtils
import io.pergasus.util.ViewUtils
import io.pergasus.util.bindView
import timber.log.Timber
import java.security.InvalidParameterException
import java.util.*

/** Home Screen for tha application */
class HomeActivity : Activity() {

    private val grid: RecyclerView by bindView(R.id.grid)
    private val drawer: DrawerLayout by bindView(R.id.drawer)
    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val fab: ImageButton by bindView(R.id.fab)
    private val loading: ProgressBar by bindView(android.R.id.empty)
    private val filtersList: RecyclerView by bindView(R.id.filters)

    private var noConnection: ImageView? = null
    private var noFiltersEmptyText: TextView? = null
    private lateinit var client: PhoenixClient
    //Internet connectivity
    private var connected: Boolean = true
    private var monitoringConnectivity: Boolean = false
    private var isLoading: Boolean = true
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var adapter: DataAdapter
    private lateinit var dataManager: DataManager
    private lateinit var filtersAdapter: FilterAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //Set action bar for the activity in order to activate the inflated menu
        setActionBar(toolbar)

        //Init client
        client = PhoenixClient(this@HomeActivity)

        //Firebase Auth
        auth = client.auth

        //Add AuthStateListener
        listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                invalidateOptionsMenu()
            }
        }

        drawer.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        //Animate the toolbar into view
        if (savedInstanceState == null) {
            animateToolbar()
        }
        setExitSharedElementCallback(DataAdapter.createSharedElementReenterCallback(this))

        //Set visibility
        fab.visibility = if (client.isConnected) View.VISIBLE else View.GONE

        //set click action for FAB
        fab.setOnClickListener {
            if (client.isLoggedIn) {
                val intent = Intent(this, CartActivity::class.java)
                FabTransform.addExtras(intent,
                        ContextCompat.getColor(this, R.color.accent), R.drawable.ic_shopping_cart_black_24dp)
                val options = ActivityOptions.makeSceneTransitionAnimation(this, fab,
                        getString(R.string.transition_new_designer_news_post))
                startActivityForResult(intent, RC_CART, options.toBundle())
            } else {
                val intent = Intent(this, AuthActivity::class.java)
                FabTransform.addExtras(intent,
                        ContextCompat.getColor(this, R.color.accent), R.drawable.ic_shopping_cart_black_24dp)
                val options = ActivityOptions.makeSceneTransitionAnimation(this, fab,
                        getString(R.string.transition_dribbble_login))
                startActivityForResult(intent, RC_LOGIN_BASIC, options.toBundle())
            }
        }


        //Customize drawer layout to support landscape mode
        drawer.setOnApplyWindowInsetsListener { _, p1 ->
            // inset the toolbar down by the status bar height
            val lpToolbar: ViewGroup.MarginLayoutParams = toolbar.layoutParams as ViewGroup.MarginLayoutParams
            if (p1 != null) {
                lpToolbar.topMargin += p1.systemWindowInsetTop
                lpToolbar.leftMargin += p1.systemWindowInsetLeft
                lpToolbar.rightMargin += p1.systemWindowInsetRight
                toolbar.layoutParams = lpToolbar

                // inset the grid top by statusbar+toolbar & the bottom by the navbar (don't clip)
                grid.setPadding(
                        grid.paddingLeft + p1.systemWindowInsetLeft, // landscape
                        p1.systemWindowInsetTop
                                + ViewUtils.getActionBarSize(this@HomeActivity),
                        grid.paddingRight + p1.systemWindowInsetRight, // landscape
                        grid.paddingBottom + p1.systemWindowInsetBottom)
                // inset the fab for the navbar
                val lpFab: ViewGroup.MarginLayoutParams = fab.layoutParams as ViewGroup.MarginLayoutParams
                lpFab.bottomMargin += p1.systemWindowInsetBottom // portrait
                lpFab.rightMargin += p1.systemWindowInsetRight // landscape
                fab.layoutParams = lpFab

                // we place a background behind the status bar to combine with it's semi-transparent
                // color to get the desired appearance.  Set it's height to the status bar height
                val statusBarBackground: View = findViewById(R.id.status_bar_background)
                val lpStatus: FrameLayout.LayoutParams = statusBarBackground.layoutParams as FrameLayout.LayoutParams
                lpStatus.height = p1.systemWindowInsetTop
                statusBarBackground.layoutParams = lpStatus

                // inset the filters list for the status bar / navbar
                // need to set the padding end for landscape case
                val ltr: Boolean = filtersList.layoutDirection == View.LAYOUT_DIRECTION_LTR
                filtersList.setPaddingRelative(filtersList.paddingStart,
                        filtersList.paddingTop + p1.systemWindowInsetTop,
                        filtersList.paddingEnd + (if (ltr) p1.systemWindowInsetRight else 0),
                        filtersList.paddingBottom + p1.systemWindowInsetBottom)
            }
            // clear this listener so insets aren't re-applied
            drawer.setOnApplyWindowInsetsListener(null)
            p1?.consumeSystemWindowInsets()!!
        }

        //init filters adapter
        filtersAdapter = FilterAdapter(this@HomeActivity,
                SourceManager.getSources(this@HomeActivity) as MutableList<Source>, object : FilterAuthoriser {
            override fun requestAuthorisation(sharedElement: View, forSource: Source) {
                val login = Intent(this@HomeActivity, AuthActivity::class.java)
                MorphTransform.addExtras(login,
                        ContextCompat.getColor(this@HomeActivity, R.color.background_dark),
                        sharedElement.height.div(2))
                val options =
                        ActivityOptions.makeSceneTransitionAnimation(this@HomeActivity,
                                sharedElement, getString(R.string.transition_dribbble_login))
                startActivityForResult(login,
                        getAuthSourceRequestCode(forSource), options.toBundle())
            }
        })

        //init dataManager
        dataManager = object : DataManager(this@HomeActivity, filtersAdapter) {
            @SuppressLint("LogConditional")
            override fun onDataLoaded(data: List<ProductItem>) {
                adapter.addAndResort(data)
                emptyState()
            }
        }

        //RecyclerView
        val columns: Int = resources.getInteger(R.integer.num_columns)
        val preloadSizeProvider: ViewPreloadSizeProvider<Product> = ViewPreloadSizeProvider()
        adapter = DataAdapter(this, dataManager, columns, preloadSizeProvider)
        grid.adapter = adapter
//        layoutManager = LinearLayoutManager(this)
        layoutManager = GridLayoutManager(this, columns)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return adapter.getItemColumnSpan(position)
            }
        }
        grid.layoutManager = layoutManager
        grid.addOnScrollListener(toolbarElevation)
        grid.setHasFixedSize(true)
        grid.addItemDecoration(GridItemDividerDecoration(this, R.dimen.divider_height, R.color.divider))
        grid.itemAnimator = SlideInItemAnimator()

        val shotPreloader: RecyclerViewPreloader<Product> = RecyclerViewPreloader<Product>(this,
                adapter, preloadSizeProvider, 4)
        grid.addOnScrollListener(shotPreloader)
        setupTaskDescription()

        //Filters
        filtersList.adapter = filtersAdapter
        filtersList.itemAnimator = FilterAdapter.FilterAnimator()
        filtersAdapter.registerFilterChangedCallback(filtersChangedCallbacks)
        dataManager.loadAllDataSources()
        val callback: ItemTouchHelper.Callback = FilterTouchHelperCallback(filtersAdapter, this)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(filtersList)
        emptyState()

    }

    private val filtersChangedCallbacks: FilterAdapter.FiltersChangedCallbacks = object : FilterAdapter.FiltersChangedCallbacks() {
        override fun onFiltersChanged(changedFilter: Source) {
            if (!changedFilter.active) {
                adapter.removeDataSource(changedFilter.key)
            }
            emptyState()
        }

        override fun onFilterRemoved(removed: Source) {
            adapter.removeDataSource(removed.key)
            emptyState()
        }
    }

    private fun emptyState() {
        if (adapter.itemCount == 0) {
            // if grid is empty check whether we're loading or if no filters are selected
            if (filtersAdapter.enabledSourcesCount > 0) {
                if (connected) {
                    isLoading = connected
                    loading.visibility = View.VISIBLE
                    setNoFiltersEmptyTextVisibility(View.GONE)
                }
            } else {
                isLoading = false
                loading.visibility = View.GONE
                setNoFiltersEmptyTextVisibility(View.VISIBLE)
            }
            toolbar.translationZ = 0f
        } else {
            isLoading = false
            noConnection?.visibility = View.GONE
            loading.visibility = View.GONE
            setNoFiltersEmptyTextVisibility(View.GONE)
        }
    }

    override fun onStart() {
        super.onStart()
        if (filtersAdapter.enabledSourcesCount <= 0) {
            loading.visibility = View.GONE
            setNoFiltersEmptyTextVisibility(View.VISIBLE)
        }
        auth.addAuthStateListener(listener)
    }

    override fun onStop() {
        auth.removeAuthStateListener(listener)
        super.onStop()
    }

    override fun onDestroy() {
        dataManager.cancelLoading()
        super.onDestroy()
    }

    private val toolbarElevation: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            // we want the grid to scroll over the top of the toolbar but for the toolbar items
            // to be clickable when visible. To achieve this we play games with elevation. The
            // toolbar is laid out in front of the grid but when we scroll, we lower it's elevation
            // to allow the content to pass in front (and reset when scrolled to top of the grid)
            if (newState == RecyclerView.SCROLL_STATE_IDLE
                    && layoutManager.findFirstVisibleItemPosition() == 0
                    && layoutManager.findViewByPosition(0).top == grid.paddingTop
                    && toolbar.translationZ != 0.0f) {
                // at top, reset elevation
                toolbar.translationZ = 0.0f
            } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING
                    && toolbar.translationZ != -1.0f
                    && adapter.itemCount != 0) {
                // grid scrolled, lower toolbar to allow content to pass in front
                toolbar.translationZ = -1.0f
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val notifications: MenuItem? = menu?.findItem(R.id.menu_notifications)
        val profile: MenuItem? = menu?.findItem(R.id.menu_profile)
        val login: MenuItem? = menu?.findItem(R.id.menu_login)
        val orders: MenuItem? = menu?.findItem(R.id.menu_orders)

        //Setup premium login text
        if (login != null) {
            if (client.isLoggedIn) {
                login.title = resources.getString(R.string.user_log_out_basic)
            } else {
                login.title = resources.getString(R.string.user_login)
            }
        }

        if (profile != null) {
            profile.isEnabled = client.isLoggedIn
        }

        if (notifications != null) {
            notifications.isEnabled = client.isLoggedIn
        }

        if (orders != null) {
            orders.isEnabled = client.isLoggedIn
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_login -> {
                if (client.isLoggedIn) {
                    val builder = AlertDialog.Builder(this@HomeActivity)
                    builder.setMessage(getString(R.string.logout_prompt))
                    builder.setNegativeButton("Logout", { dialogInterface, _ ->
                        client.logout()
                        Toast.makeText(this, resources.getString(R.string.user_logged_out), Toast
                                .LENGTH_SHORT).show()
                        dialogInterface.dismiss()
                    }).setPositiveButton("Cancel", { dialogInterface, _ ->
                        dialogInterface.cancel()
                    })
                    builder.show()
                } else {
                    val intent = Intent(this, AuthActivity::class.java)
                    FabTransform.addExtras(intent,
                            ContextCompat.getColor(this, R.color.button_accent),
                            R.drawable.ic_shopping_cart_black_24dp)
                    val options = ActivityOptions.makeSceneTransitionAnimation(this, fab,
                            getString(R.string.transition_dribbble_login))
                    startActivityForResult(intent, RC_LOGIN_BASIC, options.toBundle())
                }
                true
            }
            R.id.menu_profile -> {
                startActivity(Intent(this@HomeActivity, ProfileActivity::class.java))
                true
            }
            R.id.menu_store -> {
                //launch google maps to display the Accra Mall Store
                val mapUri = Uri.parse("geo: 5.6227348,-0.1743774")
                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                mapIntent.`package` = "com.google.android.apps.maps"
                startActivity(mapIntent)
                true
            }
            R.id.menu_filter -> {
                drawer.openDrawer(GravityCompat.END)
                true
            }
            R.id.menu_notifications -> {
                //See what's new in The Phoenix
                startActivity(Intent(this@HomeActivity, NotificationsActivity::class.java))
                true
            }
            R.id.menu_orders -> {
                //See what's new in The Phoenix
                startActivity(Intent(this@HomeActivity, LiveOrdersActivity::class.java))
                true
            }
            R.id.menu_search -> {
                val searchMenuView: View = toolbar.findViewById(R.id.menu_search)
                val options = ActivityOptions.makeSceneTransitionAnimation(this, searchMenuView,
                        getString(R.string.transition_search_back)).toBundle()
                startActivityForResult(Intent(this@HomeActivity, SearchActivity::class.java), RC_SEARCH,
                        options)
                true
            }
            R.id.menu_about -> {
                startActivity(Intent(this@HomeActivity, AboutActivity::class.java),
                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun animateToolbar() {
        // this is gross but toolbar doesn't expose it's children to animate them :(
        val t = toolbar.getChildAt(0)
        if (t != null && t is TextView) {

            // fade in and space out the title.  Animating the letterSpacing performs horribly so
            // fake it by setting the desired letterSpacing then animating the scaleX ¯\_(ツ)_/¯
            t.alpha = 0f
            t.scaleX = 0.8f

            t.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .setStartDelay(300)
                    .setDuration(600).interpolator = AnimUtils.getFastOutSlowInInterpolator(this@HomeActivity)
        }
    }

    private fun setupTaskDescription() {
        val overviewIcon = BitmapFactory.decodeResource(resources, applicationInfo.icon)
        setTaskDescription(ActivityManager.TaskDescription(getString(R.string.app_name),
                overviewIcon,
                ContextCompat.getColor(this, R.color.primary)))
        overviewIcon.recycle()
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        if (data == null || resultCode != RESULT_OK
                || !data.hasExtra(DetailsActivity.RESULT_EXTRA_SHOT_ID)) return
        // When reentering, if the shared element is no longer on screen (e.g. after an
        // orientation change) then scroll it into view.
        val sharedShotId = data.getLongExtra(DetailsActivity.RESULT_EXTRA_SHOT_ID, -1L)
        if (sharedShotId != -1L                                             // returning from a shot
                && adapter.dataItemCount > 0                           // grid populated
                && grid.findViewHolderForItemId(sharedShotId) == null) {    // view not attached
            val position = adapter.getItemPosition(sharedShotId)
            if (position == RecyclerView.NO_POSITION) return

            // delay the transition until our shared element is on-screen i.e. has been laid out
            postponeEnterTransition()
            grid.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(p0: View?, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int, p8: Int) {
                    grid.removeOnLayoutChangeListener(this)
                    startPostponedEnterTransition()
                }
            })
            grid.scrollToPosition(position)
            toolbar.translationZ = -1f
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_LOGIN_BASIC -> {
                if (resultCode == RESULT_OK) {
                    showFab()
                }
            }
            RC_AUTH_SOURCE_BUSINESS -> {
                if (resultCode == RESULT_OK) {
                    filtersAdapter.enableFilterByKey(SourceManager.SOURCE_BUSINESS, this)
                }
            }
            RC_AUTH_SOURCE_FAV -> {
                if (resultCode == RESULT_OK) {
                    filtersAdapter.enableFilterByKey(SourceManager.SOURCE_FAVORITE, this)
                }
            }
            RC_AUTH_SOURCE_STUDENT -> {
                if (resultCode == RESULT_OK) {
                    filtersAdapter.enableFilterByKey(SourceManager.SOURCE_STUDENT, this)
                }
            }
            RC_AUTH_SOURCE_HEALTH -> {
                if (resultCode == RESULT_OK) {
                    filtersAdapter.enableFilterByKey(SourceManager.SOURCE_HEALTH, this)
                }
            }
            RC_SEARCH -> {
                // reset the search icon which we hid
                val searchMenuView: View? = toolbar.findViewById(R.id.menu_search)
                if (searchMenuView != null) {
                    searchMenuView.alpha = 1.0f
                }
                if (resultCode == SearchActivity.RESULT_CODE_SAVE) {
                    val query: String? = data?.getStringExtra(SearchActivity.EXTRA_QUERY)
                    if (query == null || TextUtils.isEmpty(query)) return
                    Timber.d(query)
                    var source: Source? = null
                    var newSource = false
                    if (data.getBooleanExtra(SearchActivity.EXTRA_SAVE, false)) {
                        source = Source.PhoenixSearchSource(query, true)
                        newSource = filtersAdapter.addFilter(source)
                    }
                    if (newSource) highlightNewSources(source)
                }
            }
            RC_CART -> {
                if (resultCode == RESULT_OK) {
                    showFab()
                }
            }
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean(STATE_LOADING, isLoading)
    }

    private fun showFab() {
        fab.alpha = 0f
        fab.scaleX = 0f
        fab.scaleY = 0f
        fab.translationY = (fab.height / 2).toFloat()
        fab.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(300L)
                .setInterpolator(AnimUtils.getLinearOutSlowInInterpolator(this@HomeActivity))
                .start()
    }

    private fun setNoFiltersEmptyTextVisibility(visibility: Int) {
        if (visibility == View.VISIBLE) {
            if (noFiltersEmptyText == null) {
                // create the no filters empty text
                val stub = findViewById<ViewStub>(R.id.stub_no_filters)
                noFiltersEmptyText = stub.inflate() as TextView
                val emptyText = getText(R.string.no_filters_selected) as SpannedString
                val ssb = SpannableStringBuilder(emptyText)
                val annotations = emptyText.getSpans(0, emptyText.length, Annotation::class.java)
                if (annotations != null && annotations.isNotEmpty()) {
                    for (i in annotations.indices) {
                        val annotation = annotations[i]
                        if (annotation.key == "src") {
                            // image span markup
                            val name = annotation.value
                            val id = resources.getIdentifier(name, null, packageName)
                            if (id == 0) continue
                            ssb.setSpan(ImageSpan(this, id,
                                    ImageSpan.ALIGN_BASELINE),
                                    emptyText.getSpanStart(annotation),
                                    emptyText.getSpanEnd(annotation),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        } else if (annotation.key == "foregroundColor") {
                            // foreground color span markup
                            val name = annotation.value
                            val id = resources.getIdentifier(name, null, packageName)
                            if (id == 0) continue
                            ssb.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, id)),
                                    emptyText.getSpanStart(annotation),
                                    emptyText.getSpanEnd(annotation),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }
                }
                noFiltersEmptyText!!.text = ssb
                noFiltersEmptyText!!.setOnClickListener { _ -> drawer.openDrawer(GravityCompat.END) }
            }
            noFiltersEmptyText!!.visibility = visibility
        } else if (noFiltersEmptyText != null) {
            noFiltersEmptyText!!.visibility = visibility
        }

    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.END))
            drawer.closeDrawer(GravityCompat.END)
        else {
            super.onBackPressed()
            //finishAfterTransition()
        }
    }

    override fun onResume() {
        super.onResume()
        //Add login listener
        //todo: seems not to respond to auth state
        client.addLoginStatusListener(filtersAdapter)

        //There seems to be a problem with the AuthStateListener so we need to manually invalidate
        //the options menu
        invalidateOptionsMenu()
        checkConnectivity()
    }

    override fun onPause() {
        client.removeLoginStatusListener(filtersAdapter)
        if (monitoringConnectivity) {
            val connectivityManager: ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(connectivityCallback)
            monitoringConnectivity = false
        }
        super.onPause()
    }

    private fun checkConnectivity() {
        val connectivityManager: ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        connected = activeNetworkInfo != null && activeNetworkInfo.isConnected
        if (!connected) {
            loading.visibility = View.GONE
            if (noConnection == null) {
                val stub: ViewStub = findViewById(R.id.stub_no_connection)
                noConnection = stub.inflate() as ImageView
            }
            val avd: AnimatedVectorDrawable? = getDrawable(R.drawable.avd_no_connection) as
                    AnimatedVectorDrawable
            if (noConnection != null && avd != null) {
                noConnection!!.setImageDrawable(avd)
                avd.start()
            }

            connectivityManager.registerNetworkCallback(
                    NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                    connectivityCallback)
            monitoringConnectivity = true
        }
    }

    private val connectivityCallback: ConnectivityManager.NetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network?) {
            connected = true
            if (adapter.dataItemCount != 0) return
            runOnUiThread({
                TransitionManager.beginDelayedTransition(drawer)
                noConnection?.visibility = View.GONE
                loading.visibility = View.VISIBLE
                dataManager.loadAllDataSources()
            })
        }

        override fun onLost(network: Network?) {
            connected = false
        }
    }

    /**
     * get user authentication to enable access to filters from
     * @param filter */
    private fun getAuthSourceRequestCode(filter: Source): Int {
        when (filter.key) {
            SourceManager.SOURCE_HEALTH -> return RC_AUTH_SOURCE_HEALTH
            SourceManager.SOURCE_BUSINESS -> return RC_AUTH_SOURCE_BUSINESS
            SourceManager.SOURCE_FAVORITE -> return RC_AUTH_SOURCE_FAV
            SourceManager.SOURCE_STUDENT -> return RC_AUTH_SOURCE_STUDENT
        }
        throw InvalidParameterException()
    }

    private fun highlightNewSources(vararg sources: Source?) {
        val closeDrawerRunnable = Runnable {
            drawer.closeDrawer(GravityCompat.END)
        }

        drawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            private val filtersTouch: View.OnTouchListener = View.OnTouchListener { _, _ ->
                drawer.removeCallbacks(closeDrawerRunnable)
                false
            }

            override fun onDrawerClosed(drawerView: View) {
                // reset
                filtersList.setOnTouchListener(null)
                drawer.removeDrawerListener(this)
            }

            override fun onDrawerOpened(drawerView: View) {
                // scroll to the new item(s) and highlight them
                val filterPositions: ArrayList<Int> = ArrayList(sources.size)
                sources
                        .filterNotNull()
                        .mapTo(filterPositions) { filtersAdapter.getFilterPosition(it) }
                val scrollTo: Int = Collections.max(filterPositions)
                filtersList.smoothScrollToPosition(scrollTo)
                for (position in filterPositions) {
                    filtersAdapter.highlightFilter(position)
                }
                filtersList.setOnTouchListener(filtersTouch)
            }

            override fun onDrawerStateChanged(newState: Int) {
                // if the user interacts with the drawer manually then don't auto-close
                if (newState == DrawerLayout.STATE_DRAGGING) {
                    drawer.removeCallbacks(closeDrawerRunnable)
                }
            }

        })
        drawer.openDrawer(GravityCompat.END)
        drawer.postDelayed(closeDrawerRunnable, 2000L)
    }

    companion object {
        private const val RC_SEARCH = 1234
        private const val STATE_LOADING = "STATE_LOADING"
        private const val RC_LOGIN_BASIC = 450
        private const val RC_CART = 500
        private const val RC_AUTH_SOURCE_FAV = 19
        private const val RC_AUTH_SOURCE_BUSINESS = 98
        private const val RC_AUTH_SOURCE_STUDENT = 999
        private const val RC_AUTH_SOURCE_HEALTH = 1099
    }

}
