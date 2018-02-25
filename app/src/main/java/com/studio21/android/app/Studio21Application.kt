package com.studio21.android.app

import android.app.Application
import android.support.v7.app.AppCompatDelegate

/**
 * Created by Dmitriy on 21.02.2018.
 */
class Studio21Application: Application() {

    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }
}