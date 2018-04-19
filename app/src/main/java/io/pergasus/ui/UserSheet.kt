/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.support.annotation.IntDef
import android.support.annotation.NonNull
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import io.pergasus.R
import io.pergasus.api.DataLoadingSubject
import io.pergasus.api.FollowerListable
import io.pergasus.api.FollowersDataManager
import io.pergasus.data.Follow
import io.pergasus.data.Shop
import io.pergasus.ui.recyclerview.SlideInItemAnimator
import io.pergasus.ui.widget.BottomSheet
import io.pergasus.ui.widget.CircularImageView
import io.pergasus.util.AnimUtils.getLinearOutSlowInInterpolator
import io.pergasus.util.HtmlUtils
import io.pergasus.util.bindView
import io.pergasus.util.glide.GlideApp
import java.text.NumberFormat
import java.util.*


/**
 * Shows a list of followers for a particular shop
 */
class UserSheet : Activity() {
    companion object {
        private const val MODE_FOLLOWERS = 2
        private const val DISMISS_DOWN = 0
        private const val DISMISS_CLOSE = 1
        private const val EXTRA_MODE = "EXTRA_MODE"
        private const val EXTRA_SHOP = "EXTRA_SHOP"

        private const val TYPE_PLAYER = 7
        private const val TYPE_LOADING = -1


        fun start(@NonNull launching: Activity, shop: Shop) {
            val starter = Intent(launching, UserSheet::class.java)
            starter.putExtra(EXTRA_MODE, MODE_FOLLOWERS)
            starter.putExtra(EXTRA_SHOP, shop)
            launching.startActivity(starter/*,
                    ActivityOptions.makeSceneTransitionAnimation(launching).toBundle()*/)
        }
    }

    private var dismissState = DISMISS_DOWN

    private var shop: Shop? = null
    private var dataManager: FollowersDataManager? = null
    private var adapter: FollowerAdapter<Follow>? = null
    private var layoutManager: LinearLayoutManager? = null

