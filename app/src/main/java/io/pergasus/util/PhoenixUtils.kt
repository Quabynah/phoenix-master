/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util

import `in`.uncod.android.bypass.style.TouchableUrlSpan
import android.content.res.ColorStateList
import android.support.annotation.ColorInt
import android.text.Spanned
import android.text.TextUtils
import android.widget.TextView
import io.pergasus.ui.span.CustomerSpan
import okhttp3.HttpUrl

/** Utility for handling phoenix links  */
object PhoenixUtils {

    @JvmStatic
    /**
     * An extension to [HtmlUtils.parseHtml] which adds Phoenix
     * specific behaviour.
     */
    fun parsePhoenixHtml(
            input: String,
            linkTextColor: ColorStateList,
            @ColorInt linkHighlightColor: Int): Spanned {
        val ssb = HtmlUtils.parseHtml(input, linkTextColor, linkHighlightColor)

        val urlSpans = ssb.getSpans(0, ssb.length, TouchableUrlSpan::class.java)
        for (urlSpan in urlSpans) {
            val start = ssb.getSpanStart(urlSpan)
            if ("@" == ssb.subSequence(start, start + 1).toString()) {
                val end = ssb.getSpanEnd(urlSpan)
                ssb.removeSpan(urlSpan)
                val url = HttpUrl.parse(urlSpan.url)
                var playerId = -1L
                var playerUsername: String? = null
                try {
                    if (url != null) {
                        playerId = java.lang.Long.parseLong(url.pathSegments()[0])
                    }
                } catch (ignored: NumberFormatException) {
                    playerUsername = url!!.pathSegments()[0]
                }

                ssb.setSpan(CustomerSpan(urlSpan.url,
                        ssb.subSequence(start + 1, end).toString(),
                        playerId,
                        playerUsername,
                        linkTextColor,
                        linkHighlightColor),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return ssb
    }

    fun parseAndSetText(textView: TextView, input: String) {
        if (TextUtils.isEmpty(input)) return
        HtmlUtils.setTextWithNiceLinks(textView, parsePhoenixHtml(input,
                textView.linkTextColors, textView.highlightColor))
    }
}
