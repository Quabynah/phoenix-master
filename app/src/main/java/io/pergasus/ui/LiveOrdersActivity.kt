/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import io.pergasus.R
import io.pergasus.api.OrderDataManager
import io.pergasus.api.PhoenixClient
import io.pergasus.data.Purchase
import io.pergasus.ui.recyclerview.SlideInItemAnimator
import io.pergasus.ui.widget.CircularImageView
import io.pergasus.util.bindView
import java.text.NumberFormat
import java.util.*

/**
 * Find user's orders in the database
 * */
class LiveOrdersActivity : Activity() {
    private val grid: RecyclerView by bindView(R.id.grid)
    private val container: ViewGroup by bindView(R.id.container)
    private val noOrders: TextView by bindView(R.id.no_orders)

    private lateinit var prefs: PhoenixClient
    private lateinit var dataManager: OrderDataManager
    private lateinit var adapter: OrdersAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_orders)

        prefs = PhoenixClient(this)

        dataManager = object : OrderDataManager(this@LiveOrdersActivity, prefs.customer.key!!) {
            override fun onDataLoaded(data: List<Purchase>) {
                adapter.addLiveOrder(data)
                checkEmptyState()
            }
        }

        //Setup recyclerview
        adapter = OrdersAdapter()
        grid.adapter = adapter
        grid.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        grid.setHasFixedSize(true)
        grid.itemAnimator = SlideInItemAnimator()
        dataManager.loadData()  //Start loading data
        checkEmptyState()   //Check for data existence
    }

    private fun checkEmptyState() {
        if (adapter.itemCount > 0) {
            TransitionManager.beginDelayedTransition(container)
            grid.visibility = View.VISIBLE
            noOrders.visibility = View.GONE
        } else {
            TransitionManager.beginDelayedTransition(container)
            grid.visibility = View.GONE
            noOrders.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        dataManager.cancelLoading()
        super.onDestroy()
    }

    internal inner class OrdersViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var image: CircularImageView = view.findViewById(R.id.order_img)
        var key: TextView = view.findViewById(R.id.order_number)
        var date: TextView = view.findViewById(R.id.order_date)
        var price: TextView = view.findViewById(R.id.order_price)
        var track: Button = view.findViewById(R.id.track_order)
    }

    internal inner class OrdersAdapter : RecyclerView.Adapter<OrdersViewHolder>() {
        private var purchases: ArrayList<Purchase> = ArrayList(0)
        override fun getItemCount(): Int {
            return purchases.size
        }

        override fun onBindViewHolder(holder: OrdersViewHolder, position: Int) {
            val purchase = getItem(position)
            //navigation
            holder.itemView.setOnClickListener({
                navTrackingView(purchase)
            })
            holder.track.setOnClickListener({
                navTrackingView(purchase)
            })

            //Order number
            holder.key.text = String.format("Order #%s", purchase.purchaseId)
            //Order date
            if (purchase.timestamp != null) {
                holder.date.text = DateUtils.getRelativeTimeSpanString(purchase.timestamp!!.time,
                        System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
            }
            //Order price
            holder.price.text = NumberFormat.getCurrencyInstance(Locale.US).format(purchase.price?.toDouble())

        }

        private fun navTrackingView(purchase: Purchase) {
            val intent = Intent(this@LiveOrdersActivity, TrackingActivity::class.java)
            intent.putExtra(TrackingActivity.EXTRA_PURCHASE, purchase)
            startActivity(intent)
        }

        private fun getItem(position: Int): Purchase {
            return purchases[position]
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdersViewHolder {
            return OrdersViewHolder(layoutInflater.inflate(R.layout.live_order_item, parent, false))
        }

        /** Add live orders here from database */
        fun addLiveOrder(newItems: List<Purchase>) {
            val count = itemCount
            for (data in newItems) {
                var add = true
                for (i in 0 until count) {
                    val existingItem = getItem(i)
                    if (existingItem == data) {
                        add = false
                    }
                }
                if (add) {
                    purchases.add(data)
                }
            }
            notifyDataSetChanged()
        }
    }
}
