package com.studio21.android.util

import android.content.Context

/**
 * Created by Dmitriy on 30.01.2018.
 */
class Preferences {

    companion object {

        private fun getPreferences(context: Context) = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        private fun getEditor(context: Context) = getPreferences(context).edit()

        private val VOLUME_PREF = "volume_pref"

        fun setVolume(context: Context, volume: Float) =
                getEditor(context).putFloat(VOLUME_PREF, volume).apply()

        fun getVolume(context: Context): Float =
                getPreferences(context).getFloat(VOLUME_PREF, 0.4F)
    }
}