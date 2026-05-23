package com.nmdlock.app.core

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * NMDLock Application class.
 * Entry point for Hilt dependency injection.
 */
@HiltAndroidApp
class NMDLockApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NMDLockApplication
            private set
    }
}
