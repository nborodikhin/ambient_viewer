package com.pinnacleimagingsystems.ambientviewer.app

import android.app.Application
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.ProcessLifecycleOwner
import com.pinnacleimagingsystems.ambientviewer.Deps
import com.pinnacleimagingsystems.ambientviewer.als.LightSensor

class ViewApplication: Application(), LightSensor.Holder {
    override fun getLightSensor(): LightSensor = lightSensor

    private val lightSensor = LightSensor()

    override fun onCreate() {
        super.onCreate()
        Deps.init(this)

        lightSensor.initSensor(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        ProcessLifecycleOwner.get().lifecycle.addObserver(lightSensor)
    }

    private val lifecycleObserver = object: LifecycleObserver {
        // TODO: subcsribe to sensor and camera
    }
}