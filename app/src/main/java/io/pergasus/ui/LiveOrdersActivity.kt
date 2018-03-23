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
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade
import io.pergasus.BuildConfig
import io.pergasus.R
import io.pergasus.api.OrderDataManager
import io.pergasus.api.PhoenixClient
import io.pergasus.api.PhoenixUtils
import io.pergasus.data.Purchase
import io.pergasus.ui.recyclerview.SlideInItemAnimator
import io.pergasus.ui.widget.CircularImageView
import io.pergasus.util.bindView
import io.pergasus.util.glide.GlideApp
import timber.log.Timber
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

        dataManager = object : OrderDataManager(this@LiveOrdersActivity) {
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
        if (prefs.isLoggedIn) {
            dataManager.loadData(prefs.customer.key!!)  //Start loading data
            checkEmptyState()   //Check for data existence
        }
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
        var revoke: Button = view.findViewById(R.id.revoke_order)
    }

    internal inner class OrdersAdapter : RecyclerView.Adapter<OrdersViewHolder>() {
        private var purchases: ArrayList<Purchase> = ArrayList(0)
        private val loading: MaterialDialog

        init {
            setHasStableIds(true)
            loading = prefs.getDialog()
        }

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

            //Revoke order
            holder.revoke.setOnClickListener({
                MaterialDialog.Builder(this@LiveOrdersActivity)
                        .content("Do you wish to revoke Order #${purchase.key}")
                        .positiveText("Continue")
                        .negativeText("Cancel")
                        .onPositive({ dialog, _ ->
                            dialog.dismiss()
                            if (prefs.isConnected) {
                                loading.show()
                                dispatchItemRemoved(purchase)
                            } else {
                                Toast.makeText(this@LiveOrdersActivity, "Item cannot be removed at this time",
                                        Toast.LENGTH_SHORT).show()
                            }
                        })
                        .onNegative({ dialog, _ ->
                            dialog.dismiss()
                        })
                        .build().show()
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

            //Load profile image of user
            GlideApp.with(holder.itemView.context)
                    .asBitmap()
                    .load(prefs.customer.photo)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .transition(withCrossFade())
                    .placeholder(R.drawable.motor_placeholder)
                    .circleCrop()
                    .into(holder.image)

        }

        private fun navTrackingView(purchase: Purchase) {
            if (prefs.isLoggedIn) {
                val intent = Intent(this@LiveOrdersActivity, TrackingActivity::class.java)
                intent.putExtra(TrackingActivity.EXTRA_PURCHASE, purchase)
                startActivity(intent)
            } else {
                startActivity(Intent(this@LiveOrdersActivity, AuthActivity::class.java))
            }
        }

        private fun getItem(position: Int): Purchase {
            return purchases[position]
        }

        override fun getItemId(position: Int): Long {
            return purchases[position].id
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdersViewHolder {
            return OrdersViewHolder(layoutInflater.inflate(R.layout.live_order_item, parent, false))
        }

        /** Add live orders here from database */
        fun addLiveOrder(newItems: List<Purchase>) {
            if (newItems.isNotEmpty()) {
                for (item in newItems) {
                    var add = true
                    for (i in 0 until purchases.size) {
                        if (item.id == purchases[i].id) add = false
                    }

                    if (add) {
                        purchases.add(item)
                        notifyItemRangeChanged(0, newItems.size)
                    }
                }
            }
        }

        //Remove from database
        private fun dispatchItemRemoved(purchase: Purchase) {
            prefs.db.document(PhoenixUtils.PURCHASE_REF)
                    .collection(prefs.customer.key!!)
                    .whereEqualTo("key", purchase.key)
                    .get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val documents = task.result.documents
                            if (documents.isNotEmpty() && documents[0].exists()) {
                                documents[0].reference.delete().addOnSuccessListener { _ ->
                                    loading.dismiss()
                                    val position = purchases.indexOf(purchase)
                                    purchases.removeAt(position)
                                    notifyItemRemoved(position)
                                    if (BuildConfig.DEBUG) {
                                        Timber.d("Item ${purchase.key} removed from database")
                                    }
                                    dataManager.loadData(prefs.customer.key!!)
                                }
                            }
                        } else {
                            loading.dismiss()
                            Toast.makeText(applicationContext, task.exception?.localizedMessage,
                                    Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener { exception ->
                        loading.dismiss()
                        Toast.makeText(applicationContext, exception.localizedMessage,
                                Toast.LENGTH_SHORT).show()
                    }
        }

    }
}
