package com.pinnacleimagingsystems.ambientviewer

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.pinnacleimagingsystems.ambientviewer.storage.DataStorage
import com.pinnacleimagingsystems.ambientviewer.storage.room.RoomDataStorage
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@SuppressLint("StaticFieldLeak")
object Deps {
    val bgExecutor: Executor = Executors.newSingleThreadExecutor()

    lateinit var applicationContext: Context
    lateinit var mainExecutor: Executor
    lateinit var dataStorage: DataStorage
    lateinit var contentResolver: ContentResolver

    lateinit var prefs: SharedPreferences

    lateinit var algorithm: Algorithm

    private lateinit var mainHandler: Handler

    fun init(applicationContext: Context) {
        this.applicationContext = applicationContext
        mainHandler = Handler(Looper.getMainLooper())
        mainExecutor = Executor { runnable -> mainHandler.post(runnable) }
        dataStorage = RoomDataStorage(this.applicationContext)
        contentResolver = applicationContext.contentResolver
        prefs = applicationContext.getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        algorithm = AlgorithImpl()
    }
}