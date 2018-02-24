package com.studio21.android.view

import android.support.v4.app.Fragment
import android.view.View
import butterknife.ButterKnife
import butterknife.Unbinder

/**
 * Created by Dmitriy on 21.02.2018.
 */
open class Studio21Fragment : Fragment() {

    protected var unbinder: Unbinder? = null

    override fun onDestroyView() {
        super.onDestroyView()

        unbinder?.unbind()
    }

    protected fun butter(view: View) {
        unbinder = ButterKnife.bind(this, view)
    }
}