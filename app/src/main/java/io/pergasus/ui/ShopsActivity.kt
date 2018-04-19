/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.QuerySnapshot
import io.pergasus.BuildConfig
import io.pergasus.R
import io.pergasus.api.PhoenixClient
import io.pergasus.api.PhoenixUtils
import io.pergasus.api.ShopDataManager
import io.pergasus.data.Follow
import io.pergasus.data.Product
import io.pergasus.data.Shop
import io.pergasus.ui.recyclerview.SlideInItemAnimator
import io.pergasus.ui.transitions.MorphTransform
import io.pergasus.ui.widget.ElasticDragDismissFrameLayout
import io.pergasus.util.HtmlUtils
import io.pergasus.util.ViewUtils
import io.pergasus.util.bindView
import io.pergasus.util.glide.GlideApp
import timber.log.Timber
import java.text.NumberFormat
import java.util.*


class ShopsActivity : Activity() {

    private val draggableFrame: ElasticDragDismissFrameLayout by bindView(R.id.draggable_frame)
    private val container: ViewGroup by bindView(R.id.container)
    private val name: TextView by bindView(R.id.shop_name)
    private val about: TextView by bindView(R.id.shop_bio)
    private val avatar: ImageView by bindView(R.id.avatar)
    private val follow: Button by bindView(R.id.follow)
    private val productsCount: TextView by bindView(R.id.products_count)
    private val followersCount: TextView by bindView(R.id.followers_count)
    private val grid: RecyclerView by bindView(R.id.shop_products)
    private val loading: ProgressBar by bindView(R.id.loading)

    private var following: Boolean = false
    private lateinit var chromeFader: ElasticDragDismissFrameLayout.SystemChromeFader
    private var followerCount: Int = 0
    private var shop: Shop? = null
    private lateinit var prefs: PhoenixClient

