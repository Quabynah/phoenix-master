/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import io.pergasus.R
import io.pergasus.api.OrderDataManager
import io.pergasus.api.PhoenixClient
import io.pergasus.data.CardData
import io.pergasus.data.Purchase
import io.pergasus.ui.widget.CircularImageView
import io.pergasus.util.bindView
import io.pergasus.util.collection.*

/**
 * Find user's orders in the database
 * */
class LiveOrdersActivity : Activity() {
    private val ecPagerView: ECPagerView by bindView(R.id.ec_pager_element)

    private lateinit var prefs: PhoenixClient
    private lateinit var dataManager: OrderDataManager
    private lateinit var adapter: OrdersAdapter
    private lateinit var purchaseList: ArrayList<Purchase>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_orders)

        prefs = PhoenixClient(this)

        val purchases: List<CardData> = ArrayList(0)
        dataManager = object : OrderDataManager(this@LiveOrdersActivity) {
            override fun onDataLoaded(data: List<Purchase>) {
                //todo: do something with data retrieved
            }
        }

        val adapter: ECPagerViewAdapter = object : ECPagerViewAdapter(this@LiveOrdersActivity, purchases) {
            override fun instantiateCard(inflaterService: LayoutInflater?, head: ViewGroup?, list: ListView?, data: ECCardData<*>?) {
                if (data is CardData) {
                    val cardData = data

                    //todo: continue from here


                }
            }
        }

        ecPagerView.setPagerViewAdapter(adapter)
        ecPagerView.setBackgroundSwitcherView(findViewById(R.id.ec_bg_switcher_element))
        val itemsCountView = findViewById<ItemsCountView>(R.id.items_count_view)
        ecPagerView.setOnCardSelectedListener(object : ECPagerView.OnCardSelectedListener {
            override fun cardSelected(newPosition: Int, oldPosition: Int, totalElements: Int) {
                itemsCountView.update(newPosition, oldPosition, totalElements)
            }
        })
    }

    override fun onBackPressed() {
        if (!ecPagerView.collapse()) super.onBackPressed()
    }

    fun dpFromPx(context: Context, px: Float): Float {
        return px / context.resources.displayMetrics.density
    }

    fun pxFromDp(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    override fun onDestroy() {
        dataManager.cancelLoading()
        super.onDestroy()
    }

    class OrdersAdapter(private val host: Context, @LayoutRes private val layout: Int,
                        private val purchases: List<Purchase>) :
            ECCardContentListItemAdapter<Purchase>(host, layout, purchases) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view: View? = convertView

            if (view == null) {
                val inflater = LayoutInflater.from(context)
                view = inflater.inflate(R.layout.live_order_item, null)

                var image: CircularImageView = view.findViewById(R.id.order_img)
                var key: TextView = view.findViewById(R.id.order_number)
                var date: TextView = view.findViewById(R.id.order_date)
                var price: TextView = view.findViewById(R.id.order_price)
                var track: Button = view.findViewById(R.id.track_order)

            }
            return view!!
        }
    }

    /*internal inner class OrdersViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
    }*/
}
