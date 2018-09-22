package com.pinnacleimagingsystems.ambientviewer.storage

private const val TAG = "DataStorage"

data class DataPoint(
        val slider: Int,
        val viewingConditionLux: Int
)

interface DataStorage {
    fun saveDataPoint(dataPoint: DataPoint)

    class Dummy: DataStorage {
        override fun saveDataPoint(dataPoint: DataPoint) {
            android.util.Log.e(TAG, "saveDataPoint: $dataPoint")
        }
    }
}