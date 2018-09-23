package com.pinnacleimagingsystems.ambientviewer.storage.room

import com.pinnacleimagingsystems.ambientviewer.storage.DataPoint

fun DataPoint.toDb(): DbDataPoint = DbDataPoint().apply {
    fileName = this@toDb.fileName
    parameter = this@toDb.slider
    lux = this@toDb.viewingConditionLux
    time = this@toDb.timeStamp
}

fun DbDataPoint.toDataPoint(): DataPoint = DataPoint(
        fileName = this@toDataPoint.fileName,
        slider = this@toDataPoint.parameter,
        viewingConditionLux = this@toDataPoint.lux,
        timeStamp = this@toDataPoint.time
)


