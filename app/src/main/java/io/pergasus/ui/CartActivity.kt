/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.transition.TransitionManager
import android.view.*
import android.widget.*
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.target.Target
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.QuerySnapshot
import io.pergasus.BuildConfig
import io.pergasus.R
import io.pergasus.R.string.price
import io.pergasus.api.PhoenixClient
import io.pergasus.api.PhoenixUtils
import io.pergasus.data.Order
import io.pergasus.data.PhoenixNotification
import io.pergasus.data.Purchase
import io.pergasus.ui.recyclerview.Divided
import io.pergasus.ui.recyclerview.GridItemDividerDecoration
import io.pergasus.ui.recyclerview.SlideInItemAnimator
import io.pergasus.ui.transitions.FabTransform
import io.pergasus.ui.transitions.MorphTransform
import io.pergasus.ui.widget.BadgedFourThreeImageView
import io.pergasus.util.AnimUtils
import io.pergasus.util.ShortcutHelper
import io.pergasus.util.bindView
import io.pergasus.util.glide.GlideApp
import timber.log.Timber
import java.text.NumberFormat
import java.util.*


/** User's Cart Activity */
@SuppressLint("LogConditional")
class CartActivity : Activity() {
    //Widgets
    private val frame: FrameLayout by bindView(R.id.draggable_frame)
    private val frameContent: ViewGroup by bindView(R.id.bottom_sheet_content)
    private val grid: RecyclerView by bindView(R.id.grid)
    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val fab: ImageButton by bindView(R.id.fab)
    private val noCart: TextView by bindView(R.id.no_cart)
    private val itemsCost: TextView by bindView(R.id.total_cost)
    var fabPosting: ImageButton? = null

