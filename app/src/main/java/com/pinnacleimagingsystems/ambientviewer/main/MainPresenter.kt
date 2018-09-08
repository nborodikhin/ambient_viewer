package com.pinnacleimagingsystems.ambientviewer.main

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.net.Uri
import com.pinnacleimagingsystems.ambientviewer.ConsumableEvent
import com.pinnacleimagingsystems.ambientviewer.Deps
import com.pinnacleimagingsystems.ambientviewer.Prefs
import com.pinnacleimagingsystems.ambientviewer.tasks.CopyTask
import java.io.File

abstract class MainPresenter: ViewModel() {
    class State {
        sealed class Event {
            data class ViewFile(val file: String): Event()

            fun asConsumable(): ConsumableEvent<Event> = ConsumableEvent(this)
        }

        val currentFile by lazy { MutableLiveData<File>() }
        val eventDescription by lazy { MutableLiveData<String>() }
        val event by lazy { MutableLiveData<ConsumableEvent<Event>>() }
        val lastFile by lazy { MutableLiveData<File>() }
    }

    val state = State()

    abstract fun onFileSelected(uri: Uri)
    abstract fun onLastFileClicked()
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
        val lastFile = Deps.prefs.getString(Prefs.LAST_NAME, null)
        if (lastFile != null) {
            val file = File(lastFile)
            if (file.exists()) {
                state.lastFile.value = file
            }
        }
    }

    override fun onFileSelected(uri: Uri) {
        val copy = CopyTask(context)

        fun deliverLoadResult(state: MainPresenter.State, uri: Uri, copyResult: CopyTask.CopyResult) {
            when(copyResult) {
                is CopyTask.CopyResult.UnsupportedType -> {
                    state.eventDescription.value = "Failed: unsupported type ${copyResult.mimeType}"
                }
                is CopyTask.CopyResult.Failure -> {
                    state.eventDescription.value = "Failed: exception ${copyResult.exception}"
                }
                is CopyTask.CopyResult.Success -> {
                    val file = copyResult.file

                    state.eventDescription.value = "loaded file $file of ${copyResult.mimeType} from $uri"
                    state.currentFile.value = file
                    state.event.value = State.Event.ViewFile(file.absolutePath).asConsumable()

                    Deps.prefs.edit().apply{
                        putString(Prefs.LAST_NAME, file.absolutePath)
                    }.apply()
                    state.lastFile.value = file
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

    override fun onLastFileClicked() {
        val file = state.lastFile.value ?: return

        state.event.value = State.Event.ViewFile(file.absolutePath).asConsumable()
        state.eventDescription.value = "Loaded last: $file"
    }
}
