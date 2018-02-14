/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toolbar
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.util.ViewPreloadSizeProvider
import io.pergasus.BuildConfig
import io.pergasus.R
import io.pergasus.api.NotificationDataManager
import io.pergasus.api.PhoenixClient
import io.pergasus.data.PhoenixNotification
import io.pergasus.ui.widget.CircularImageView
import io.pergasus.util.bindView
import io.pergasus.util.glide.GlideApp
import java.util.*

/** Notification page */
class NotificationsActivity : Activity() {

    private val container: ViewGroup by bindView(R.id.container)
    private val grid: RecyclerView by bindView(R.id.grid)
    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val noNotifications: TextView by bindView(R.id.no_notifications)

    private lateinit var prefs: PhoenixClient
    private lateinit var adapter: NotificationsAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var dataManager: NotificationDataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        setActionBar(toolbar)

        toolbar.setNavigationOnClickListener({ finish() })  //End activity on back-arrow press

        prefs = PhoenixClient(this)

        //get intent data here
        val intent = intent
        if (intent != null) {
            val title = intent.getStringExtra("title")
            val body = intent.getStringExtra("body")
            //Log results for now
            if (BuildConfig.DEBUG) Log.d(TAG, "Message from notification is $title | $body")
        }

        val preloadSizeProvider: ViewPreloadSizeProvider<PhoenixNotification> = ViewPreloadSizeProvider()
        adapter = NotificationsAdapter(preloadSizeProvider)
        grid.adapter = adapter
        grid.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(this)
        grid.layoutManager = layoutManager
        val shotPreloader: RecyclerViewPreloader<PhoenixNotification> =
                RecyclerViewPreloader<PhoenixNotification>(this, adapter, preloadSizeProvider, 4)
        grid.addOnScrollListener(shotPreloader)
        dataManager = object : NotificationDataManager(this@NotificationsActivity) {
            override fun onDataLoaded(data: List<PhoenixNotification>) {
                adapter.addNotifications(data)
                checkEmptyState()
            }
        }
        dataManager.loadAllNotifications()
        checkEmptyState()
    }

    private fun checkEmptyState() {
        if (adapter.itemCount > 0) {
            TransitionManager.beginDelayedTransition(container)
            grid.visibility = View.VISIBLE
            noNotifications.visibility = View.GONE
        } else {
            TransitionManager.beginDelayedTransition(container)
            grid.visibility = View.GONE
            noNotifications.visibility = View.VISIBLE
        }
    }


    internal inner class NotificationsAdapter(private val preloadSizeProvider: ViewPreloadSizeProvider<PhoenixNotification>)
        : RecyclerView.Adapter<NotificationHolder>(), ListPreloader.PreloadModelProvider<PhoenixNotification> {
        private var notifications: ArrayList<PhoenixNotification> = ArrayList(0)

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): NotificationHolder {
            val view = layoutInflater.inflate(R.layout.notifications_item, parent, false)
            return NotificationHolder(view)
        }

        override fun getItemCount(): Int {
            return notifications.size
        }

        override fun onBindViewHolder(holder: NotificationHolder?, position: Int) {
            val phoenixNotifications = notifications[position]

            if (holder != null) {
                //Load image
                GlideApp.with(applicationContext)
                        .load(phoenixNotifications.image)
                        .placeholder(R.drawable.avatar_placeholder)
                        .error(R.drawable.avatar_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .into(holder.image)

                //Set title
                holder.title.text = phoenixNotifications.title

                //Set message
                holder.message.text = phoenixNotifications.message

                //Set time
                if (phoenixNotifications.timestamp != null) {
                    holder.timestamp.text = DateUtils.getRelativeTimeSpanString(phoenixNotifications.timestamp?.time!!,
                            System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS,
                            DateUtils.FORMAT_SHOW_TIME)

                }

                //Set item preloader
                preloadSizeProvider.setView(holder.image)
            }

        }

        override fun getPreloadItems(position: Int): MutableList<PhoenixNotification> {
            val notification = getItem(position)
            return Collections.singletonList(notification)
        }

        override fun getPreloadRequestBuilder(item: PhoenixNotification): RequestBuilder<*>? {
            return GlideApp.with(applicationContext).load(item.image)
        }

        /** Main entry point for new [PhoenixNotification] */
        fun addNotifications(newItems: List<PhoenixNotification>) {
            val count = itemCount
            for (data in newItems) {
                var add = true
                for (i in 0 until count) {
                    val existingItem = getItem(i)
                    if (existingItem != null && existingItem == data) {
                        add = false
                    }
                }
                if (add) {
                    notifications.add(data)
                }
            }
            notifyDataSetChanged()
        }

        private fun getItem(position: Int): PhoenixNotification? {
            if (position < 0 || position >= notifications.size) return null
            return notifications[position]
        }

        override fun getItemId(position: Int): Long {
            return notifications[position].hashCode().toLong()
        }

    }

    internal inner class NotificationHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image: CircularImageView = itemView.findViewById(R.id.notification_image)
        var title: TextView = itemView.findViewById(R.id.notification_title)
        var message: TextView = itemView.findViewById(R.id.notification_msg)
        var timestamp: TextView = itemView.findViewById(R.id.notification_time)
    }

    companion object {
        private val TAG = NotificationsActivity::class.java.simpleName
    }
}
