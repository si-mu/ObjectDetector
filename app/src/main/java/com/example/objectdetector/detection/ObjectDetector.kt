package com.example.objectdetector.detection

import android.content.Context
import android.util.Log
import com.example.objectdetector.utils.FileUtil
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer

class ObjectDetector(modelPath: String, labelPath: String, context: Context) {

    private val interpreter: Interpreter
    private val labels: List<String>

    init {
        try {
            // Load the model and labels
            val modelBuffer = FileUtil.loadMappedFile(context, modelPath)
            interpreter = Interpreter(modelBuffer, Interpreter.Options().apply { numThreads = 4 })
            labels = FileUtil.loadLabels(context, labelPath)

            // Ensure labels match model output
            if (labels.isEmpty()) {
                throw IllegalArgumentException("Labels file cannot be empty.")
            }
        } catch (e: Exception) {
            Log.e("ObjectDetector", "Error loading model or labels: ${e.message}", e)
            throw RuntimeException("Error initializing ObjectDetector")
        }
    }

    fun detect(inputBuffer: ByteBuffer): List<DetectionResult> {
        // Model outputs:
        val outputLocations = Array(1) { Array(10) { FloatArray(4) } } // Bounding boxes
        val outputClasses = Array(1) { FloatArray(10) }                // Class indices
        val outputScores = Array(1) { FloatArray(10) }                 // Confidence scores
        val outputDetectionCount = FloatArray(1)                      // Number of detections

        try {
            // Run inference
            val outputMap = mapOf(
                0 to outputLocations,
                1 to outputClasses,
                2 to outputScores,
                3 to outputDetectionCount
            )
            interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)
        } catch (e: Exception) {
            Log.e("ObjectDetector", "Error during detection: ${e.message}", e)
            return emptyList()
        }

        // Parse detections
        val detectionCount = outputDetectionCount[0].toInt()
        return (0 until detectionCount).mapNotNull { i ->
            val confidence = outputScores[0][i]
            if (confidence > 0.6) { // Confidence threshold
                val classIndex = outputClasses[0][i].toInt()
                val label = labels.getOrNull(classIndex) ?: "Unknown"

                // Convert bounding box from FloatArray to List<Float>
                val boundingBox = outputLocations[0][i]

                DetectionResult(
                    label = label,
                    confidence = confidence,
                    boundingBox = boundingBox // Use FloatArray directly
                )
            } else null
        }
    }
}
