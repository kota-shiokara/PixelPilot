package jp.ikanoshiokara.pixelpilot

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ComponentActivity
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

@Composable
fun PreviewView(
    modifier: Modifier = Modifier,
    onSuccess: (List<String>) -> Unit = {},
    onFailure: (Exception) -> Unit = {},
) {
    AndroidView(
        factory = { context ->
            val cameraExecutor = Executors.newSingleThreadExecutor()
            val previewView = PreviewView(context).also {
                it.scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageCapture = ImageCapture.Builder().build()

                val imageAnalyzer = ImageAnalysis.Builder()
                    .build()
                    .also {
                        it.setAnalyzer(
                            cameraExecutor,
                            BarcodeAnalyser(
                                onSuccess = { barcodeValue ->
                                    val rawValue = barcodeValue.filterNotNull()
                                    onSuccess(rawValue)
                                },
                                onFailure = onFailure
                            )
                        )
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        context as ComponentActivity,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalyzer
                    )

                } catch (e: Exception) {
                    onFailure(e)
                }
            }, ContextCompat.getMainExecutor(context))
            previewView
        },
        modifier = modifier
    )
}