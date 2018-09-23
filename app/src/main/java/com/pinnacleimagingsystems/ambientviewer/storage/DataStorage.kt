package com.pinnacleimagingsystems.ambientviewer.storage

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData

private const val TAG = "DataStorage"

data class DataPoint(
        val fileName: String,
        val slider: Int,
        val viewingConditionLux: Int,
        val timeStamp: Long
)

interface DataStorage {
    fun saveDataPoint(dataPoint: DataPoint)

    fun getAllDataPoints(onRetrieved: (List<DataPoint>) -> Unit)

    class Dummy: DataStorage {
        override fun getAllDataPoints(onRetrieved: (List<DataPoint>) -> Unit) {
            android.util.Log.e(TAG, "getAllDataPoints")
        }

        override fun saveDataPoint(dataPoint: DataPoint) {
            android.util.Log.e(TAG, "saveDataPoint: $dataPoint")
        }
    }
}