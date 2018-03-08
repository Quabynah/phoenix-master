/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import io.pergasus.R
import io.pergasus.api.PhoenixApplication
import io.pergasus.ui.widget.InkPageIndicator
import io.pergasus.util.bindView
import java.security.InvalidParameterException

/**
 * A miniature tutorial screen for new installation
 */
class WelcomeActivity : Activity(), ViewPager.OnPageChangeListener {

    private val pager: ViewPager by bindView(R.id.pager)
    private val pageIndicator: InkPageIndicator by bindView(R.id.indicator)
    private val container: ViewGroup by bindView(R.id.container)
    private val previous: Button by bindView(R.id.page_previous)
    private val next: Button by bindView(R.id.page_next)

    private lateinit var clientState: PhoenixApplication.Companion.PhoenixClientState
    private var currentPage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        clientState = PhoenixApplication.Companion.PhoenixClientState(this)

        pager.adapter = WelcomePagerAdapter()
        pager.pageMargin = resources.getDimensionPixelSize(R.dimen.spacing_micro)
        pageIndicator.setViewPager(pager)
        pager.addOnPageChangeListener(this)

        //button actions
        next.setOnClickListener({
            pager.currentItem = pager.currentItem++
        })
        previous.setOnClickListener({
            pager.currentItem = pager.currentItem--
        })

    }

    override fun onBackPressed() {
        startActivity(Intent(this@WelcomeActivity, HomeActivity::class.java))
        finish()
    }

    internal inner class WelcomePagerAdapter : PagerAdapter() {
        private var layoutInflater: LayoutInflater = LayoutInflater.from(this@WelcomeActivity)

        private var pageInformative: View? = null
        private var pageResponsive: View? = null
        private var pageSecured: View? = null
        private var pageReliable: View? = null
        private var pageAuthentication: View? = null
        private var pageGetStarted: View? = null

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view == `object`
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val layout = getPage(position, container)
            container.addView(layout)
            return layout
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getCount(): Int {
            return PAGES
        }

        private fun getPage(position: Int, container: ViewGroup): View {
            return when (position) {
                0 -> {
                    if (pageInformative == null) {
                        //todo: search feature
                        pageInformative = layoutInflater.inflate(R.layout.welcome_page_item, container, false)
                    }
                    pageInformative!!
                }
                1 -> {
                    if (pageResponsive == null) {
                        //todo: fast and responsive UX
                        pageResponsive = layoutInflater.inflate(R.layout.welcome_page_item, container, false)
                    }
                    pageResponsive!!
                }
                2 -> {
                    if (pageSecured == null) {
                        //todo: secured payment methods
                        pageSecured = layoutInflater.inflate(R.layout.welcome_page_item, container, false)
                    }
                    pageSecured!!
                }
                3 -> {
                    if (pageReliable == null) {
                        //todo: reliable wrt offline capability
                        pageReliable = layoutInflater.inflate(R.layout.welcome_page_item, container, false)
                    }
                    pageReliable!!
                }
                4 -> {
                    if (pageAuthentication == null) {
                        //todo: quick authentication using firebase AuthUI
                        pageAuthentication = layoutInflater.inflate(R.layout.welcome_page_item, container, false)
                    }
                    pageAuthentication!!
                }
                5 -> {
                    if (pageGetStarted == null) {
                        //todo: get started
                        pageGetStarted = layoutInflater.inflate(R.layout.welcome_page_item, container, false)
                    }
                    pageGetStarted!!
                }
                else -> throw InvalidParameterException("View not implemented")
            }
        }

    }

    override fun onPageScrollStateChanged(state: Int) {}

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        when (position) {
            0 -> {
                hidePrevious()
                currentPage = position
                setNextAction()
            }
            1, 2, 3, 4 -> {
                currentPage = position
                showPrevious()
                setNextAction()
            }
            else -> {
                hidePrevious()
                next.text = getString(R.string.get_started)
                next.setOnClickListener({ onBackPressed() })
            }
        }
    }

    private fun setNextAction() {
        next.text = getString(R.string.next)
    }

    private fun showPrevious() {
        previous.visibility = View.VISIBLE
        previous.setOnClickListener({
            pager.currentItem = pager.currentItem--
        })

    }

    private fun hidePrevious() {
        previous.visibility = View.INVISIBLE
    }

    companion object {
        private const val PAGES = 6
    }
}
