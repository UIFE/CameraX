package com.cfe.cameraxeffects

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.extensions.BokehImageCaptureExtender
import androidx.camera.extensions.HdrImageCaptureExtender
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.cfe.cameraxeffects.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var preview:Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraSelector: CameraSelector
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind the layout
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.activity = this
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            bindPreview()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview() {
        preview = Preview.Builder().build()
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK) // Define the camera which you want to use.
            .build()

        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview)
        preview.setSurfaceProvider(binding.previewView.createSurfaceProvider(camera.cameraInfo))
        buildImageCapture()
    }

    private fun buildImageCapture() {
        val builder = ImageCapture.Builder()

        // For Bokeh effect (aka - Portrait mode)
        // Check if that mode is available on the device
        // If yes, enable that effect.
        val bokehImageCapture = BokehImageCaptureExtender.create(builder)
        if(bokehImageCapture.isExtensionAvailable(cameraSelector)) {
            // Enable the effect
            bokehImageCapture.enableExtension(cameraSelector)
        }

        // For HDR too - same process
        val hdrImageCapture = HdrImageCaptureExtender.create(builder)
        if(hdrImageCapture.isExtensionAvailable(cameraSelector)) {
            hdrImageCapture.enableExtension(cameraSelector)
        }

        imageCapture = builder
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(binding.root.display.rotation) // You can set your rotation but right now I am choosing the current screen rotation.
            .build()

        // This binding will make sure to initialize and destroy of various resources.
        // Developer need not to make it explicitly.

        // But if you want to make it custom then, you will have to make a custom lifecycle
        // and then attach this to that lifecycle.

        // Right now, we are using the lifecycle of current activity.

        // If you want to make a custom lifecycle visit cameraX library architecture on developers.android.com

        cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
    }

    fun captureImage() {
        val dialog = AlertDialog.Builder(this)
            .setMessage("Saving picture....")
            .setCancelable(false)
            .show()

        // getExternalFilesDir(null) -> This will use the storage which is allocated to each app
        // on android. By using this, I don't have to make separate permission call for storage.

        val file = File(getExternalFilesDir(null)?.absolutePath, System.currentTimeMillis().toString() + ".jpg")

        // Atomically creates a new, empty file named by this abstract pathname if
        // and only if a file with this name does not yet exist.
        file.createNewFile()

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputFileOptions,
            Executors.newSingleThreadExecutor(),
            object: ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        dialog.dismiss()
                        Toast.makeText(this@MainActivity, "Image saved!", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        dialog.dismiss()
                        Toast.makeText(this@MainActivity, exception.message, Toast.LENGTH_LONG).show()
                    }
                }

            }
        )

    }
}
