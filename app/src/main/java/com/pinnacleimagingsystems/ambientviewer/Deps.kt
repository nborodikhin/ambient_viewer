package com.pinnacleimagingsystems.ambientviewer

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object Deps {
    val bgExecutor: Executor = Executors.newSingleThreadExecutor()
    lateinit var mainExecutor: Executor

    lateinit var prefs: SharedPreferences

    fun createAlgorithm(): Algorithm
        = AlgorithImpl()

    private lateinit var mainHandler: Handler

    fun init(applicaionContext: Context) {
        mainHandler = Handler(Looper.getMainLooper())
        mainExecutor = Executor { runnable -> mainHandler.post(runnable) }
        prefs = applicaionContext.getSharedPreferences("Prefs", Context.MODE_PRIVATE)
    }
}