package com.sully.momo

import android.app.Application
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Preferences.initialize(this)
        DriveIds.initialize(this)

        Timber.plant(Timber.DebugTree())
    }
}