    private val bottomSheet: BottomSheet by bindView(R.id.bottom_sheet)
    private val content: ViewGroup by bindView(R.id.bottom_sheet_content)
    private val titleBar: ViewGroup by bindView(R.id.title_bar)
    private val close: ImageView by bindView(R.id.close)
    private val title: TextView by bindView(R.id.title)
    private val grid: RecyclerView by bindView(R.id.item_list)
    private var largeAvatarSize: Int = 0

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(MODE_FOLLOWERS)
    internal annotation class PlayerSheetMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sheet)

        largeAvatarSize = resources.getDimension(R.dimen.large_avatar_size).toInt()

        val intent = intent
        @PlayerSheetMode val mode = intent.getIntExtra(EXTRA_MODE, -1)
        when (mode) {
            MODE_FOLLOWERS -> {
                shop = intent.getParcelableExtra(EXTRA_SHOP)
                title.text = resources.getQuantityString(
                        R.plurals.followers,
                        shop?.followers_count?.toInt()!!,
                        NumberFormat.getInstance().format(shop?.followers_count!!))
                dataManager = object : FollowersDataManager(this@UserSheet, shop!!) {
                    override fun onDataLoaded(data: List<Follow>) {
                        adapter?.addItems(data)
                    }

                }
            }
            else -> throw IllegalArgumentException("Unknown launch mode.")
        }

        bottomSheet.registerCallback(object : BottomSheet.Callbacks() {
            override fun onSheetDismissed() {
                finishAfterTransition()
            }

            override fun onSheetPositionChanged(sheetTop: Int, userInteracted: Boolean) {
                if (userInteracted && close.visibility != View.VISIBLE) {
                    close.visibility = View.VISIBLE
                    close.alpha = 0f
                    close.animate()
                            .alpha(1f)
                            .setDuration(400L)
                            .setInterpolator(getLinearOutSlowInInterpolator(this@UserSheet))
                            .start()
                }
                if (sheetTop == 0) {
                    showClose()
                } else {
                    showDown()
                }
            }
        })

        layoutManager = LinearLayoutManager(this)
        grid.layoutManager = layoutManager
        grid.itemAnimator = SlideInItemAnimator()
        adapter = FollowerAdapter(this@UserSheet)
        dataManager?.registerCallback(adapter!!)
        grid.adapter = adapter
        grid.addOnScrollListener(titleElevation)
        dataManager?.loadData() // kick off initial load

    }

    override fun onDestroy() {
        dataManager?.cancelLoading()
        super.onDestroy()
    }

    private val titleElevation: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            val raiseTitleBar: Boolean = dy > 0 || grid.computeVerticalScrollOffset() != 0
            titleBar.isActivated = raiseTitleBar // animated via a StateListAnimator
        }
    }

    private fun showClose() {
        if (dismissState == DISMISS_CLOSE) return
        dismissState = DISMISS_CLOSE
        val downToClose: AnimatedVectorDrawable =
                ContextCompat.getDrawable(this, R.drawable.avd_down_to_close) as AnimatedVectorDrawable
        close.setImageDrawable(downToClose)
        downToClose.start()
    }

    private fun showDown() {
        if (dismissState == DISMISS_DOWN) return
        dismissState = DISMISS_DOWN
        val closeToDown: AnimatedVectorDrawable =
                ContextCompat.getDrawable(this, R.drawable.avd_close_to_down) as AnimatedVectorDrawable
        close.setImageDrawable(closeToDown)
        closeToDown.start()
    }

    fun dismiss(view: View) {
        if (view.visibility != View.VISIBLE) return
        bottomSheet.dismiss()
    }

    private inner class FollowerAdapter<T : FollowerListable>(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DataLoadingSubject.DataLoadingCallbacks {
        private var loading = true
        private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
        internal var items: MutableList<T> = ArrayList(0)

        internal val dataItemCount: Int
            get() = items.size

        private val loadingMoreItemPosition: Int
            get() = if (loading) itemCount - 1 else RecyclerView.NO_POSITION

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return when (viewType) {
                TYPE_PLAYER -> createPlayerViewHolder(parent)
                else -> LoadingViewHolder(
                        // TYPE_LOADING
                        layoutInflater.inflate(R.layout.list_loading, parent, false))
            }
        }

        private fun createPlayerViewHolder(parent: ViewGroup): SheetViewHolder {
            return SheetViewHolder(
                    layoutInflater.inflate(R.layout.sheet_item, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position == loadingMoreItemPosition) return
            bindPlayer(holder as SheetViewHolder, items[position])
        }

        private fun bindPlayer(holder: SheetViewHolder, t: T) {
            GlideApp.with(holder.itemView.context)
                    .load(t.customer?.photo)
                    .apply(RequestOptions.circleCropTransform())
                    .apply(RequestOptions().placeholder(R.drawable.avatar_placeholder))
                    .apply(RequestOptions().override(largeAvatarSize, largeAvatarSize))
                    .transition(withCrossFade())
                    .into(holder.playerAvatar)
            holder.playerName.text = t.customer?.name?.toLowerCase(Locale.US)
            if (!TextUtils.isEmpty(t.customer?.info)) {
                HtmlUtils.parseAndSetText(holder.playerBio, t.customer?.info)
            }
            if (t.dateCreated != null) {
                holder.timeAgo.text = DateUtils.getRelativeTimeSpanString(t.dateCreated?.time!!,
                        System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
                        .toString().toLowerCase(Locale.US)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (position < dataItemCount && dataItemCount > 0) {
                TYPE_PLAYER
            } else TYPE_LOADING
        }

        override fun getItemId(position: Int): Long {
            return if (getItemViewType(position) == TYPE_LOADING) {
                -1L
            } else items[position].id!!
        }

        override fun getItemCount(): Int {
            return dataItemCount + if (loading) 1 else 0
        }

        override fun dataStartedLoading() {
            if (loading) return
            loading = true
            notifyItemInserted(loadingMoreItemPosition)
        }

        override fun dataFinishedLoading() {
            if (!loading) return
            val loadingPos = loadingMoreItemPosition
            loading = false
            notifyItemRemoved(loadingPos)
        }

        internal fun addItems(newItems: List<T>) {
            val insertRangeStart = dataItemCount
            items.addAll(newItems)
            notifyItemRangeInserted(insertRangeStart, newItems.size)
        }

    }

    internal class SheetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var playerAvatar: CircularImageView = itemView.findViewById(R.id.player_avatar)
        var playerName: TextView = itemView.findViewById(R.id.player_name)
        var playerBio: TextView = itemView.findViewById(R.id.player_bio)
        var timeAgo: TextView = itemView.findViewById(R.id.time_ago)
    }

    internal class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var progress: ProgressBar = itemView as ProgressBar

    }


}
