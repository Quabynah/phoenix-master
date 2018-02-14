/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import java.util.*

/**
 * Classes related to sorting [ProductItem]s.
 */
class ProductItemSorting {

    /**
     * A comparator that compares [ProductItem]s based on their `weight` attribute.
     */
    class ProductItemComparator : Comparator<ProductItem> {

        override fun compare(lhs: ProductItem, rhs: ProductItem): Int {
            return java.lang.Float.compare(lhs.weight, rhs.weight)
        }
    }

    /**
     * Interface for weighing a group of [ProductItem]s
     */
    interface ProductItemGroupWeigher<in T : ProductItem> {
        fun weigh(items: List<T>)
    }

    /**
     * Applies a weight to a group of [ProductItem]s according to their natural order.
     */
    class NaturalOrderWeigher : ProductItemGroupWeigher<ProductItem> {

        override fun weigh(items: List<ProductItem>) {
            val step = 1.0f / items.size.toFloat()
            for (i in items.indices) {
                val item = items[i]
                item.weight = item.page + i.toFloat() * step
            }
        }
    }
}

