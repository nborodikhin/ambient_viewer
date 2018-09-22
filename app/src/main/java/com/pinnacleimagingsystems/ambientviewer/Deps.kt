package com.pinnacleimagingsystems.ambientviewer

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.pinnacleimagingsystems.ambientviewer.storage.DataStorage
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object Deps {
    val bgExecutor: Executor = Executors.newSingleThreadExecutor()
    lateinit var mainExecutor: Executor
    lateinit var dataStorage: DataStorage

    lateinit var prefs: SharedPreferences

    fun createAlgorithm(): Algorithm
        = AlgorithImpl()

    private lateinit var mainHandler: Handler

    fun init(applicaionContext: Context) {
        mainHandler = Handler(Looper.getMainLooper())
        mainExecutor = Executor { runnable -> mainHandler.post(runnable) }
        dataStorage = DataStorage.Dummy()
        prefs = applicaionContext.getSharedPreferences("Prefs", Context.MODE_PRIVATE)
    }
}