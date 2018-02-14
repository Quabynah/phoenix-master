/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import io.pergasus.data.Product

/**
 * Utility class for applying weights to a group of {@Link Product}s for sorting. Weighs products
 * relative to the most priced in the group.
 */
open class ProductWeigher: ProductItemSorting.ProductItemGroupWeigher<Product> {
    override fun weigh(items: List<Product>) {
        var maxLikes = 0f
        for (shot in items) {
            maxLikes = Math.max(maxLikes, shot.price?.toFloat()!!)
        }
        for (shot in items) {
            val weight = 1f - shot.price?.toFloat()!! / maxLikes
            shot.weight = shot.page + weight
        }
    }

}