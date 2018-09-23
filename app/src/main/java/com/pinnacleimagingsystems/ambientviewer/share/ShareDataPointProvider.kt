package com.pinnacleimagingsystems.ambientviewer.share

import android.net.Uri
import android.os.Environment
import android.support.annotation.MainThread
import android.support.v4.content.FileProvider
import com.pinnacleimagingsystems.ambientviewer.Deps
import com.pinnacleimagingsystems.ambientviewer.R
import java.io.File
import java.io.FileWriter

object ShareDataPointProvider {
    private const val DATA_POINTS_FILE = "datapoints.csv"

    private fun getCsvFile(): File {
        val parent = Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        return File(parent, DATA_POINTS_FILE)
    }

    @MainThread
    fun prepareSharedFile(continuation: (Uri?) -> Unit) {
        Deps.dataStorage.getAllDataPoints { dataPoints ->
            val file = getCsvFile()
            file.parentFile.mkdirs()
            FileWriter(file).use { writer ->
                writer.write("fileName,parameter,lux,timestamp\r\n")

                dataPoints.forEach {
                    with(it) {
                        writer.write("$fileName,$slider,$viewingConditionLux,$timeStamp\r\n")
                    }
                }
            }
            val uri = FileProvider.getUriForFile(
                    Deps.applicationContext,
                    Deps.applicationContext.getString(R.string.share_authority),
                    file
            )
            continuation(uri)
        }
    }
}