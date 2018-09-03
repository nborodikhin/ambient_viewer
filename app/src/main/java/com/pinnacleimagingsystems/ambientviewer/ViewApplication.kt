package com.pinnacleimagingsystems.ambientviewer

import android.app.Application
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.ProcessLifecycleOwner

class ViewApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        Deps.init(this)
    }

    private val lifecycleObserver = object: LifecycleObserver {
        // TODO: subcsribe to sensor and camera
    }
}