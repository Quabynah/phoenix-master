/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.support.annotation.NonNull
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import io.pergasus.R
import io.pergasus.api.PhoenixClient
import io.pergasus.api.Source
import io.pergasus.api.Source.SourceComparator
import io.pergasus.api.SourceManager
import io.pergasus.ui.recyclerview.FilterSwipeDismissListener
import io.pergasus.util.AnimUtils
import io.pergasus.util.ColorUtils
import io.pergasus.util.ViewUtils
import java.util.*

class FilterAdapter(private val host: Activity, val filters: MutableList<Source>,
                    private val authoriser: FilterAuthoriser) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>(),
        FilterSwipeDismissListener, PhoenixClient.AppLoginStatusListener {
    private val context: Context = host.applicationContext
    private var callbacks: MutableList<FiltersChangedCallbacks>? = null

    init {
        setHasStableIds(true)
    }

    val enabledSourcesCount: Int
        get() {
            return filters.count { it.active }
        }

    interface FilterAuthoriser {
        fun requestAuthorisation(sharedElement: View, forSource: Source)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val holder = FilterViewHolder(LayoutInflater.from(parent
                .context).inflate(R.layout.filter_item, parent, false))
        holder.itemView.setOnClickListener(View.OnClickListener {
            val position = holder.adapterPosition
            if (position == RecyclerView.NO_POSITION) return@OnClickListener
            val filter = filters[position]
            if (isAuthorisedPhoenixSource(filter) && !PhoenixClient(holder.itemView.context).isLoggedIn) {
                authoriser.requestAuthorisation(holder.filterIcon, filter)
            } else {
                filter.active = !filter.active
                holder.filterName.isEnabled = filter.active
                notifyItemChanged(position, if (filter.active)
                    FILTER_ENABLED
                else
                    FILTER_DISABLED)
                SourceManager.updateSource(filter, holder.itemView.context)
                dispatchFiltersChanged(filter)
            }
        })
        return holder
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = filters[position]
        if (holder != null) {
            holder.isSwipeable = filter.isSwipeDismissable
            holder.filterName.text = filter.name
            holder.filterName.isEnabled = filter.active
            if (filter.iconRes > 0) {
                holder.filterIcon.setImageDrawable(
                        holder.itemView.context.getDrawable(filter.iconRes))
            }
            holder.filterIcon.imageAlpha = if (filter.active)
                FILTER_ICON_ENABLED_ALPHA
            else
                FILTER_ICON_DISABLED_ALPHA
        }
    }

    override fun onBindViewHolder(holder: FilterViewHolder,
                                  position: Int,
                                  payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            // if we're doing a partial re-bind i.e. an item is enabling/disabling or being
            // highlighted then data hasn't changed. Just set state based on the payload
            val filterEnabled = payloads.contains(FILTER_ENABLED)
            val filterDisabled = payloads.contains(FILTER_DISABLED)
            if (filterEnabled || filterDisabled) {
                holder.filterName.isEnabled = filterEnabled
            }
        }
    }

    override fun getItemCount(): Int {
        return filters.size
    }

    override fun getItemId(position: Int): Long {
        return filters[position].key.hashCode().toLong()
    }

    override fun onItemDismiss(position: Int) {
        val removing = filters[position]
        if (removing.isSwipeDismissable) {
            removeFilter(removing)
        }
    }

    override fun onUserLogin() {
    }

    override fun onUserLogout() {
        for (i in filters.indices) {
            val filter = filters[i]
            if (filter.active && isAuthorisedPhoenixSource(filter)) {
                filter.active = false
                SourceManager.updateSource(filter, context)
                dispatchFiltersChanged(filter)
                notifyItemChanged(i, FILTER_DISABLED)
            }
        }
    }

    /**
     * Adds a new data source to the list of filters. If the source already exists then it is simply
     * activated.
     *
     * @param toAdd the source to add
     * @return whether the filter was added (i.e. if it did not already exist)
     */
    fun addFilter(toAdd: Source): Boolean {
        // first check if it already exists
        val count = filters.size
        for (i in 0 until count) {
            val existing = filters[i]
            if (existing == toAdd && existing.key.equals(toAdd.key, ignoreCase = true)) {
                // already exists, just ensure it's active
                if (!existing.active) {
                    existing.active = true
                    dispatchFiltersChanged(existing)
                    notifyItemChanged(i, FILTER_ENABLED)
                    SourceManager.updateSource(existing, context)
                }
                return false
            }
        }
        // didn't already exist, so add it
        filters.add(toAdd)
        Collections.sort(filters, SourceComparator())
        dispatchFiltersChanged(toAdd)
        notifyDataSetChanged()
        SourceManager.addSource(toAdd, context)
        return true
    }

    private fun removeFilter(removing: Source) {
        val position = filters.indexOf(removing)
        filters.removeAt(position)
        notifyItemRemoved(position)
        dispatchFilterRemoved(removing)
        SourceManager.removeSource(removing, context)
    }

    fun getFilterPosition(filter: Source): Int {
        return filters.indexOf(filter)
    }

    fun enableFilterByKey(@NonNull key: String, @NonNull context: Context) {
        val count = filters.size
        for (i in 0 until count) {
            val filter = filters[i]
            if (filter.key == key) {
                if (!filter.active) {
                    filter.active = true
                    notifyItemChanged(i, FILTER_ENABLED)
                    dispatchFiltersChanged(filter)
                    SourceManager.updateSource(filter, context)
                }
                return
            }
        }
    }

    fun highlightFilter(adapterPosition: Int) {
        notifyItemChanged(adapterPosition, HIGHLIGHT)
    }

    fun registerFilterChangedCallback(callback: FiltersChangedCallbacks) {
        if (callbacks == null) {
            callbacks = ArrayList(0)
        }
        callbacks!!.add(callback)
    }

    fun unregisterFilterChangedCallback(callback: FiltersChangedCallbacks) {
        if (callbacks != null && !callbacks!!.isEmpty()) {
            callbacks!!.remove(callback)
        }
    }

    private fun isAuthorisedPhoenixSource(source: Source): Boolean {
        return (source.key.equals(SourceManager.SOURCE_FAVORITE, ignoreCase = true)
                || source.key.equals(SourceManager.SOURCE_HEALTH, ignoreCase = true)
                || source.key.equals(SourceManager.SOURCE_BUSINESS, ignoreCase = true)
                || source.key.equals(SourceManager.SOURCE_STUDENT, ignoreCase = true))
    }

    private fun dispatchFiltersChanged(filter: Source) {
        if (callbacks != null && !callbacks!!.isEmpty()) {
            for (callback in callbacks!!) {
                callback.onFiltersChanged(filter)
            }
        }
    }

    private fun dispatchFilterRemoved(filter: Source) {
        if (callbacks != null && !callbacks!!.isEmpty()) {
            for (callback in callbacks!!) {
                callback.onFilterRemoved(filter)
            }
        }
    }

    abstract class FiltersChangedCallbacks {
        open fun onFiltersChanged(changedFilter: Source) {}
        open fun onFilterRemoved(removed: Source) {}
    }

    class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var filterName: TextView = itemView.findViewById(R.id.filter_name)
        var filterIcon: ImageView = itemView.findViewById(R.id.filter_icon)
        var isSwipeable: Boolean = false

    }

    class FilterAnimator : DefaultItemAnimator() {

        override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
            return true
        }

        override fun obtainHolderInfo(): RecyclerView.ItemAnimator.ItemHolderInfo {
            return FilterHolderInfo()
        }

        /* package */ internal class FilterHolderInfo : RecyclerView.ItemAnimator.ItemHolderInfo() {
            var doEnable: Boolean = false
            var doDisable: Boolean = false
            var doHighlight: Boolean = false
        }

        override fun recordPreLayoutInformation(state: RecyclerView.State,
                                                viewHolder: RecyclerView.ViewHolder,
                                                changeFlags: Int,
                                                payloads: List<Any>): RecyclerView.ItemAnimator.ItemHolderInfo {
            val info = super.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads) as FilterHolderInfo
            if (!payloads.isEmpty()) {
                info.doEnable = payloads.contains(FILTER_ENABLED)
                info.doDisable = payloads.contains(FILTER_DISABLED)
                info.doHighlight = payloads.contains(HIGHLIGHT)
            }
            return info
        }

        override fun animateChange(oldHolder: RecyclerView.ViewHolder,
                                   newHolder: RecyclerView.ViewHolder,
                                   preInfo: RecyclerView.ItemAnimator.ItemHolderInfo,
                                   postInfo: RecyclerView.ItemAnimator.ItemHolderInfo): Boolean {
            if (newHolder is FilterViewHolder && preInfo is FilterHolderInfo) {

                if (preInfo.doEnable || preInfo.doDisable) {
                    val iconAlpha = ObjectAnimator.ofInt(newHolder.filterIcon,
                            ViewUtils.IMAGE_ALPHA,
                            if (preInfo.doEnable)
                                FILTER_ICON_ENABLED_ALPHA
                            else
                                FILTER_ICON_DISABLED_ALPHA)
                    iconAlpha.duration = 300L
                    iconAlpha.interpolator = AnimUtils.getFastOutSlowInInterpolator(newHolder
                            .itemView.context)
                    iconAlpha.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            dispatchChangeStarting(newHolder, false)
                            newHolder.itemView.setHasTransientState(true)
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            newHolder.itemView.setHasTransientState(false)
                            dispatchChangeFinished(newHolder, false)
                        }
                    })
                    iconAlpha.start()
                } else if (preInfo.doHighlight) {
                    val highlightColor = ContextCompat.getColor(newHolder.itemView.context, R.color.accent)
                    val fadeFromTo = ColorUtils.modifyAlpha(highlightColor, 0)
                    val highlightBackground = ObjectAnimator.ofArgb(
                            newHolder.itemView,
                            ViewUtils.BACKGROUND_COLOR,
                            fadeFromTo,
                            highlightColor,
                            fadeFromTo)
                    highlightBackground.duration = 1000L
                    highlightBackground.interpolator = LinearInterpolator()
                    highlightBackground.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            dispatchChangeStarting(newHolder, false)
                            newHolder.itemView.setHasTransientState(true)
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            newHolder.itemView.background = null
                            newHolder.itemView.setHasTransientState(false)
                            dispatchChangeFinished(newHolder, false)
                        }
                    })
                    highlightBackground.start()
                }
            }
            return super.animateChange(oldHolder, newHolder, preInfo, postInfo)
        }

    }

    companion object {
        //Filters
        private const val FILTER_ENABLED = 2
        private const val FILTER_DISABLED = 3
        private const val HIGHLIGHT = 4
        private const val FILTER_ICON_ENABLED_ALPHA = 179 // 70%
        private const val FILTER_ICON_DISABLED_ALPHA = 51 // 20%
    }


}
