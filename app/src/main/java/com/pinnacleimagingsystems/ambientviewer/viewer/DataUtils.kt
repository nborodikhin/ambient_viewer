package com.pinnacleimagingsystems.ambientviewer.viewer

import com.pinnacleimagingsystems.ambientviewer.storage.DataPoint

fun createDataPoint(
        fileName: String,
        algorithmParameters: AlgorithmParameters,
        viewingLux: Int
) = DataPoint(
        fileName = fileName,
        slider = algorithmParameters.slider,
        viewingConditionLux = viewingLux
)