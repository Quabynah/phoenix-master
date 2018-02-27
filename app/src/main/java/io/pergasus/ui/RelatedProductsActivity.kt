/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.Toolbar
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import io.pergasus.R
import io.pergasus.api.PhoenixClient
import io.pergasus.api.ProductItem
import io.pergasus.api.RelatedProductDataManager
import io.pergasus.data.Product
import io.pergasus.ui.recyclerview.SlideInItemAnimator
import io.pergasus.util.bindView

/**
 * Related products: Shows products related to some other preset product
 */
class RelatedProductsActivity : Activity() {
    private val container: ViewGroup by bindView(R.id.container)
    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val results: RecyclerView by bindView(R.id.grid)

    private lateinit var client: PhoenixClient
    private lateinit var loading: MaterialDialog
    private lateinit var dataManager: RelatedProductDataManager
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var adapter: SearchDataAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_related_products)
        setActionBar(toolbar)

        toolbar.setNavigationOnClickListener({ finishAfterTransition() })

        client = PhoenixClient(this@RelatedProductsActivity)
        loading = client.getDialog()

        dataManager = object : RelatedProductDataManager(this@RelatedProductsActivity) {
            override fun onDataLoaded(data: List<ProductItem>) {
                adapter.addAndResort(data)
                checkEmptyState()
            }
        }

        val columns: Int = resources.getInteger(R.integer.num_columns)
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
        results.setHasFixedSize(true)
        val shotPreloader: RecyclerViewPreloader<Product> = RecyclerViewPreloader<Product>(this,
                adapter, preloadSizeProvider, 4)
        results.addOnScrollListener(shotPreloader)

        val intent = intent
        if (intent.hasExtra(EXTRA_PRODUCT)) {
            loading.show()
            val product = intent.getParcelableExtra<Product>(EXTRA_PRODUCT)
            //Compare by price, category and shop
            dataManager.loadRelatedProductsFromSource(product)
        }
    }

    private fun checkEmptyState() {
        if (loading.isShowing) loading.dismiss()
    }

    override fun onBackPressed() {
        if (loading.isShowing) loading.dismiss()
        else super.onBackPressed()
    }

    companion object {
        const val EXTRA_PRODUCT = "EXTRA_PRODUCT"
    }
}
