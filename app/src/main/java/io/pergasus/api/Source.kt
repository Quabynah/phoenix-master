/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.support.annotation.DrawableRes
import io.pergasus.R
import java.util.*

open class Source(val key: String,
                  val sortOrder: Int,
                  val name: String,
                  @param:DrawableRes @field:DrawableRes
                  val iconRes: Int,
                  var active: Boolean) {

    open val isSwipeDismissable: Boolean
        get() = false

    open class PhoenixSource(key: String, sortOrder: Int, name: String, active: Boolean) : Source
    (key, sortOrder, name, R.drawable.ic_stat_ic_notification, active)

    class PhoenixSearchSource(val query: String, active: Boolean) : PhoenixSource(QUERY_PREFIX + query, SEARCH_SORT_ORDER, "“$query”", active) {

        override val isSwipeDismissable: Boolean
            get() = true

        companion object {
            const val QUERY_PREFIX = "QUERY_PREFIX"
            const val SEARCH_SORT_ORDER = 200
        }
    }

    class SourceComparator : Comparator<Source> {

        override fun compare(t: Source, t1: Source): Int {
            return t.sortOrder - t1.sortOrder
        }
    }
}
