package fr.berliat.hskwidget.ui.OCR

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.databinding.FragmentOcrCaptureBinding
import fr.berliat.hskwidget.domain.Utils
import java.time.Instant
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CaptureImageFragment : Fragment() {
    private lateinit var viewBinding: FragmentOcrCaptureBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentOcrCaptureBinding.inflate(inflater, container, false) // Inflate here

        cameraExecutor = Executors.newSingleThreadExecutor()

        return viewBinding.root // Return the root view of the binding
    }

    override fun onResume() {
        super.onResume()
        Utils.hideKeyboard(requireContext(), viewBinding.root)
        Utils.logAnalyticsScreenView("CaptureImage")
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(requireContext(),
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Permission request denied")
            } else {
                startCamera()
            }
        }

    private fun startCamera() {
        // Set scale type so the preview fills the view correctly
        val previewView: PreviewView = viewBinding.viewFinder
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            // Creating the 2 outputs and bind them into a unique useCaseGroup around a unique ViewPort.
            // This ensures we're capturing what we see on screen.
            // Thank you https://github.com/AyusmaTech/CameraRectCropSample
            // and https://stackoverflow.com/questions/59242315/how-to-crop-image-rectangle-in-camera-preview-on-camerax
            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val viewPort = ViewPort.Builder(Rational(previewView.width, previewView.height), Surface.ROTATION_0).build()
            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview) //your preview
                .addUseCase(imageCapture!!)
                .setViewPort(viewPort)
                .build()

            try {
                val cameraProvider = cameraProviderFuture.get()
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup)

                // Enable pinch-to-zoom on PreviewView
                val scaleGestureDetector = ScaleGestureDetector(
                    requireContext(),
                    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            val currentZoomRatio =
                                camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                            val delta = detector.scaleFactor
                            camera.cameraControl.setZoomRatio(currentZoomRatio * delta)
                            return true
                        }
                    })

                viewBinding.viewFinder.setOnTouchListener { view, event ->
                    view.performClick()
                    scaleGestureDetector.onTouchEvent(event)
                    true
                }

                Log.d(TAG, "Camera started")
            } catch(exc: Exception) {
                Log.e(TAG, "Couldn't start camera: binding failed", exc)

                Utils.logAnalyticsError(
                    "OCR_CAPTURE",
                    "CameraBindingFailed",
                    exc.message ?: ""
                )
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(Instant.now().toEpochMilli())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + getString(R.string.app_name) + " OCR")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)

                    Utils.logAnalyticsError(
                        "OCR_CAPTURE",
                        "PhotoCaptureFailed",
                        exc.message ?: ""
                    )
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Log.d(TAG, msg)
                    handleImage(output.savedUri!!)
                }
            }
        )
    }


    private fun handleImage(imageUri: Uri) {
        Log.i(TAG, "Processing image at path $imageUri")

        try {
            val action = CaptureImageFragmentDirections.displayOCR(imageUri.toString(), arguments?.getString("preText") ?: "")
            findNavController().navigate(action)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error processing cropped image", Toast.LENGTH_SHORT).show()

            Utils.logAnalyticsError(
                "OCR_CAPTURE",
                "ProcessingPictureFailed",
                e.message ?: ""
            )
        }
    }

    companion object {
        private const val TAG = "CaptureImageFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}