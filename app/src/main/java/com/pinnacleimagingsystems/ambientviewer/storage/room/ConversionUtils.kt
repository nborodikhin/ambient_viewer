package com.pinnacleimagingsystems.ambientviewer.storage.room

import com.pinnacleimagingsystems.ambientviewer.storage.DataPoint
import java.util.concurrent.TimeUnit

fun DataPoint.toDb(): DbDataPoint = DbDataPoint().apply {
    fileName = this@toDb.fileName
    parameter = this@toDb.slider
    lux = this@toDb.viewingConditionLux
    time = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
}


