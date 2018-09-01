package com.pinnacleimagingsystems.ambientviewer.main.tasks

import android.content.Context
import android.net.Uri
import android.support.annotation.MainThread
import android.support.annotation.WorkerThread
import android.util.Log
import com.pinnacleimagingsystems.ambientviewer.main.MainPresenter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class CopyTask(val context: Context) {
    companion object {
        private const val TAG = "CopyTask"
    }

    sealed class CopyResult {
        data class UnsupportedType(val mimeType: String): CopyResult()
        data class Success(val mimeType: String, val file: File): CopyResult()
        data class Failure(val exception: Exception): CopyResult()
    }

    @WorkerThread
    fun copyFile(uri: Uri): CopyResult {
        val contentResolver = context.contentResolver

        val mimeType = contentResolver.getType(uri)
        val stream = contentResolver.openInputStream(uri)

        if (mimeType != "image/jpeg") {
            return CopyResult.UnsupportedType(mimeType)
        }

        Log.e(TAG, "uri $uri")
        Log.e(TAG, "type $mimeType")

        try {
            stream.use {
                val file = copyStream(stream)
                return CopyResult.Success(mimeType, file)
            }
        } catch (e: Exception) {
            return CopyResult.Failure(e)
        }
    }

    @MainThread
    fun deliverLoadResult(state: MainPresenter.State, uri: Uri, copyResult: CopyResult) {
        when(copyResult) {
            is CopyResult.UnsupportedType -> {
                state.event.value = "Failed: unsupported type ${copyResult.mimeType}"
            }
            is CopyResult.Failure -> {
                state.event.value = "Failed: exception ${copyResult.exception}"
            }
            is CopyResult.Success -> {
                state.event.value = "loaded file ${copyResult.file} of ${copyResult.mimeType} from $uri"
                state.currentFile.value = copyResult.file
            }
        }
    }

    @WorkerThread
    private fun copyStream(stream: InputStream): File {
        val cacheDir = context.cacheDir
        val outputFile = File.createTempFile("file-", ".copy", cacheDir)

        val buffer = ByteArray(1024 * 1024)

        FileOutputStream(outputFile).use { out ->
            while (stream.available() > 0) {
                val bytes = stream.read(buffer, 0, buffer.size)
                out.write(buffer, 0, bytes)
            }
        }

        return outputFile
    }

}