package com.pinnacleimagingsystems.ambientviewer.storage.room

import android.arch.persistence.room.*

@Entity(tableName = "DataPoint")
class DbDataPoint {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    @ColumnInfo(name = "file_name")
    var fileName: String = ""

    @ColumnInfo(name = "parameter")
    var parameter: Float = -1.0f

    @ColumnInfo(name = "lux")
    var lux: Int = -1

    @ColumnInfo(name = "time")
    var time: Long = -1
}

@Dao
interface DataPointDao {
    @Query(value = "SELECT * FROM DataPoint")
    fun allDataPoints(): List<DbDataPoint>

    @Insert
    fun addDataPoint(dataPoint: DbDataPoint)

    @Query("DELETE FROM DataPoint")
    fun deleteAll()
}

@Database(entities = [DbDataPoint::class], version = 3, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
        abstract fun dataPoints(): DataPointDao
}
