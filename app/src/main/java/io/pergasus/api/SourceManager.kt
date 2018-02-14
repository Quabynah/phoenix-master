/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.content.Context
import android.content.SharedPreferences
import io.pergasus.R
import java.util.*


object SourceManager {
    val SOURCE_FAVORITE = "SOURCE_FAVORITE"
    val SOURCE_HEALTH = "SOURCE_HEALTH"
    val SOURCE_CLOTHING = "SOURCE_CLOTHING"
    val SOURCE_BUSINESS = "SOURCE_BUSINESS"
    val SOURCE_ENTERTAINMENT = "SOURCE_ENTERTAINMENT"
    val SOURCE_KIDS = "SOURCE_KIDS"
    val SOURCE_STUDENT = "SOURCE_STUDENT"
    private val SOURCES_PREF = "SOURCES_PREF"
    private val KEY_SOURCES = "KEY_SOURCES"

    fun getSources(context: Context): List<Source> {
        val prefs = context.getSharedPreferences(SOURCES_PREF, Context.MODE_PRIVATE)
        val sourceKeys = prefs.getStringSet(KEY_SOURCES, null)
        if (sourceKeys == null) {
            setupDefaultSources(context, prefs.edit())
            return getDefaultSources(context)
        }
        val sources = ArrayList<Source>(sourceKeys.size)
        for (sourceKey in sourceKeys) {
            if (sourceKey.startsWith(Source.PhoenixSearchSource.QUERY_PREFIX)) {
                sources.add(Source.PhoenixSearchSource(
                        sourceKey.replace(Source.PhoenixSearchSource.QUERY_PREFIX, ""),
                        prefs.getBoolean(sourceKey, false)))
            } else {
                sources.add(getSource(context, sourceKey, prefs.getBoolean(sourceKey, false))!!)
            }
        }
        Collections.sort(sources, Source.SourceComparator())
        return sources
    }

    fun addSource(toAdd: Source, context: Context) {
        val prefs = context.getSharedPreferences(SOURCES_PREF, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val sourceKeys = prefs.getStringSet(KEY_SOURCES, null)
        sourceKeys?.add(toAdd.key)
        editor.putStringSet(KEY_SOURCES, sourceKeys)
        editor.putBoolean(toAdd.key, toAdd.active)
        editor.apply()
    }

    fun updateSource(source: Source, context: Context) {
        val editor = context.getSharedPreferences(SOURCES_PREF, Context.MODE_PRIVATE).edit()
        editor.putBoolean(source.key, source.active)
        editor.apply()
    }

    fun removeSource(source: Source, context: Context) {
        val prefs = context.getSharedPreferences(SOURCES_PREF, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val sourceKeys = prefs.getStringSet(KEY_SOURCES, null)
        sourceKeys?.remove(source.key)
        editor.putStringSet(KEY_SOURCES, sourceKeys)
        editor.remove(source.key)
        editor.apply()
    }

    private fun getSource(context: Context, key: String, active: Boolean): Source? {
        for (source in getDefaultSources(context)) {
            if (source.key == key) {
                source.active = active
                return source
            }
        }
        return null
    }

    private fun setupDefaultSources(context: Context, editor: SharedPreferences.Editor) {
        val defaultSources = getDefaultSources(context)
        val keys = HashSet<String>(defaultSources.size)
        for (source in defaultSources) {
            keys.add(source.key)
            editor.putBoolean(source.key, source.active)
        }
        editor.putStringSet(KEY_SOURCES, keys)
        editor.commit()
    }

    private fun getDefaultSources(context: Context): ArrayList<Source> {
        val defaultSources = ArrayList<Source>(7)
        defaultSources.add(Source.PhoenixSource(SOURCE_HEALTH, 100,
                context.getString(R.string.source_health), false))
        defaultSources.add(Source.PhoenixSource(SOURCE_CLOTHING, 101,
                context.getString(R.string.source_clothing), true))
        defaultSources.add(Source.PhoenixSource(SOURCE_BUSINESS, 102,
                context.getString(R.string.source_business), false))
        defaultSources.add(Source.PhoenixSource(SOURCE_ENTERTAINMENT, 103,
                context.getString(R.string.source_entertainment), true))
        defaultSources.add(Source.PhoenixSource(SOURCE_KIDS, 104,
                context.getString(R.string.source_kids), true))
        defaultSources.add(Source.PhoenixSource(SOURCE_STUDENT, 105,
                context.getString(R.string.source_students), false))
        //200 sort order range left for user searches
        defaultSources.add(Source.PhoenixSource(SOURCE_FAVORITE, 300,
                context.getString(R.string.source_favorite), false))
        defaultSources.add(Source.PhoenixSearchSource(context.getString(R.string.source_recommended_search),
                false))
        return defaultSources
    }

}
