package com.pinnacleimagingsystems.ambientviewer.main

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.pinnacleimagingsystems.ambientviewer.main.tasks.CopyTask
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

abstract class MainPresenter: ViewModel() {
    class State {
        val currentFile by lazy { MutableLiveData<File>() }
        val event by lazy { MutableLiveData<String>() }
    }

    val state = State()

    abstract fun onFileSelected(uri: Uri)
}

class MainPresenterImpl: MainPresenter() {
    companion object {
        private const val TAG = "MainPresenterImpl"
    }

    private lateinit var context: Application
    private lateinit var handler: Handler

    private val bgExecutor = Executors.newSingleThreadExecutor()
    private val mainExecutor = Executor { runnable -> handler.post(runnable) }

    fun init(context: Application) {
        this.context = context
        this.handler = Handler(Looper.getMainLooper())
    }

    override fun onFileSelected(uri: Uri) {
        val copy = CopyTask(context)

        bgExecutor.execute {
            val copyResult = copy.copyFile(uri)

            mainExecutor.execute {
                copy.deliverLoadResult(state, uri, copyResult)
            }
        }
    }
}
