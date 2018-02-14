/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.ActivityOptions
import android.app.assist.AssistContent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.NonNull
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.graphics.Palette
import android.support.v7.widget.RecyclerView
import android.text.Spanned
import android.text.TextUtils
import android.text.format.DateUtils
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionListenerAdapter
import android.transition.TransitionManager
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import io.pergasus.BuildConfig
import io.pergasus.R
import io.pergasus.api.PhoenixClient
import io.pergasus.api.PhoenixUtils
import io.pergasus.data.Comment
import io.pergasus.data.Product
import io.pergasus.ui.recyclerview.InsetDividerDecoration
import io.pergasus.ui.recyclerview.SlideInItemAnimator
import io.pergasus.ui.transitions.FabTransform
import io.pergasus.ui.widget.*
import io.pergasus.util.*
import io.pergasus.util.AnimUtils.getFastOutSlowInInterpolator
import io.pergasus.util.customtabs.CustomTabActivityHelper
import io.pergasus.util.glide.GlideApp
import io.pergasus.util.glide.getBitmap
import org.jetbrains.annotations.Nullable
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("LogConditional")
/** Details of each category selected is shown here */
class DetailsActivity : Activity() {

    //Widgets
    private val draggableFrame: ElasticDragDismissFrameLayout by bindView(R.id.draggable_frame)
    private val back: ImageButton by bindView(R.id.back)
    private val imageView: ParallaxScrimageView by bindView(R.id.shot)
    private val commentsList: RecyclerView by bindView(R.id.dribbble_comments)
    private val fab: FABToggle by bindView(R.id.fab_heart)

    //Customized widgets
    private lateinit var shotDescription: View
    private lateinit var shotSpacer: View
    private lateinit var likeCount: Button
    private lateinit var viewCount: Button
    private lateinit var share: Button
    private lateinit var playerAvatar: ImageView
    private var enterComment: EditText? = null
    private var postComment: ImageButton? = null
    private lateinit var title: View
    private lateinit var price: View
    private lateinit var description: View
    private lateinit var playerName: TextView
    private lateinit var shotTimeAgo: TextView
    private var commentFooter: View? = null
    private lateinit var userAvatar: ImageView
    private lateinit var chromeFader: ElasticDragDismissFrameLayout.SystemChromeFader
    private lateinit var commentAnimator: CommentAnimator
    private lateinit var adapter: CommentsAdapter

    //Others
    private var fabOffset: Int = 0
    private var largeAvatarSize: Float = 0.0f
    private var cardElevation: Float = 0.0f
    private var shot: Product? = null
    private lateinit var prefs: PhoenixClient
    private var performingLike: Boolean = false
    private var allowComment: Boolean = false
    private val data = ArrayList<Comment>(0)    //Empty arrayList

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        prefs = PhoenixClient(this@DetailsActivity)
        largeAvatarSize = resources.getDimension(R.dimen.large_avatar_size)
        cardElevation = resources.getDimension(R.dimen.z_card)

        chromeFader = object : ElasticDragDismissFrameLayout.SystemChromeFader(this) {
            override fun onDragDismissed() {
                setResultAndFinish()
            }
        }

        back.setOnClickListener { setResultAndFinish() }
        fab.setOnClickListener {
            if (prefs.isLoggedIn) {
                fab.toggle()
                appendCart()
            } else {
                val intent = Intent(this@DetailsActivity, AuthActivity::class.java)
                FabTransform.addExtras(intent,
                        ContextCompat.getColor(this@DetailsActivity, R.color.button_accent),
                        R.drawable.ic_heart_empty_56dp)
                val options = ActivityOptions.makeSceneTransitionAnimation(this@DetailsActivity, fab,
                        getString(R.string.transition_dribbble_login))
                startActivityForResult(intent, RC_LOGIN_LIKE, options.toBundle())
            }
        }

        //SET UP VIEW FOR DETAILS
        shotDescription = layoutInflater.inflate(R.layout.dribbble_shot_description,
                commentsList, false)
        shotSpacer = shotDescription.findViewById(R.id.shot_spacer)
        title = shotDescription.findViewById(R.id.shot_title)
        description = shotDescription.findViewById(R.id.shot_description)
        price = shotDescription.findViewById(R.id.shot_price)
        likeCount = shotDescription.findViewById(R.id.shot_like_count) as Button
        viewCount = shotDescription.findViewById(R.id.shot_view_count) as Button
        share = (shotDescription.findViewById(R.id.shot_share_action) as Button)
        playerName = shotDescription.findViewById(R.id.player_name) as TextView
        playerAvatar = shotDescription.findViewById(R.id.player_avatar) as ImageView
        shotTimeAgo = shotDescription.findViewById(R.id.shot_time_ago) as TextView

