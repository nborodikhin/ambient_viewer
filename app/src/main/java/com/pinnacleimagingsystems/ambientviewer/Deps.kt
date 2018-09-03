package com.pinnacleimagingsystems.ambientviewer

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object Deps {
    val bgExecutor: Executor = Executors.newSingleThreadExecutor()
    lateinit var mainExecutor: Executor

    private lateinit var mainHandler: Handler

    fun init(applicaionContext: Context) {
        mainHandler = Handler(Looper.getMainLooper())
        mainExecutor = Executor { runnable -> mainHandler.post(runnable) }
    }
}