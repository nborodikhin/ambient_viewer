package com.pinnacleimagingsystems.ambientviewer.viewer

import com.pinnacleimagingsystems.ambientviewer.storage.DataPoint
import java.util.concurrent.TimeUnit

fun createDataPoint(
        fileName: String,
        algorithmParameters: AlgorithmParameters,
        viewingLux: Int
) = DataPoint(
        fileName = fileName,
        slider = algorithmParameters.parameter,
        viewingConditionLux = viewingLux,
        timeStamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
)