        setupCommenting()
        //RECYCLERVIEW
        commentsList.addOnScrollListener(scrollListener)
        commentsList.onFlingListener = flingListener

        val intent = intent
        if (intent.hasExtra(EXTRA_SHOT)) {
            shot = intent.getParcelableExtra(EXTRA_SHOT)
            bindProduct(true)
        } /*else if (intent.data != null) {
            val url: HttpUrl? = HttpUrl.parse(intent.dataString)
            if (url != null && url.pathSize() == 2 && url.pathSegments()[0] == "product") {
                try {
                    //val shotPath: String = url.pathSegments()[1]
                    //val id: Long = parseLong(shotPath.substring(0, shotPath.indexOf("-")))

                } catch (e: NumberFormatException) {
                    reportUrlError()
                } catch (e: StringIndexOutOfBoundsException) {
                    reportUrlError()
                }
            }
        }*/

    }

    //Setup details with the provided data from intent
    private fun bindProduct(postponeTransition: Boolean) {
        if (shot == null) return
        val res = resources

        GlideApp.with(this)
                .load(shot!!.url)
                .listener(shotLoadListener)
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                .apply(RequestOptions().priority(Priority.IMMEDIATE))
                .apply(RequestOptions().optionalFitCenter())
                .transition(withCrossFade())
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .into(imageView)
        val shotClick: View.OnClickListener = View.OnClickListener {
            if (shot != null)
                openLink(shot!!.url)
        }
        imageView.setOnClickListener(shotClick)
        shotSpacer.setOnClickListener(shotClick)

        if (postponeTransition) postponeEnterTransition()

        imageView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                imageView.viewTreeObserver.removeOnPreDrawListener(this)
                calculateFabPosition()
                if (postponeTransition) startPostponedEnterTransition()
                return true
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (title as FabOverlapTextView).setText(shot?.name)
        } else {
            (title as TextView).text = shot?.name
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (price as FabOverlapTextView).setText(NumberFormat.getCurrencyInstance(Locale.US)
                    .format((shot?.price)?.toLong()).toString().toLowerCase())
        } else {
            (price as TextView).text = NumberFormat.getCurrencyInstance(Locale.US)
                    .format((shot?.price)?.toLong()).toString().toLowerCase()
        }

        if (!TextUtils.isEmpty(shot!!.description)) {
            val descText: Spanned = shot!!.getParsedDescription(
                    ContextCompat.getColorStateList(this, R.color.dribbble_links)!!,
                    ContextCompat.getColor(this, R.color.dribbble_link_highlight))!!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                (description as FabOverlapTextView).setText(shot?.description)
            } else {
                HtmlUtils.setTextWithNiceLinks(description as TextView, descText)
            }
        } else {
            description.visibility = View.GONE
        }

        val nf = NumberFormat.getInstance()

        //Like button
        likeCount.text = res.getQuantityString(R.plurals.likes, 10000, nf.format(10000))
        likeCount.setOnClickListener {
            (likeCount.compoundDrawables[1] as AnimatedVectorDrawable).start()
        }

        //View button
        viewCount.text = res.getQuantityString(R.plurals.views, 13000, nf.format(13000))
        viewCount.setOnClickListener {
            (viewCount.compoundDrawables[1] as AnimatedVectorDrawable).start()
        }

        //Share Button
        share.setOnClickListener {
            (share.compoundDrawables[1] as AnimatedVectorDrawable).start()
            ShareProductTask(this, shot!!).execute()
        }

        if (shot != null) {
            playerName.text = "—${shot?.shop?.toLowerCase()}"
            GlideApp.with(this)
                    .load(shot?.logo)
                    .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                    .apply(RequestOptions().circleCrop())
                    .apply(RequestOptions().placeholder(R.drawable.avatar_placeholder))
                    .transition(withCrossFade())
                    .apply(RequestOptions().override(largeAvatarSize.toInt(), largeAvatarSize.toInt()))
                    .into(playerAvatar)

            if (shot?.timestamp != null) {
                shotTimeAgo.text = DateUtils.getRelativeTimeSpanString(shot!!.timestamp!!.time,
                        System.currentTimeMillis(),
                        DateUtils.SECOND_IN_MILLIS).toString().toLowerCase()
            } else {
                for (i in 0 until shot?.brand!!.size) {
                    //Append brand names to product's shop
                    shotTimeAgo.text = shot?.brand?.get(i)?.toLowerCase() + ", "
                }
            }
            val shopClick: View.OnClickListener = View.OnClickListener {
                val details = Intent(this@DetailsActivity, ShopsActivity::class.java)
                details.putExtra(ShopsActivity.EXTRA_SHOP_NAME, shot?.shop)
                val options = ActivityOptions.makeSceneTransitionAnimation(this@DetailsActivity,
                        playerAvatar, getString(R.string.transition_player_avatar))
                startActivity(details, options.toBundle())
            }
            playerAvatar.setOnClickListener(shopClick)
            playerName.setOnClickListener(shopClick)
        } else {
            playerName.visibility = View.GONE
            playerAvatar.visibility = View.GONE
            shotTimeAgo.visibility = View.GONE
        }

        //Bind Recyclerview's adapter to the recyclerview
        commentAnimator = CommentAnimator()
        commentsList.itemAnimator = commentAnimator
        adapter = CommentsAdapter(shotDescription, commentFooter, 0L,
                resources.getInteger(R.integer.comment_expand_collapse_duration).toLong())
        commentsList.adapter = adapter
        commentsList.addItemDecoration(InsetDividerDecoration(
                CommentViewHolder::class.java,
                res.getDimensionPixelSize(R.dimen.divider_height),
                res.getDimensionPixelSize(R.dimen.keyline_1),
                ContextCompat.getColor(this, R.color.divider)))
        loadComments()
        if (prefs.isLoggedIn) checkLiked()

    }

    //Load comments form database
    private fun loadComments() {
        if (data.isNotEmpty()) data.clear()
        prefs.db.document(PhoenixUtils.COMMENT_REF)
                .collection(shot?.id?.toString()!!)
                .whereEqualTo("id", shot?.id)
                .get()
                .addOnCompleteListener({ task ->
                    if (task.isSuccessful) {
                        for (document in task.result.documents) {
                            if (document.exists()) {
                                val comment: Comment? = document.toObject(Comment::class.java)
                                if (comment != null) {
                                    data.add(comment)   //Add comment
                                }
                            }
                        }
                         if (data.isNotEmpty()){
                            adapter.addComments(data)
                         }
                    }
                }).addOnFailureListener { e ->
            Log.d(this@DetailsActivity.localClassName,
                    "Exception from comments: ${e.localizedMessage}")
        }
    }

    /** Post [Comment] to the database */
    fun postComment(view: View) {
        //Verify user login status
        if (prefs.isLoggedIn) {
            //Perform the following if user is logged in
            if (TextUtils.isEmpty(enterComment?.text)) return
            enterComment?.isEnabled = false
            //Generate hash map for the comment itself
            val comment = Comment(
                    shot?.id!!,
                    enterComment?.text?.toString()!!,
                    0,
                    shot?.url!!,
                    prefs.customer,
                    Date(System.currentTimeMillis())
            )
            //Push comment to db
            prefs.db.document(PhoenixUtils.COMMENT_REF)
                    .collection(shot?.id?.toString()!!)
                    .document()
                    .set(comment.toHashMap(comment))
                    .addOnFailureListener { e -> Log.d(TAG, "Exception on comment post: ${e.localizedMessage}") }
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            loadComments()
                            enterComment?.text?.clear()
                            enterComment?.isEnabled = true
                        } else {
                            Log.d(TAG, "Exception on comment post: ${task.exception?.toString()}")
                        }
                    }

        } else {
            //Prompt user to login
            val login = Intent(this@DetailsActivity, AuthActivity::class.java)
            FabTransform.addExtras(login, ContextCompat.getColor(
                    this@DetailsActivity, R.color.background_light), R.drawable.ic_comment_add)
            val options = ActivityOptions.makeSceneTransitionAnimation(
                    this@DetailsActivity, postComment, getString(R.string.transition_dribbble_login))
            startActivityForResult(login, RC_LOGIN_COMMENT, options.toBundle())
        }
    }

    //Set status bar color from the most dominant color of the image
    //The color is picked from the top-most part of the image rather than the whole image
    private val shotLoadListener = object : RequestListener<Drawable?> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable?>?,
                                  isFirstResource: Boolean): Boolean {
            return false
        }

        /** Shows loaded resource for image */
        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable?>?,
                                     dataSource: DataSource?, isFirstResource: Boolean): Boolean {
            val bitmap: Bitmap? = resource?.getBitmap()
            val twentyFourDip: Int = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24.0f,
                    this@DetailsActivity.resources.displayMetrics)).toInt()
            if (bitmap != null) {
                Palette.from(bitmap)
                        .maximumColorCount(3)
                        .clearFilters()
                        .setRegion(0, 0, bitmap.width - 1, twentyFourDip)
                        .generate { palette ->
                            val isDark: Boolean
                            val lightness: Int = ColorUtils.isDark(palette)
                            isDark = if (lightness == ColorUtils.LIGHTNESS_UNKNOWN) {
                                ColorUtils.isDark(bitmap, bitmap.width / 2, 0)
                            } else {
                                lightness == ColorUtils.IS_DARK
                            }

                            if (!isDark) { // make back icon dark on light images
                                back.setColorFilter(ContextCompat.getColor(
                                        this@DetailsActivity, R.color.dark_icon))
                            }

                            // color the status bar. Set a complementary dark color on L,
                            // light or dark color on M (with matching status bar icons)
                            var statusBarColor = window.statusBarColor
                            val topColor: Palette.Swatch? =
                                    ColorUtils.getMostPopulousSwatch(palette)
                            if (topColor != null &&
                                    (isDark || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
                                statusBarColor = ColorUtils.scrimify(topColor.rgb,
                                        isDark, SCRIM_ADJUSTMENT)
                                // set a light status bar on M+
                                if (!isDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    ViewUtils.setLightStatusBar(imageView)
                                }
                            }

                            if (statusBarColor != window.statusBarColor) {
                                imageView.setScrimColor(statusBarColor)
                                val statusBarColorAnim: ValueAnimator = ValueAnimator.ofArgb(
                                        window.statusBarColor, statusBarColor)
                                statusBarColorAnim.addUpdateListener({ animator: ValueAnimator? ->
                                    window.statusBarColor = animator!!.animatedValue as Int
                                })
                                statusBarColorAnim.duration = 1000L
                                statusBarColorAnim.interpolator = getFastOutSlowInInterpolator(this@DetailsActivity)
                                statusBarColorAnim.start()
                            }
                        }

                Palette.from(bitmap)
                        .clearFilters()
                        .generate { palette ->
                            // color the ripple on the image spacer (default is grey)
                            shotSpacer.background = ViewUtils.createRipple(palette, 0.25f, 0.5f,
                                    ContextCompat.getColor(this@DetailsActivity, R.color.mid_grey),
                                    true)
                            // slightly more opaque ripple on the pinned image to compensate
                            // for the scrim
                            imageView.foreground = ViewUtils.createRipple(palette, 0.3f, 0.6f,
                                    ContextCompat.getColor(this@DetailsActivity, R.color.mid_grey),
                                    true)
                        }
            }
            imageView.background = null
            return false
        }

    }

    //Notify user of a bad url from intent data
    private fun reportUrlError() {
        Snackbar.make(draggableFrame, R.string.bad_phoenix_url, Snackbar.LENGTH_SHORT).show()
        draggableFrame.postDelayed({ finishAfterTransition() }, 3000L)
    }

    /**
     * We run a transition to expand/collapse comments. Scrolling the RecyclerView while this is
     * running causes issues, so we consume touch events while the transition runs.
     */
    private val touchEater: View.OnTouchListener = View.OnTouchListener { _, _ -> true }

    //Append cart with product
    private fun appendCart() {
        performingLike = true
        val map = hashMapOf(
                Pair<String, Any?>("id", shot?.id),
                Pair<String, Any?>("name", shot?.name),
                Pair<String, Any?>("image", shot?.url),
                Pair<String, Any?>("price", shot?.price),
                Pair<String, Any?>("quantity", "1"),    //todo: add quantity chooser
                Pair<String, Any?>("timestamp", Date(System.currentTimeMillis()))
        )

        if (fab.isChecked) {
            //ref: phoenix/orders/{key}/**
            //Add product to customers' orders
            prefs.db.document(PhoenixUtils.ORDER_REF)   //ref: phoenix/orders
                    .collection(prefs.customer.key!!)
                    .document()
                    .set(map)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(applicationContext, "${shot?.name} has been added to your shopping cart",
                                    Toast.LENGTH_SHORT).show()
                        } else {
                            fab.isChecked = false
                        }
                    }
        } else {
            //ref: phoenix/orders/{key}/**
            //Remove from orders
            performingLike = false
            prefs.db.document(PhoenixUtils.ORDER_REF)
                    .collection(prefs.customer.key!!)
                    .whereEqualTo("name", shot?.name)
                    .get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if (task.result.documents[0].exists()) {
                                task.result.documents[0].reference.delete()
                                        .addOnCompleteListener { newTask ->
                                            fab.isChecked = !newTask.isSuccessful
                                        }
                            }
                        }
                    }
        }
    }

    //Listener for entering comment
    private val enterCommentFocus: View.OnFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        // kick off an anim (via animated state list) on the post button. see
        // @drawable/ic_add_comment
        postComment?.isActivated = hasFocus

        // prevent content hovering over image when not pinned.
        if (hasFocus) {
            imageView.bringToFront()
            imageView.offset = -imageView.height
            imageView.isImmediatePin = true
        }
    }

    //Open link from url using GoogleCustomTabs
    private fun openLink(url: String?) {
        if (url != null && !TextUtils.isEmpty(url)) {
            CustomTabActivityHelper.openCustomTab(
                    this@DetailsActivity,
                    CustomTabsIntent.Builder()
                            .setToolbarColor(ContextCompat.getColor(this@DetailsActivity, R.color.background_super_dark))
                            .addDefaultShareMenuItem()
                            .build(),
                    Uri.parse(url))
        }
    }

    //Check database to see whether the user has already added item to shopping cart
    private fun checkLiked() {
        //ref: phoenix/orders/{key}/**
        prefs.db.document(PhoenixUtils.ORDER_REF)
                .collection(prefs.customer.key!!)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        for (item in task.result.documents) {
                            if (item.exists()) {
                                val product = item.toObject(Product::class.java)
                                if (shot?.name != null && product.name!! == shot?.name!!) {
                                    TransitionManager.beginDelayedTransition(draggableFrame)
                                    fab.isChecked = true
                                }
                            }
                        }
                    } else {
                        if (BuildConfig.DEBUG) Log.d(TAG, task.exception?.localizedMessage)
                    }
                }
    }

    //Fling listener for recyclerview
    private val flingListener: RecyclerView.OnFlingListener = object : RecyclerView.OnFlingListener() {
        override fun onFling(velocityX: Int, velocityY: Int): Boolean {
            imageView.isImmediatePin = true
            return false
        }
    }

    //Scroll Listener for the recyclerview
    private val scrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            val scrollY: Int = shotDescription.top
            imageView.offset = scrollY
            fab.setOffset(fabOffset + scrollY)
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            // as we animate the main image's elevation change when it 'pins' at it's min height
            // a fling can cause the title to go over the image before the animation has a chance to
            // run. In this case we short circuit the animation and just jump to state.
            imageView.isImmediatePin = newState == RecyclerView.SCROLL_STATE_SETTLING
        }
    }

    //Calculate the position of the FAB when user starts to scroll the content
    private fun calculateFabPosition() {
        // calculate 'natural' position i.e. with full height image. Store it for use when scrolling
        fabOffset = imageView.height + title.height - fab.height / 2
        fab.setOffset(fabOffset)

        // calculate min position i.e. pinned to the collapsed image when scrolled
        fab.setMinOffset(imageView.minimumHeight - fab.height / 2)
    }

    //Setup commenting field
    private fun setupCommenting() {
        allowComment = prefs.isLoggedIn
        if (allowComment && commentFooter == null) {
            commentFooter = layoutInflater.inflate(R.layout.dribbble_enter_comment,
                    commentsList, false)
            userAvatar = commentFooter?.findViewById(R.id.avatar) as ForegroundImageView
            enterComment = commentFooter?.findViewById(R.id.comment) as EditText
            postComment = commentFooter?.findViewById(R.id.post_comment) as ImageButton
            enterComment?.onFocusChangeListener = enterCommentFocus

            GlideApp.with(this)
                    .load(prefs.customer.photo)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.ic_player)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .circleCrop()
                    .transition(withCrossFade())
                    .into(userAvatar)

        } else if (!allowComment && commentFooter != null) {
            adapter.removeCommentingFooter()
            commentFooter = null
            Toast.makeText(applicationContext, R.string.prospects_cant_post, Toast.LENGTH_SHORT).show()
        }

    }

    //Set result for any calling activity
    private fun setResultAndFinish() {
        val resultData = Intent()
        resultData.putExtra(RESULT_EXTRA_SHOT_ID, shot?.id)
        setResult(Activity.RESULT_OK, resultData)
        finishAfterTransition()
    }

    //Function to check whether the user commenting is the sender of that same comment
    private fun isOP(): Boolean {
        return prefs.isLoggedIn
    }

    //help with return transition
    override fun onBackPressed() {
        setResultAndFinish()
    }

    //helps with return transition
    override fun onNavigateUp(): Boolean {
        setResultAndFinish()
        return true
    }

    override fun onPause() {
        draggableFrame.removeListener(chromeFader)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (!performingLike) {
            if (prefs.isLoggedIn) checkLiked()
        }
        draggableFrame.addListener(chromeFader)
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onProvideAssistContent(outContent: AssistContent) {
        outContent.webUri = Uri.parse(shot?.url)
    }

    //Handle results from intent actions
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_LOGIN_LIKE -> {
                if (resultCode == RESULT_OK) {
                    fab.isChecked = true
                    appendCart()
                    setupCommenting()
                }
            }

            RC_LOGIN_COMMENT -> {
                if (resultCode == RESULT_OK) {
                    setupCommenting()
                }
            }
        }
    }

    //Adapter for the recyclerview
    internal inner class CommentsAdapter(private val description: View?,
                                         @param:Nullable private var footer: View?,
                                         commentCount: Long,
                                         expandDuration: Long) :
            RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val EXPAND: Int = 0x1
        private val COLLAPSE: Int = 0x2
        private val COMMENT_LIKE: Int = 0x3
        private val REPLY: Int = 0x4

        private var comments = ArrayList<Comment>(0)
        private var expandCollapse: Transition = AutoTransition()

        private var loading: Boolean = false
        private var noComments: Boolean = false
        private var expandedCommentPosition: Int = RecyclerView.NO_POSITION

        init {
            expandCollapse.duration = expandDuration
            //expandCollapse.interpolator = getFastOutSlowInInterpolator(this@DetailsActivity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                expandCollapse.addListener(object : TransitionListenerAdapter() {
                    override fun onTransitionStart(transition: Transition?) {
                        commentsList.setOnTouchListener(touchEater)
                    }

                    override fun onTransitionEnd(transition: Transition?) {
                        commentAnimator.setAnimateMoves(true)
                        commentsList.setOnTouchListener(null)
                    }
                })
            }
            noComments = commentCount == 0L
            loading = !noComments

        }

        override fun getItemViewType(position: Int): Int {
            Log.d(TAG, position.toString())
            if (position == 0) return R.layout.dribbble_shot_description
            if (position == 1) {
                if (loading) return R.layout.loading
                if (noComments) return R.layout.dribbble_no_comments
            }
            if (footer != null) {
                val footerPos = if (loading || noComments) 2 else comments.size + 1
                if (position == footerPos) return R.layout.dribbble_enter_comment
            }
            return R.layout.dribbble_comment
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
            when (getItemViewType(position)) {
                R.layout.dribbble_comment -> {
                    if (comments.isNotEmpty()) {
                        bindComment(holder as CommentViewHolder, getComment(position))
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int, payloads: MutableList<Any>?) {
            if (holder is CommentViewHolder) {
                bindPartialCommentChange(holder, position, payloads)
            } else {
                onBindViewHolder(holder, position)
            }
        }

        private fun bindPartialCommentChange(holder: CommentViewHolder, position: Int, partialChangePayloads: MutableList<Any>?) {
            // for certain changes we don't need to rebind data, just update some view state
            if ((partialChangePayloads!!.contains(EXPAND)
                    || partialChangePayloads.contains(COLLAPSE))
                    || partialChangePayloads.contains(REPLY)) {
                setExpanded(holder, position == expandedCommentPosition)
            } else if (partialChangePayloads.contains(COMMENT_LIKE)) {
                return // nothing to do
            } else {
                onBindViewHolder(holder, position)
            }
        }

        private fun bindComment(holder: CommentViewHolder, comment: Comment?) {
            val position = holder.adapterPosition
            val isExpanded = position == expandedCommentPosition
            GlideApp.with(this@DetailsActivity)
                    .load(comment?.user?.photo)
                    .apply(RequestOptions.circleCropTransform())
                    .apply(RequestOptions.placeholderOf(R.drawable.avatar_placeholder))
                    .transition(withCrossFade(300))
                    .into(holder.avatar)
            holder.author.text = comment?.user?.name
            holder.author.isOriginalPoster = isOP()
            holder.timeAgo.text = if (comment?.timestamp == null)
                DateUtils.getRelativeTimeSpanString(Date(System.currentTimeMillis()).time + 3L,
                        System.currentTimeMillis(),
                        DateUtils.SECOND_IN_MILLIS)
                        .toString().toLowerCase()
            else
                DateUtils.getRelativeTimeSpanString(comment.timestamp!!.time,
                        System.currentTimeMillis(),
                        DateUtils.SECOND_IN_MILLIS)
                        .toString().toLowerCase()
            HtmlUtils.setTextWithNiceLinks(holder.commentBody, comment?.getParsedBody(holder
                    .commentBody) as CharSequence?)
            holder.likeHeart.isChecked = comment?.liked != null && comment.liked!!
            if (prefs.isLoggedIn) {
                holder.likeHeart.isEnabled = comment?.user?.id != prefs.customer.id
            }
            holder.likesCount.text = (comment?.likes_count)?.toString()
            setExpanded(holder, isExpanded)
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
            when (viewType) {
                R.layout.dribbble_shot_description -> {
                    return SimpleViewHolder(description!!)
                }
                R.layout.dribbble_comment -> {
                    return createCommentHolder(parent, viewType)
                }
                R.layout.loading, R.layout.dribbble_no_comments -> {
                    return SimpleViewHolder(layoutInflater.inflate(viewType,
                            parent, false))
                }
                R.layout.dribbble_enter_comment -> {
                    return SimpleViewHolder(footer!!)
                }
                else -> throw IllegalArgumentException()
            }

        }

        @SuppressLint("SetTextI18n")
        private fun createCommentHolder(parent: ViewGroup?, viewType: Int): CommentViewHolder {
            val holder = CommentViewHolder(layoutInflater.inflate
            (viewType, parent, false))

            holder.itemView.setOnClickListener {
                val position: Int = holder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                val type = getItemViewType(position)
                if (type != R.layout.dribbble_comment) return@setOnClickListener
                if (comments.isEmpty()) return@setOnClickListener

                val comment = getComment(position)
                TransitionManager.beginDelayedTransition(commentsList, expandCollapse)
                commentAnimator.setAnimateMoves(false)

                // collapse any currently expanded items
                if (RecyclerView.NO_POSITION != expandedCommentPosition) {
                    notifyItemChanged(expandedCommentPosition, COLLAPSE)
                }

                // expand this item (if it wasn't already)
                if (expandedCommentPosition != position) {
                    expandedCommentPosition = position
                    notifyItemChanged(position, EXPAND)
                    if (comment.liked == null) {
                        //todo: some action
                    }
                    if (enterComment != null && enterComment!!.hasFocus()) {
                        enterComment?.clearFocus()
                        ImeUtils.hideIme(enterComment!!)
                    }
                    holder.itemView.requestFocus()
                } else {
                    expandedCommentPosition = RecyclerView.NO_POSITION
                }

            }

            holder.reply.setOnClickListener {
                val position = holder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener

                val comment = getComment(position)
                enterComment?.setText("@ ${comment.user?.name} ")
                enterComment?.setSelection(enterComment?.text!!.length)

                // collapse the comment and scroll the reply box (in the footer) into view
                expandedCommentPosition = RecyclerView.NO_POSITION
                notifyItemChanged(position, REPLY)
                holder.reply.jumpDrawablesToCurrentState()
                enterComment?.requestFocus()
                commentsList.smoothScrollToPosition(itemCount - 1)
            }

            holder.likeHeart.setOnClickListener {
                if (prefs.isLoggedIn) {
                    val position = holder.adapterPosition
                    if (position == RecyclerView.NO_POSITION) return@setOnClickListener

                    val comment = getComment(position)
                    if (comment.liked != null && !comment.liked!!) {
                        comment.liked = true
                        comment.likes_count = comment.likes_count.plus(1)
                        holder.likesCount.text = (comment.likes_count).toString()
                        notifyItemChanged(position, COMMENT_LIKE)
                        //Add map
                        val map: HashMap<String, Any> = hashMapOf(
                                Pair("id", System.currentTimeMillis()),
                                Pair("customer", prefs.customer),
                                Pair("comment", comment)
                        )
                        prefs.db.document(PhoenixUtils.LIKES_REF)   //like ref: phoenix/likes
                                .collection(prefs.customer.key!!)   //.../{userKey}/
                                .document()
                                .set(map)
                                .addOnFailureListener { exception ->
                                    Toast.makeText(this@DetailsActivity,
                                            exception.localizedMessage, Toast.LENGTH_SHORT).show()
                                }.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d(TAG, "Like added successfully")
                            } else {
                                Toast.makeText(this@DetailsActivity,
                                        task.exception?.localizedMessage,
                                        Toast.LENGTH_SHORT).show()
                            }
                        }

                    } else {
                        comment.liked = false
                        comment.likes_count--
                        holder.likesCount.text = (comment.likes_count).toString()
                        notifyItemChanged(position, COMMENT_LIKE)
                        prefs.db.document(PhoenixUtils.LIKES_REF)   //like ref: phoenix/likes
                                .collection(prefs.customer.key!!)   //.../{userKey}/
                                .get()
                                .addOnFailureListener { exception ->
                                    Toast.makeText(this@DetailsActivity,
                                            exception.localizedMessage, Toast.LENGTH_SHORT).show()
                                }
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        task.result.documents
                                                .filter { it.exists() && it.contains(comment.toString()) }
                                                .forEach {
                                                    //delete item from database
                                                    it.reference.delete()
                                                }
                                    } else {
                                        Toast.makeText(this@DetailsActivity,
                                                task.exception?.localizedMessage,
                                                Toast.LENGTH_SHORT).show()
                                    }
                                }

                    }
                } else {
                    holder.likeHeart.isChecked = false
                    startActivityForResult(Intent(this@DetailsActivity,
                            AuthActivity::class.java), RC_LOGIN_LIKE)
                }
            }

            holder.likesCount.setOnClickListener(View.OnClickListener {
                val position = holder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return@OnClickListener

                //val comment = getComment(position)
                //todo: comments like calls to know who sent a comment
            })

            return holder
        }

        @NonNull
        private fun getComment(adapterPosition: Int): Comment {
            return comments[adapterPosition - 1] // description
        }

        override fun getItemCount(): Int {
            var count = 1 // description
            if (comments.isNotEmpty()) {
                count += comments.size
            } else {
                count++ // either loading or no comments
            }
            if (footer != null) count++
            return count
        }

        private fun setExpanded(holder: CommentViewHolder, isExpanded: Boolean) {
            holder.itemView.isActivated = isExpanded
            holder.reply.visibility = if (isExpanded && allowComment) View.VISIBLE else View.GONE
            holder.likeHeart.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.likesCount.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }

        private fun hideLoadingIndicator() {
            if (!loading) return
            loading = false
            notifyItemRemoved(1)
        }

        private fun hideNoCommentsIndicator() {
            if (!noComments) return
            noComments = false
            notifyItemRemoved(1)
        }

        fun addComments(newComments: List<Comment>) {
            if (newComments.isNotEmpty()) {
                comments.clear()    //Clear comments first
                hideLoadingIndicator()
                hideNoCommentsIndicator()
                val count = comments.size
                for (item in newComments) {
                    var add = true
                    for (i in 0 until count) {
                        val existingItem: Comment? = comments[i]
                        if (existingItem != null && existingItem == item) {
                            add = false
                            return
                        }
                    }
                    if (add) {
                        comments.add(item)  //Add item
                    }
                }
                notifyItemRangeInserted(1, newComments.size)
            }
            Log.d(TAG, newComments.toString())
        }

        fun removeCommentingFooter() {
            if (footer == null) return
            val footerPos = itemCount - 1
            footer = null
            notifyItemRemoved(footerPos)
        }

    }


    /**
     * A {@link RecyclerView.ItemAnimator} which allows disabling move animations. RecyclerView
     * does not like animating item height changes. {@link android.transition.ChangeBounds} allows
     * this but in order to simultaneously collapse one item and expand another, we need to run the
     * Transition on the entire RecyclerView. As such it attempts to move views around. This
     * custom item animator allows us to stop RecyclerView from trying to handle this for us while
     * the transition is running.
     */
    internal class CommentAnimator : SlideInItemAnimator() {
        private var animateMoves = false

        fun setAnimateMoves(animateMoves: Boolean) {
            this.animateMoves = animateMoves
        }

        override fun animateMove(
                holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
            if (!animateMoves) {
                dispatchMoveFinished(holder)
                return false
            }
            return super.animateMove(holder, fromX, fromY, toX, toY)
        }

    }

    //Simple viewholder
    internal class SimpleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    //Comment viewholder
    internal class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var avatar: ImageView = itemView.findViewById(R.id.player_avatar)
        var author: AuthorTextView = itemView.findViewById(R.id.comment_author)
        var timeAgo: TextView = itemView.findViewById(R.id.comment_time_ago)
        var commentBody: TextView = itemView.findViewById(R.id.comment_text)
        var reply: ImageButton = itemView.findViewById(R.id.comment_reply)
        var likeHeart: CheckableImageButton = itemView.findViewById(R.id.comment_like)
        var likesCount: TextView = itemView.findViewById(R.id.comment_likes_count)


    }

    companion object {
        private val TAG: String = DetailsActivity::class.java.simpleName
        val EXTRA_SHOT = "EXTRA_SHOT"
        val RESULT_EXTRA_SHOT_ID = "RESULT_EXTRA_SHOT_ID"
        private val RC_LOGIN_LIKE = 0
        private val RC_LOGIN_COMMENT = 1
        private val SCRIM_ADJUSTMENT = 0.075f
    }
}

