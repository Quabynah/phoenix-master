/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.app.Activity
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.pergasus.data.Product
import timber.log.Timber

abstract class RelatedProductDataManager(private val context: Activity) : BaseDataManager<List<ProductItem>>(context) {
    private var queries: HashMap<String, Query> = HashMap(0)
    private val prefs: PhoenixClient = phoenixClient

    /** Cancel loading call */
    override fun cancelLoading() {
        if (queries.size > 0) {
            queries.clear()
        }
    }

    fun loadRelatedProductsFromSource(product: Product) {
        val sources = arrayOf(
                SourceManager.SOURCE_HEALTH,
                SourceManager.SOURCE_FAVORITE,
                SourceManager.SOURCE_STUDENT,
                SourceManager.SOURCE_BUSINESS,
                SourceManager.SOURCE_CLOTHING,
                SourceManager.SOURCE_ENTERTAINMENT,
                SourceManager.SOURCE_KIDS
        )
        for (source in sources) {
            loadRelatedSource(source, product)
        }
    }

    private fun loadRelatedSource(source: String, product: Product) {
        when (source) {
            SourceManager.SOURCE_HEALTH -> {
                loadHealthSource(product)
            }
            SourceManager.SOURCE_FAVORITE -> {
                loadFavoriteSource(product)
            }
            SourceManager.SOURCE_STUDENT -> {
                loadStudentSource(product)
            }
            SourceManager.SOURCE_BUSINESS -> {
                loadBusinessSource(product)
            }
            SourceManager.SOURCE_CLOTHING -> {
                loadClothingSource(product)
            }
            SourceManager.SOURCE_ENTERTAINMENT -> {
                loadEntertainmentSource(product)
            }
            SourceManager.SOURCE_KIDS -> {
                loadKidsSource(product)
            }
            else -> return
        }
    }

    private fun loadKidsSource(product: Product) {
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
                        val data = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        if (data.category!!.contains(product.category!!, true)
                                or data.price!!.contains(product.price!!, true)
                                or data.shop!!.contains(product.shop!!, true)) {
                            products.add(data)
                        }
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_KIDS)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_KIDS)
            }
        })
    }

    private fun loadEntertainmentSource(product: Product) {
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
                        val data = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        if (data.category!!.contains(product.category!!, true)
                                or data.price!!.contains(product.price!!, true)
                                or data.shop!!.contains(product.shop!!, true)) {
                            products.add(data)
                        }
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_ENTERTAINMENT)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_ENTERTAINMENT)
            }
        })
    }

    private fun loadClothingSource(product: Product) {
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
                        val data = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        if (data.category!!.contains(product.category!!, true)
                                or data.price!!.contains(product.price!!, true)
                                or data.shop!!.contains(product.shop!!, true)) {
                            products.add(data)
                        }
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_CLOTHING)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_CLOTHING)
            }
        })
    }

    private fun loadBusinessSource(product: Product) {
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
                        val data = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        if (data.category!!.contains(product.category!!, true)
                                or data.price!!.contains(product.price!!, true)
                                or data.shop!!.contains(product.shop!!, true)) {
                            products.add(data)
                        }
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_BUSINESS)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_BUSINESS)
            }
        })
    }

    private fun loadStudentSource(product: Product) {
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
                        val data = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        if (data.category!!.contains(product.category!!, true)
                                or data.price!!.contains(product.price!!, true)
                                or data.shop!!.contains(product.shop!!, true)) {
                            products.add(data)
                        }
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_STUDENT)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_STUDENT)
            }
        })
    }

    private fun loadFavoriteSource(product: Product) {
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
                        val data = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        if (data.category!!.contains(product.category!!, true)
                                or data.price!!.contains(product.price!!, true)
                                or data.shop!!.contains(product.shop!!, true)) {
                            products.add(data)
                        }
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_FAVORITE)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_FAVORITE)
            }
        })
    }

    private fun loadHealthSource(product: Product) {
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
                        val data = doc.document.toObject(Product::class.java).withId<Product>(docId)
                        if (data.category!!.contains(product.category!!, true)
                                or data.price!!.contains(product.price!!, true)
                                or data.shop!!.contains(product.shop!!, true)) {
                            products.add(data)
                        }
                    } else {
                        sourceLoaded(products, SourceManager.SOURCE_HEALTH)
                    }
                }
                sourceLoaded(products, SourceManager.SOURCE_HEALTH)
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
        if (data != null && data.isNotEmpty()) {
            setDataSource(data, key)
            onDataLoaded(data)
        }
        queries.remove(key)
    }
}