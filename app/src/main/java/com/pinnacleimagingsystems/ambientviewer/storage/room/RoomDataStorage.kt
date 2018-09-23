package com.pinnacleimagingsystems.ambientviewer.storage.room

import android.arch.persistence.room.Room
import android.content.Context
import com.pinnacleimagingsystems.ambientviewer.Deps
import com.pinnacleimagingsystems.ambientviewer.storage.DataPoint
import com.pinnacleimagingsystems.ambientviewer.storage.DataStorage

class RoomDataStorage(applicationContext: Context): DataStorage {
    private val bgThread get() = Deps.bgExecutor
    private val mainThread get() = Deps.mainExecutor

    private val db = createDB(applicationContext)

    private fun createDB(applicationContext: Context) =
            Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::
                    class.java,
                    "DataPoints"
            ).apply {
                fallbackToDestructiveMigration()
            }.build()

    override fun saveDataPoint(dataPoint: DataPoint) {
        val dbDataPoint = dataPoint.toDb()

        bgThread.execute {
            db.dataPoints().addDataPoint(dbDataPoint)
        }
    }

    override fun getAllDataPoints(onRetrieved: (List<DataPoint>) -> Unit) {
        bgThread.execute {
            val result = db.dataPoints().allDataPoints().map { dbDataPoint ->
                dbDataPoint.toDataPoint()
            }

            mainThread.execute {
                onRetrieved(result)
            }
        }
    }
}