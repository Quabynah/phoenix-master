/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util.onboarding

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import java.util.ArrayList

import io.pergasus.R

/**
 * Ready to use PaperOnboarding fragment
 */
class PaperOnboardingFragment : Fragment() {

    private var mOnChangeListener: PaperOnboardingOnChangeListener? = null
    private var mOnRightOutListener: PaperOnboardingOnRightOutListener? = null
    private var mOnLeftOutListener: PaperOnboardingOnLeftOutListener? = null
    var elements: ArrayList<PaperOnboardingPage>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            elements = arguments!!.get(ELEMENTS_PARAM) as ArrayList<PaperOnboardingPage>
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.onboarding_main_layout, container, false)

        // create engine for onboarding element
        val mPaperOnboardingEngine = PaperOnboardingEngine(view.findViewById(R.id.onboardingRootView), elements, activity!!.applicationContext)
        // set listeners
        mPaperOnboardingEngine.setOnChangeListener(mOnChangeListener!!)
        mPaperOnboardingEngine.setOnLeftOutListener(mOnLeftOutListener!!)
        mPaperOnboardingEngine.setOnRightOutListener(mOnRightOutListener!!)

        return view
    }

    fun setOnChangeListener(onChangeListener: PaperOnboardingOnChangeListener) {
        this.mOnChangeListener = onChangeListener
    }

    fun setOnRightOutListener(onRightOutListener: PaperOnboardingOnRightOutListener) {
        this.mOnRightOutListener = onRightOutListener
    }

    fun setOnLeftOutListener(onLeftOutListener: PaperOnboardingOnLeftOutListener) {
        this.mOnLeftOutListener = onLeftOutListener
    }

    companion object {

        private const val ELEMENTS_PARAM = "elements"


        fun newInstance(elements: ArrayList<PaperOnboardingPage>): PaperOnboardingFragment {
            val fragment = PaperOnboardingFragment()
            val args = Bundle()
            args.putSerializable(ELEMENTS_PARAM, elements)
            fragment.arguments = args
            return fragment
        }
    }

}
