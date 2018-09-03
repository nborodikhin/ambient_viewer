package com.pinnacleimagingsystems.ambientviewer.main

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.net.Uri
import com.pinnacleimagingsystems.ambientviewer.Deps
import com.pinnacleimagingsystems.ambientviewer.tasks.CopyTask
import java.io.File

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

    private val bgExecutor = Deps.bgExecutor
    private val mainExecutor = Deps.mainExecutor

    fun init(context: Application) {
        this.context = context
    }

    override fun onFileSelected(uri: Uri) {
        val copy = CopyTask(context)

        fun deliverLoadResult(state: MainPresenter.State, uri: Uri, copyResult: CopyTask.CopyResult) {
            when(copyResult) {
                is CopyTask.CopyResult.UnsupportedType -> {
                    state.event.value = "Failed: unsupported type ${copyResult.mimeType}"
                }
                is CopyTask.CopyResult.Failure -> {
                    state.event.value = "Failed: exception ${copyResult.exception}"
                }
                is CopyTask.CopyResult.Success -> {
                    state.event.value = "loaded file ${copyResult.file} of ${copyResult.mimeType} from $uri"
                    state.currentFile.value = copyResult.file
                }
            }
        }

        bgExecutor.execute {
            val copyResult = copy.copyFile(uri)

            mainExecutor.execute {
                deliverLoadResult(state, uri, copyResult)
            }
        }
    }
}
