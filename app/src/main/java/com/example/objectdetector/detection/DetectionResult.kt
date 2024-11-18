package com.example.objectdetector.detection

data class DetectionResult(
    val label: String,
    val confidence: Float,
    val boundingBox: FloatArray
)