    //Others
    private lateinit var client: PhoenixClient  //Shared preferences
    private lateinit var adapter: CartItemAdapter   //Adapter for recyclerview
    private lateinit var layoutManager: LinearLayoutManager //LayoutManager for recyclerview
    private val orders: ArrayList<Order> = ArrayList(0) //Empty arrayList for Orders

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)
        //Setup Toolbar support
        setActionBar(toolbar)

        //Animate the toolbar if the user ios launching the activity for the first time
        if (savedInstanceState == null) animateToolbar()

        //close the activity when user clicks on the back button
        toolbar.setNavigationOnClickListener { setResultAndFinish() }

        //Allow enter transition from FAB transformation
        if (!FabTransform.setup(this, frameContent)) {
            MorphTransform.setup(this, frameContent,
                    ContextCompat.getColor(this, R.color.background_dark), 0)
        }

        //Setup SharedPreferences
        client = PhoenixClient(this@CartActivity)

        //Setup recyclerview
        adapter = CartItemAdapter(this@CartActivity)    //init adapter
        grid.adapter = adapter  //set adapter for recyclerview
        layoutManager = LinearLayoutManager(this)   //init layout manager
        grid.layoutManager = layoutManager  //set layout manager for recyclerview
        grid.itemAnimator = SlideInItemAnimator()   //set item animator (when items are entering in)

        //Add separator for items in the recyclerview
        grid.addItemDecoration(GridItemDividerDecoration(this, R.dimen.divider_height, R.color.divider))

        //Enable shortcut to cart in Android 8.0+
        ShortcutHelper.reportCartUsed(this)
    }

    //Function to return results to the calling activity, if any
    private fun setResultAndFinish() {
        setResult(RESULT_OK)
        finishAfterTransition()
    }

    //Function to get data from the Orders database reference
    private fun getData() {
        //ref: phoenix/orders/{userKey}/**
        if (client.isLoggedIn) {
            if (orders.isNotEmpty()) orders.clear()
            client.db.document(PhoenixUtils.ORDER_REF)
                    .collection(client.customer.key!!)
                    .addSnapshotListener(this@CartActivity, EventListener<QuerySnapshot?> { p0, p1 ->
                        if (p1 != null) {
                            Toast.makeText(this@CartActivity, p1.localizedMessage,
                                    Toast.LENGTH_LONG).show()
                            noCart.visibility = View.VISIBLE
                            Timber.d(p1.localizedMessage)
                            return@EventListener
                        }
                        if (p0 != null) {
                            p0.documents
                                    .filter { it.exists() }
                                    .mapTo(orders) { it.toObject(Order::class.java) }
                            adapter.addOrders(orders)
                            //Hide no cart label if there are items in the database
                            noCart.visibility = if (p0.isEmpty) View.VISIBLE else View.GONE

                            val total: Double = getTotalValue(orders)
                            val quantity: Double = getTotalQuantity(orders)
                            //Set total cost
                            itemsCost.visibility = View.VISIBLE
                            itemsCost.text = String.format(Locale.US, "Total Cost: %s",
                                    NumberFormat.getCurrencyInstance(Locale.US).format(total))
                            checkEmptyState()

                            //Add click action
                            fab.setOnClickListener {
                                if (adapter.itemCount > 0) {
                                    val order = Intent(this, OrderActivity::class.java)
                                    FabTransform.addExtras(order,
                                            ContextCompat.getColor(this, R.color.accent), R.drawable.ic_attach_money_black_24dp)
                                    order.putExtra(OrderActivity.EXTRA_CART_PRICE, total)
                                    order.putExtra(OrderActivity.EXTRA_CART_QUANTITY, quantity)
                                    order.putExtra(OrderActivity.EXTRA_CART_TITLE, String.format("For the purchase of ${adapter.itemCount} items"))
                                    val options = ActivityOptions.makeSceneTransitionAnimation(this, fab,
                                            getString(R.string.transition_new_designer_news_post))
                                    startActivityForResult(order, CODE_ORDER, options.toBundle())
                                }
                            }
                        }
                    })

        } else {
            val intent = Intent(this@CartActivity, AuthActivity::class.java)
            FabTransform.addExtras(intent,
                    ContextCompat.getColor(this, R.color.button_accent),
                    R.drawable.ic_attach_money_black_24dp)
            val options = ActivityOptions.makeSceneTransitionAnimation(this, fab,
                    getString(R.string.transition_dribbble_login))
            startActivity(intent, options.toBundle())
        }

    }

    //Called once the activity starts up after onCreate has been called
    override fun onStart() {
        super.onStart()
        orders.clear()
        getData()
    }

    //Called once the activity has stopped
    override fun onStop() {
        orders.clear()
        super.onStop()
    }

    //Check the number of items in the adapter and update the UI respectively
    private fun checkEmptyState() {
        if (adapter.itemCount > 0) {
            TransitionManager.beginDelayedTransition(frame)
            grid.visibility = View.VISIBLE
            noCart.visibility = View.GONE
            fab.visibility = View.VISIBLE
        } else {
            TransitionManager.beginDelayedTransition(frame)
            grid.visibility = View.GONE
            noCart.visibility = View.VISIBLE
            fab.visibility = View.GONE
        }
    }

    //Calculates the total sum of all items' price in the user's shopping cart
    private fun getTotalValue(orders: ArrayList<Order>): Double {
        var total = 0.00
        if (orders.size > 0) {
            (0 until orders.size)
                    .map { orders[it] }
                    //Multiply the price by the quantity to get the total value
                    .forEach { total += (it.price?.toDouble()!!) * (it.quantity?.toInt()!!) }
        }
        return total

    }

    //Calculates the total sum of all items' quantity in the user's shopping cart
    private fun getTotalQuantity(orders: ArrayList<Order>): Double {
        var total = 0.00
        if (orders.size > 0) {
            (0 until orders.size)
                    .map { orders[it] }
                    //Multiply the price by the quantity to get the total value
                    .forEach { total += it.quantity?.toDouble()!! }
        }
        return total

    }

    //Handle results from intent actions
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            CODE_ORDER -> {
                when (resultCode) {
                    OrderActivity.RESULT_DRAG_DISMISSED -> {
                        showFab()
                    }
                    OrderActivity.RESULT_PAYING -> {
                        if (data != null && data.hasExtra(RESULT_PRICE)) {
                            showPostingProgress()
                            //Build new Purchase Object
                            val builder = Purchase.Builder()
                                    .setId(System.currentTimeMillis())
                                    .setKey(client.customer.key!!)
                                    .setPrice(price.toString())

                            //Add each item in orders list
                            for (item in orders) {
                                builder.setOrderItems(item)
                            }
                            client.db.document(PhoenixUtils.PURCHASE_REF)
                                    .collection(client.customer.key!!)
                                    .document()
                                    .set(builder.build())
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            //adapter.clear()
                                            doSuccessAnimation()    //Do animation
                                        } else {
                                            doFailureAnimation()    //Do animation
                                        }
                                    }
                        }
                    }
                    else -> {
                        super.onActivityResult(requestCode, resultCode, data)
                    }
                }
            }
        }
    }

    private fun doSuccessAnimation() {
        if (client.isConnected) clearCart()
        // success animation
        val complete: AnimatedVectorDrawable? =
                getDrawable(R.drawable.avd_upload_complete) as AnimatedVectorDrawable
        if (complete != null) {
            fabPosting?.setImageDrawable(complete)
            complete.start()
            // length of R.drawable.avd_upload_complete
            fabPosting?.postDelayed({
                fabPosting?.visibility = View.GONE
                sendNotification()  //Send notification to user
                //Show success screen
                val intent = Intent(this@CartActivity, SuccessActivity::class.java)
                intent.putExtra(SuccessActivity.EXTRA_PAGE, SuccessActivity.PAGE_SUCCESS)
                startActivity(intent)
                setResultAndFinish()
            }, 2000)
        } else {
            sendNotification()  //Send notification to user
            //Show success screen
            val intent = Intent(this@CartActivity, SuccessActivity::class.java)
            intent.putExtra(SuccessActivity.EXTRA_PAGE, SuccessActivity.PAGE_SUCCESS)
            startActivity(intent)
            setResultAndFinish()
        }
    }

    private fun sendNotification() {
        //Send notification to database
        val notification = PhoenixNotification(
                "Congratulations",
                "Your recent purchase was successful",
                "dummy",    //Dummy text
                client.customer.key!!,
                "dummy" //Dummy text
        )
        client.db.collection(PhoenixUtils.NOTIFICATIONS_REF)
                .document()
                .set(notification.toHashMap(notification))
                .addOnFailureListener { exception ->
                    if (BuildConfig.DEBUG) Timber.d(exception.localizedMessage)
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (BuildConfig.DEBUG) Timber.d("Notification sent")
                        //Firebase functions will notify the user of new notification
                    } else {
                        if (BuildConfig.DEBUG) Timber.d(task.exception?.localizedMessage)
                    }
                }
    }

    private fun clearCart() {
        client.db.document(PhoenixUtils.ORDER_REF)
                .collection(client.customer.key!!)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        querySnapshot.documents
                                .filter { it.exists() }
                                .forEach { it.reference.delete() }
                        adapter.notifyDataSetChanged()
                        checkEmptyState()
                    }
                }
    }

    //Show failure animation
    private fun doFailureAnimation() {
        // failure animation
        val failed: AnimatedVectorDrawable? =
                getDrawable(R.drawable.avd_upload_error) as AnimatedVectorDrawable
        if (failed != null) {
            fabPosting?.setImageDrawable(failed)
            failed.start()
        }
        // remove the upload progress 'fab' and reshow the regular one
        fabPosting?.animate()
                ?.alpha(0f)
                ?.rotation(90f)
                ?.setStartDelay(2000L) // leave error on screen briefly
                ?.setDuration(300L)
                ?.setInterpolator(AnimUtils
                        .getFastOutSlowInInterpolator(this@CartActivity))
                ?.setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        fabPosting?.visibility = View.GONE
                        fabPosting?.alpha = 1f
                        fabPosting?.rotation = 0f
                    }
                })
    }

    //Show posting progress once data has been
    private fun showPostingProgress() {
        ensurePostingProgressInflated()
        fabPosting?.visibility = View.VISIBLE
        // if stub has just been inflated then it will not have been laid out yet
        if (fabPosting?.isLaidOut!!) {
            revealPostingProgress()
        } else {
            fabPosting?.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(p0: View?, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int, p8: Int) {
                    fabPosting?.removeOnLayoutChangeListener(this)
                    revealPostingProgress()
                }
            })
        }
    }

    private fun ensurePostingProgressInflated() {
        if (fabPosting != null) return
        fabPosting = (findViewById<ViewStub>(R.id.stub_posting_progress)).inflate() as ImageButton?
    }

    private fun revealPostingProgress() {
        val reveal = ViewAnimationUtils.createCircularReveal(fabPosting,
                fabPosting?.pivotX!!.toInt(),
                fabPosting?.pivotY!!.toInt(),
                0f,
                (fabPosting?.width!! / 2).toFloat())
                .setDuration(600L)
        reveal.interpolator = AnimUtils.getFastOutLinearInInterpolator(this)
        reveal.start()
        val uploading: AnimatedVectorDrawable? = getDrawable(R.drawable.avd_uploading) as AnimatedVectorDrawable
        if (uploading != null) {
            fabPosting?.setImageDrawable(uploading)
            uploading.start()
            updateData()
        }
    }

    private fun updateData() {
        if (client.isLoggedIn) {
            if (client.isConnected) {
                client.db.document(PhoenixUtils.ORDER_REF)
                        .collection(client.customer.key!!)
                        .get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                showSuccessProgress()
                            } else {
                                Toast.makeText(this, task.exception?.localizedMessage,
                                        Toast.LENGTH_LONG).show()
                                hidePostingProgress()
                            }
                        }
            } else {
                Toast.makeText(this, "Cannot retrieve data at this time", Toast.LENGTH_LONG).show()
                hidePostingProgress()
            }
        }
    }

    private fun hidePostingProgress() {
        // success animation
        val failed = getDrawable(R.drawable.avd_upload_error) as AnimatedVectorDrawable?
        if (failed != null) {
            fabPosting?.setImageDrawable(failed)
            failed.start()
        }
        fabPosting?.animate()
                ?.alpha(0f)
                ?.rotation(90f)
                ?.setStartDelay(2000L) // leave error on screen briefly
                ?.setDuration(300L)
                ?.setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        fabPosting?.visibility = View.GONE
                        fabPosting?.alpha = 1f
                        fabPosting?.rotation = 0f
                    }
                })
                ?.interpolator = AnimUtils.getFastOutSlowInInterpolator(this@CartActivity)

    }

    private fun showSuccessProgress() {
        // success animation
        val complete = getDrawable(R.drawable.avd_upload_complete) as AnimatedVectorDrawable?
        if (complete != null) {
            fabPosting?.setImageDrawable(complete)
            complete.start()
            fabPosting?.postDelayed({ fabPosting?.visibility = View.GONE }, 2100)
        }
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
                .setInterpolator(AnimUtils.getLinearOutSlowInInterpolator(this@CartActivity))
                .start()
    }

    //Animate the title of the toolbar
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
                    .setDuration(600).interpolator = AnimUtils.getFastOutSlowInInterpolator(this@CartActivity)
        }
    }

    //Adapter for customer's orders
    internal inner class CartItemAdapter(private val host: Activity) : RecyclerView.Adapter<CartViewHolder>() {
        private val items: ArrayList<Order> = ArrayList(0)
        private val loading: MaterialDialog

        init {
            setHasStableIds(true)
            loading = client.getDialog()
        }

        override fun onBindViewHolder(holder: CartViewHolder, p0: Int) {
            val position = holder.adapterPosition
            val order = items[position]
            holder.name.text = order.name
            holder.price.text = NumberFormat.getCurrencyInstance(Locale.US)
                    .format(order.price?.toDouble())
            holder.quantity.text = String.format("%s units added", order.quantity)
            GlideApp.with(host)
                    .load(order.image)
                    .placeholder(R.color.content_placeholder)
                    .error(R.color.content_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .transition(withCrossFade())
                    .into(holder.image)

            holder.itemView.setOnClickListener({ _ ->
                val builder = MaterialDialog.Builder(this@CartActivity)
                val v = layoutInflater.inflate(R.layout.order_item_info, null, false)
                builder.customView(v, false)  //Attach view to builder
                val dialog = builder.build()
                //Get items in layout
                val img = v.findViewById<BadgedFourThreeImageView>(R.id.order_info_image)
                val name = v.findViewById<TextView>(R.id.order_info_name)
                val details = v.findViewById<TextView>(R.id.order_info_details)
                val del = v.findViewById<Button>(R.id.order_info_delete)
                //Load image into view
                GlideApp.with(host)
                        .load(order.image)
                        .placeholder(R.color.content_placeholder)
                        .error(R.color.content_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .transition(withCrossFade())
                        .into(img)
                name.text = order.name  //Order name
                val dateInfo = DateUtils.getRelativeDateTimeString(this@CartActivity,
                        if (order.timestamp == null) System.currentTimeMillis() else order.timestamp?.time!!,
                        DateUtils.SECOND_IN_MILLIS, DateUtils.SECOND_IN_MILLIS, DateUtils
                        .FORMAT_ABBREV_ALL)
                val info = String.format("Purchased %s goods for ${NumberFormat
                        .getCurrencyInstance().format(order.price?.toDouble())} each\n on %s",
                        order.quantity, dateInfo)
                details.text = info
                //Delete action
                del.setOnClickListener({
                    dialog.dismiss()
                    removeOrder(order)
                })
                dialog.show()
            })
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
            val v: View = LayoutInflater.from(host).inflate(R.layout.order_item, parent, false)
            return CartViewHolder(v)
        }

        override fun getItemId(position: Int): Long {
            return items[position].id
        }

        fun addOrders(newItems: ArrayList<Order>) {
            deduplicateAndAdd(newItems)
            notifyDataSetChanged()
        }

        private fun removeOrder(order: Order) {
            if (client.isConnected) {
                loading.show()
                dispatchOrderRemoved(order)
            } else {
                Toast.makeText(this@CartActivity, "Item cannot be removed at this time",
                        Toast.LENGTH_SHORT).show()
            }
        }

        fun clear() {
            items.clear()
            notifyItemRangeChanged(0, items.size)
        }

        //Remove from database
        private fun dispatchOrderRemoved(order: Order) {
            client.db.document(PhoenixUtils.ORDER_REF)
                    .collection(client.customer.key!!)
                    .whereEqualTo("name", order.name)
                    .get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val documents = task.result.documents
                            if (documents.isNotEmpty() && documents[0].exists()) {
                                documents[0].reference.delete().addOnSuccessListener { _ ->
                                    loading.dismiss()
                                    val position = items.indexOf(order)
                                    items.removeAt(position)
                                    notifyItemRemoved(position)
                                    getData()
                                    if (BuildConfig.DEBUG) {
                                        Timber.d("Item ${order.name} removed from database")
                                    }
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


        @SuppressLint("LogConditional")
        private fun deduplicateAndAdd(newItems: ArrayList<Order>) {
            Timber.d(newItems.toString())
            val count = itemCount
            for (newItem in newItems) {
                val add = !(0 until count)
                        .map { getItem(it) }
                        .contains(newItem)
                if (add) {
                    add(newItem)
                }
            }
        }

        private fun add(newItem: Order) {
            items.add(newItem)
        }

        private fun getItem(position: Int): Order {
            return items[position]
        }

    }

    internal inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), Divided {
        var name: TextView = itemView.findViewById(R.id.order_name)
        var image: BadgedFourThreeImageView = itemView.findViewById(R.id.order_image)
        var price: TextView = itemView.findViewById(R.id.order_price)
        var quantity: TextView = itemView.findViewById(R.id.order_quantity)
    }

    companion object {
        private const val CODE_ORDER = 13
        const val RESULT_PRICE = "RESULT_PRICE"
    }
}
