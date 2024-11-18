package com.example.objectdetector

import com.example.objectdetector.detection.DetectionResult
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.objectdetector.camera.CameraManager
import com.example.objectdetector.detection.ObjectDetector
import com.example.objectdetector.utils.ImagePreprocessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var objectDetector: ObjectDetector
    private lateinit var previewView: PreviewView
    private lateinit var resultsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        resultsTextView = findViewById(R.id.resultsTextView)
        val captureButton: Button = findViewById(R.id.captureButton)

        checkCameraPermission()

        captureButton.setOnClickListener {
            val tempFile = File(cacheDir, "captured_image.jpg")
            cameraManager.captureImage(tempFile) { file ->
                processCapturedImage(file)
            }
        }
    }

    // Check if camera permission is granted
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Start the camera after permission is granted
    private fun startCamera() {
        // Initialize cameraManager and start camera after permission is granted
        cameraManager = CameraManager(this)
        cameraManager.startCamera(this, previewView)

        objectDetector = ObjectDetector("model.tflite", "labels.txt", this)
    }


    // Handle the result of the camera permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processCapturedImage(file: File) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = ImagePreprocessor.decodeImageFile(file)
                val inputBuffer = ImagePreprocessor.preprocessImage(bitmap, 300)
                // Remove the second argument (300), as it's not required by the detect method
                val results = objectDetector.detect(inputBuffer)
                withContext(Dispatchers.Main) {
                    displayResults(results)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun displayResults(results: List<DetectionResult>) {
        val resultText = if (results.isEmpty()) {
            "No objects detected."
        } else {
            results.joinToString("\n") { result ->
                "${result.label}: ${"%.2f".format(result.confidence * 100)}% (Box: ${result.boundingBox.joinToString(", ")})"
            }
        }
        resultsTextView.text = "Detection Results:\n$resultText"
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }
}
