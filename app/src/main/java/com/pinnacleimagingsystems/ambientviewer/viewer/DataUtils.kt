package com.pinnacleimagingsystems.ambientviewer.viewer

import com.pinnacleimagingsystems.ambientviewer.storage.DataPoint

fun createDataPoint(
        algorithmParameters: AlgorithmParameters,
        viewingLux: Int
) = DataPoint(
        slider = algorithmParameters.slider,
        viewingConditionLux = viewingLux
)