/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */
package io.pergasus.api

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.pergasus.data.Product
import io.pergasus.ui.FilterAdapter
import timber.log.Timber

/**
 * Responsible for loading data from the various sources. Instantiating classes are responsible for
 * providing the [onDataLoaded] method to do something with the data.
 */
@SuppressLint("LogConditional")
abstract class DataManager(private val context: Activity, private val filterAdapter: FilterAdapter) :
        BaseDataManager<List<ProductItem>>(context) {

    private lateinit var pageIndexes: HashMap<String, Int>
    private var queries: HashMap<String, Query> = HashMap(0)
    private val prefs: PhoenixClient = phoenixClient

    private val filterListener = object : FilterAdapter.FiltersChangedCallbacks() {
        /** Call when filters change */
        override fun onFiltersChanged(changedFilter: Source) {
            if (changedFilter.active) {
                loadSource(changedFilter)
            } else {
                // filter deactivated
                val key = changedFilter.key
                if (queries.containsKey(key)) {
                    queries.remove(key)
                }
            }
        }
    }

    init {
        filterAdapter.registerFilterChangedCallback(filterListener)
        setupPageIndexes()
    }

    /** Cancel loading call */
    override fun cancelLoading() {
        if (queries.size > 0) {
            queries.clear()
        }
    }

    private fun setupPageIndexes() {
        val dataSources = filterAdapter.filters
        pageIndexes = HashMap(0)
        for (source in dataSources) {
            pageIndexes.put(key = source.key, value = 0)
        }
    }

    /** Load all [ProductItem.dataSource]s */
    fun loadAllDataSources() {
        for (filter in filterAdapter.filters) {
            loadSource(filter)
        }
    }

    private fun getNextPageIndex(dataSource: String): Int {
        var nextPage = 1 // default to one – i.e. for newly added sources
        if (pageIndexes.containsKey(dataSource)) {
            nextPage = pageIndexes[dataSource]!! + 1
        }
        pageIndexes.put(dataSource, nextPage)
        return nextPage
    }

    private fun sourceIsEnabled(key: String): Boolean {
        return pageIndexes[key] != 0
    }

    private fun loadSource(source: Source) {
        if (source.active) {
            loadStarted()
            getNextPageIndex(source.key)
            when (source.key) {
                SourceManager.SOURCE_HEALTH -> {
                    loadHealthSource()
                }
                SourceManager.SOURCE_FAVORITE -> {
                    loadFavoriteSource()
                }
                SourceManager.SOURCE_STUDENT -> {
                    loadStudentSource()
                }
                SourceManager.SOURCE_BUSINESS -> {
                    loadBusinessSource()
                }
                SourceManager.SOURCE_CLOTHING -> {
                    loadClothingSource()
                }
                SourceManager.SOURCE_ENTERTAINMENT -> {
                    loadEntertainmentSource()
                }
                SourceManager.SOURCE_KIDS -> {
                    loadKidsSource()
                }
                else -> {
                    if (source is Source.PhoenixSearchSource) {
                        loadSearchSource(source)
                    }
                }
            }
        }
    }

    /**
     * Load data source for kids
     */
    private fun loadKidsSource() {
        val get = prefs.db.document(PhoenixUtils.PRODUCTS_REF)
                .collection(PhoenixUtils.KIDS_REF)
                .orderBy("timestamp")
                .limit(50)

        queries[SourceManager.SOURCE_KIDS] = get
        get.addSnapshotListener(context, EventListener<QuerySnapshot?> { p0, p1 ->
            if (p1 != null) {
                loadFailed(p1.localizedMessage, SourceManager.SOURCE_KIDS)
                return@EventListener
            }
            val products = ArrayList<Product>(0)
            if (p0 != null) {
                for (doc in p0.documentChanges) {
                    if (doc.document.exists()) {
                        val docId = doc.document.id
                        val product = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        products.add(product)
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_KIDS)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_KIDS)
            }
        })
    }

    /**
     * Load data source for entertainment
     */
    private fun loadEntertainmentSource() {
        val get = prefs.db.document(PhoenixUtils.PRODUCTS_REF)
                .collection(PhoenixUtils.ENTERTAINMENT_REF)
                .orderBy("timestamp")
                .limit(50)
        queries[SourceManager.SOURCE_ENTERTAINMENT] = get
        get.addSnapshotListener(context, EventListener<QuerySnapshot?> { p0, p1 ->
            if (p1 != null) {
                loadFailed(p1.localizedMessage, SourceManager.SOURCE_ENTERTAINMENT)
                return@EventListener
            }
            val products = ArrayList<Product>(0)
            if (p0 != null) {
                for (doc in p0.documentChanges) {
                    if (doc.document.exists()) {
                        val docId = doc.document.id
                        val product = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        products.add(product)
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_ENTERTAINMENT)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_ENTERTAINMENT)
            }
        })
    }

    /**
     * Load data source for clothing
     */
    private fun loadClothingSource() {
        val get = prefs.db.document(PhoenixUtils.PRODUCTS_REF)
                .collection(PhoenixUtils.CLOTHING_REF)
                .orderBy("timestamp")
                .limit(50)
        queries[SourceManager.SOURCE_CLOTHING] = get
        get.addSnapshotListener(context, EventListener<QuerySnapshot?> { p0, p1 ->
            if (p1 != null) {
                loadFailed(p1.localizedMessage, SourceManager.SOURCE_CLOTHING)
                return@EventListener
            }
            val products = ArrayList<Product>(0)
            if (p0 != null) {
                for (doc in p0.documentChanges) {
                    if (doc.document.exists()) {
                        val docId = doc.document.id
                        val product = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        products.add(product)
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_CLOTHING)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_CLOTHING)
            }
        })
    }

    /**
     * Load data source for businesses
     */
    private fun loadBusinessSource() {
        val get = prefs.db.document(PhoenixUtils.PRODUCTS_REF)
                .collection(PhoenixUtils.BUSINESS_REF)
                .orderBy("timestamp")
                .limit(50)
        queries[SourceManager.SOURCE_BUSINESS] = get
        get.addSnapshotListener(context, EventListener<QuerySnapshot?> { p0, p1 ->
            if (p1 != null) {
                loadFailed(p1.localizedMessage, SourceManager.SOURCE_BUSINESS)
                return@EventListener
            }
            val products = ArrayList<Product>(0)
            if (p0 != null) {
                for (doc in p0.documentChanges) {
                    if (doc.document.exists()) {
                        val docId = doc.document.id
                        val product = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        products.add(product)
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_BUSINESS)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_BUSINESS)
            }
        })
    }

    /**
     * Load data source for students
     */
    private fun loadStudentSource() {
        val get = prefs.db.document(PhoenixUtils.PRODUCTS_REF)
                .collection(PhoenixUtils.STUDENT_REF)
                .orderBy("timestamp")
                .limit(50)
        queries.put(SourceManager.SOURCE_STUDENT, get)
        get.addSnapshotListener(context, EventListener<QuerySnapshot?> { p0, p1 ->
            if (p1 != null) {
                loadFailed(p1.localizedMessage, SourceManager.SOURCE_STUDENT)
                return@EventListener
            }
            val products = ArrayList<Product>(0)
            if (p0 != null) {
                for (doc in p0.documentChanges) {
                    if (doc.document.exists()) {
                        val docId = doc.document.id
                        val product = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        products.add(product)
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_STUDENT)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_STUDENT)
            }
        })
    }

    /**
     * Load data source for favorites
     */
    private fun loadFavoriteSource() {
        val get = prefs.db.document(PhoenixUtils.PRODUCTS_REF)
                .collection(PhoenixUtils.FAVORITE_REF)
                .orderBy("timestamp")
                .limit(50)
        queries.put(SourceManager.SOURCE_FAVORITE, get)
        get.addSnapshotListener(context, EventListener<QuerySnapshot?> { p0, p1 ->
            if (p1 != null) {
                loadFailed(p1.localizedMessage, SourceManager.SOURCE_FAVORITE)
                return@EventListener
            }
            val products = ArrayList<Product>(0)
            if (p0 != null) {
                for (doc in p0.documentChanges) {
                    if (doc.document.exists()) {
                        val docId = doc.document.id
                        val product = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        products.add(product)
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_FAVORITE)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_FAVORITE)
            }
        })
    }

    /**
     * Load data source for health
     */
    private fun loadHealthSource() {
        val get = prefs.db.document(PhoenixUtils.PRODUCTS_REF)
                .collection(PhoenixUtils.HEALTH_REF)
                .orderBy("timestamp")
                .limit(50)
        queries.put(SourceManager.SOURCE_HEALTH, get)
        get.addSnapshotListener(context, EventListener<QuerySnapshot?> { p0, p1 ->
            if (p1 != null) {
                loadFailed(p1.localizedMessage, SourceManager.SOURCE_HEALTH)
                return@EventListener
            }
            val products = ArrayList<Product>(0)
            if (p0 != null) {
                for (doc in p0.documentChanges) {
                    if (doc.document.exists()) {
                        val docId = doc.document.id
                        val product = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        products.add(product)
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_HEALTH)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_HEALTH)
            }
        })
    }

    /**
     * Load data source for user's search
     */
    private fun loadSearchSource(source: Source.PhoenixSearchSource) {
        val get = prefs.db.document(PhoenixUtils.PRODUCTS_REF)
                .collection(source.query)
                .orderBy("timestamp")
                .limit(50)
        queries.put(source.key, get)
        get.addSnapshotListener(context, EventListener<QuerySnapshot?> { p0, p1 ->
            if (p1 != null) {
                loadFailed(p1.localizedMessage, source.key)
                return@EventListener
            }
            val products = ArrayList<Product>(0)
            if (p0 != null) {
                for (doc in p0.documentChanges) {
                    if (doc.document.exists()) {
                        val docId = doc.document.id
                        val product = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        products.add(product)
                    } else {
                        sourceLoaded(products, source.key)
                    }
                }
                sourceLoaded(products, source.key)
            }
        })
    }

    private fun loadFailed(exception: String?, key: String) {
        Timber.d("Exception: $exception")
        loadFinished()
        queries.remove(key)
    }

    private fun sourceLoaded(data: List<Product>?, key: String) {
        loadFinished()
        if (data != null && data.isNotEmpty() && sourceIsEnabled(key)) {
            setDataSource(data, key)
            onDataLoaded(data)
        }
        queries.remove(key)
    }

}