    private var adapter: SearchDataAdapter? = null
    private var layoutManager: GridLayoutManager? = null
    private var dataManager: ShopDataManager? = null
    private var columns: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shops)

        chromeFader = ElasticDragDismissFrameLayout.SystemChromeFader(this)
        prefs = PhoenixClient(this@ShopsActivity)

        val intent = intent
        if (intent.hasExtra(EXTRA_SHOP_NAME)) {
            val extra = intent.getStringExtra(EXTRA_SHOP_NAME)
            name.text = String.format("%s (Loading)", extra)
            if (intent.hasExtra(EXTRA_SHOP_KEY)) {
                val key = intent.getStringExtra(EXTRA_SHOP_KEY)
                //Load shop by key, if any else load by name
                if (key.isNullOrEmpty()) loadShop(extra)
                else loadShop(key)
            }
        }

        // setup immersive mode i.e. draw behind the system chrome & adjust insets
        draggableFrame.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            draggableFrame.systemUiVisibility = (View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        }

        draggableFrame.setOnApplyWindowInsetsListener({ _, insets ->
            val params = draggableFrame.layoutParams as ViewGroup.MarginLayoutParams
            params.leftMargin += insets.systemWindowInsetLeft   //left margin
            params.rightMargin += insets.systemWindowInsetRight   //right margin
            (avatar.layoutParams as ViewGroup.MarginLayoutParams).topMargin += insets.systemWindowInsetTop
            ViewUtils.setPaddingTop(container, insets.systemWindowInsetTop)
            ViewUtils.setPaddingBottom(grid, insets.systemWindowInsetBottom)
            // clear this listener so insets aren't re-applied
            draggableFrame.setOnApplyWindowInsetsListener(null)
            return@setOnApplyWindowInsetsListener insets
        })

        setExitSharedElementCallback(SearchDataAdapter.createSharedElementReenterCallback(this))
    }

    private fun loadShop(extra: String?) {
        if (extra == null) return
        prefs.db.collection(PhoenixUtils.DB_PREFIX + "/" + PhoenixUtils.SHOP_REF)
                .get()
                .addOnFailureListener { exception ->
                    Toast.makeText(this@ShopsActivity, exception.localizedMessage,
                            Toast.LENGTH_SHORT).show()
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        for (shops in task.result.documents) {
                            if (shops.exists()) {
                                val obj = shops.toObject(Shop::class.java)
                                if (obj.name == extra || obj.key == extra) {
                                    shop = obj  //Assign shop to object
                                    bindShop()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this@ShopsActivity, task.exception?.localizedMessage,
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun bindShop() {
        Timber.d("bindShop called successfully")
        val res = resources
        val nf: NumberFormat = NumberFormat.getInstance()

        //Load image from url
        GlideApp.with(this)
                .load(shop?.logo)
                .apply(RequestOptions().circleCrop())
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                .apply(RequestOptions().error(R.drawable.ic_player))
                .apply(RequestOptions().override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL))
                .apply(RequestOptions().placeholder(R.drawable.avatar_placeholder))
                .transition(withCrossFade())
                .into(avatar)

        //Name
        name.text = shop?.name

        //Bio
        if (shop?.motto == null) {
            about.visibility = View.GONE
        } else {
            HtmlUtils.parseAndSetText(about, shop?.motto)
        }

        setFollowersCount(shop?.followers_count)

        //Stats
        productsCount.text = res.getQuantityString(R.plurals.purchases, shop?.products_count!!
                .toLong().toInt(), nf.format(shop?.products_count!!))

        //Follow button
        follow.setOnClickListener(doFollow)

        //Add ripple and animated drawable effect to textViews
        followersCount.setOnClickListener {
            actionClick(followersCount)
        }
        productsCount.setOnClickListener {
            actionClick(productsCount)
        }

        dataManager = object : ShopDataManager(this@ShopsActivity, shop!!) {
            override fun onDataLoaded(data: List<Product>) {
                if (data.isNotEmpty()) {
                    if (adapter?.dataItemCount == 0) {
                        loading.visibility = View.GONE
                        ViewUtils.setPaddingTop(grid, productsCount.bottom)
                    }
                    adapter?.addAndResort(data)
                }
            }
        }

        columns = resources.getInteger(R.integer.num_columns)
        //RecyclerView
        val preloadSizeProvider: ViewPreloadSizeProvider<Product> = ViewPreloadSizeProvider()
        adapter = SearchDataAdapter(this, dataManager, columns, preloadSizeProvider)
        setExitSharedElementCallback(SearchDataAdapter.createSharedElementReenterCallback(this))
        grid.adapter = adapter
        grid.itemAnimator = SlideInItemAnimator()
        grid.visibility = View.VISIBLE
        layoutManager = GridLayoutManager(this, columns)
        layoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return adapter?.getItemColumnSpan(position)!!
            }
        }
        grid.layoutManager = layoutManager
        grid.setHasFixedSize(true)
        val shotPreloader: RecyclerViewPreloader<Product> = RecyclerViewPreloader<Product>(this,
                adapter!!, preloadSizeProvider, 4)
        grid.addOnScrollListener(shotPreloader)
        // forward on any clicks above the first item in the grid (i.e. in the paddingTop)
        // to 'pass through' to the view behind
        grid.setOnTouchListener(View.OnTouchListener { _, event ->
            val firstVisible = layoutManager?.findFirstVisibleItemPosition()!!
            if (firstVisible > 0) return@OnTouchListener false

            // if no data loaded then pass through
            if (adapter?.dataItemCount == 0) {
                return@OnTouchListener container.dispatchTouchEvent(event)
            }

            val vh = grid.findViewHolderForAdapterPosition(0) ?: return@OnTouchListener false
            val firstTop = vh.itemView.top
            if (event?.y!! < firstTop) {
                return@OnTouchListener container.dispatchTouchEvent(event)
            }
            return@OnTouchListener false
        })

        if (dataManager?.phoenixClient!!.isLoggedIn) {
            //Query db for the current user
            dataManager?.phoenixClient!!
                    .db.document(PhoenixUtils.FOLLOW_REF)
                    .collection(shop?.key!!)
                    .orderBy("timestamp")
                    .addSnapshotListener(this@ShopsActivity, EventListener<QuerySnapshot?> { p0, p1 ->
                        if (p1 != null) {
                            Timber.d(p1.localizedMessage)
                            return@EventListener
                        }
                        if (p0 != null) {
                            for (data in p0.documents) {
                                if (data.exists()) {
                                    val follow1 = data.toObject(Follow::class.java)
                                    if (follow1.customer?.key == prefs.customer.key) {
                                        //Set following to be true
                                        TransitionManager.beginDelayedTransition(container)
                                        follow.setText(R.string.following)
                                        follow.isActivated = true
                                    }
                                }
                            }
                        }
                    })
        }

        if (shop?.products_count!! > 0) {
            dataManager?.loadData()
        } else {
            loading.visibility = View.GONE
        }
    }

    private fun actionClick(view: TextView) {
        (view.compoundDrawables[1] as AnimatedVectorDrawable).start()
        if (view.id == R.id.followers_count) {
            // Show a list of followers for this shopping outlet
            UserSheet.start(this@ShopsActivity, shop!!)
        }
    }

    private fun setFollowersCount(l: Long?) {
        if (l == null) return
        followerCount = l.toInt()
        followersCount.text = resources.getQuantityString(R.plurals.followers, followerCount,
                NumberFormat.getInstance().format(followerCount))
        if (followerCount == 0) {
            followersCount.background = null
        }
    }

    private val doFollow: View.OnClickListener = View.OnClickListener {
        if (prefs.isLoggedIn) {
            if (following) {
                prefs.db.document(PhoenixUtils.FOLLOW_REF)
                        .collection(shop?.key!!)
                        .document(prefs.customer.key!!)
                        .delete()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                updateFollowers(task, shop?.followers_count!!.minus(1))
                            }
                        }
                        .addOnFailureListener { exception ->
                            if (BuildConfig.DEBUG) {
                                Timber.d(exception.localizedMessage)
                            }
                        }
                following = false
                TransitionManager.beginDelayedTransition(container)
                follow.setText(R.string.follow)
                follow.isActivated = false
                setFollowersCount((followerCount - 1).toLong())
            } else {
                //Get user instance and create a map from it
                val customer = prefs.customer
                //Create follow instance
                val hashMap = hashMapOf(
                        Pair<String, Any?>("id", System.currentTimeMillis()),
                        Pair<String, Any?>("customer", customer.toHashMap(customer)),
                        Pair<String, Any?>("shop", shop?.toHashMap(shop!!)),
                        Pair<String, Any?>("timestamp", Date(System.currentTimeMillis()))
                )

                //Push to database
                prefs.db.document(PhoenixUtils.FOLLOW_REF)
                        .collection(shop?.key!!)
                        .document(prefs.customer.key!!)
                        .set(hashMap)
                        .addOnFailureListener { exception ->
                            if (BuildConfig.DEBUG) {
                                Timber.d(exception.localizedMessage)
                            }
                        }.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                updateFollowers(task, shop?.followers_count!!.plus(1))
                            } else {
                                if (BuildConfig.DEBUG) {
                                    Timber.d(task.exception?.localizedMessage)
                                }
                            }
                        }
                following = true
                TransitionManager.beginDelayedTransition(container)
                follow.setText(R.string.following)
                follow.isActivated = true
                setFollowersCount((followerCount + 1).toLong())

            }
        } else {
            val login = Intent(this, AuthActivity::class.java)
            MorphTransform.addExtras(login,
                    ContextCompat.getColor(this, R.color.button_accent),
                    resources.getDimensionPixelSize(R.dimen.dialog_corners))
            val options = ActivityOptions.makeSceneTransitionAnimation(this, follow, getString(R.string.transition_dribbble_login))
            startActivity(login, options.toBundle())
        }
    }

    private fun updateFollowers(task: Task<Void>, value: Long) {
        //Add to shop's follower's count as well
        //Ref: /phoenix/web/shops/{shop_key}
        prefs.db.document(PhoenixUtils.DB_PREFIX + "/" + PhoenixUtils.SHOP_REF + "/" + shop?.key)
                .update("followers_count", value)
                .addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        if (BuildConfig.DEBUG) {
                            Timber.d("Followers updated")
                        }
                    } else {
                        if (BuildConfig.DEBUG) {
                            Timber.d(task.exception?.localizedMessage)
                        }
                    }
                }
    }

    override fun onResume() {
        super.onResume()
        draggableFrame.addListener(chromeFader)
    }

    override fun onPause() {
        draggableFrame.removeListener(chromeFader)
        super.onPause()
    }

    override fun onDestroy() {
        if (dataManager != null) {
            dataManager!!.cancelLoading()
        }
        super.onDestroy()
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        if (data == null || resultCode != RESULT_OK
                || !data.hasExtra(DetailsActivity.RESULT_EXTRA_SHOT_ID)) return
        // When reentering, if the shared element is no longer on screen (e.g. after an
        // orientation change) then scroll it into view.
        val sharedShotId = data.getLongExtra(DetailsActivity.RESULT_EXTRA_SHOT_ID, -1L)
        if (sharedShotId != -1L                                             // returning from a shot
                && adapter?.dataItemCount!! > 0                           // grid populated
                && grid.findViewHolderForItemId(sharedShotId) == null) {    // view not attached
            val position = adapter?.getItemPosition(sharedShotId)
            if (position == RecyclerView.NO_POSITION) return

            // delay the transition until our shared element is on-screen i.e. has been laid out
            postponeEnterTransition()
            grid.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(p0: View?, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int, p8: Int) {
                    grid.removeOnLayoutChangeListener(this)
                    startPostponedEnterTransition()
                }
            })
            if (position != null) {
                grid.scrollToPosition(position)
            }
        }
    }

    companion object {
        const val EXTRA_SHOP_NAME = "EXTRA_SHOP_NAME"
        const val EXTRA_SHOP_KEY = "EXTRA_SHOP_KEY"

    }
}
