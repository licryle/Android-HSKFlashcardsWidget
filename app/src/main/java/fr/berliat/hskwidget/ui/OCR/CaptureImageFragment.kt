package fr.berliat.hskwidget.ui.OCR


import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.yalantis.ucrop.UCrop
import fr.berliat.hskwidget.databinding.FragmentOcrCaptureBinding
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CaptureImageFragment : Fragment() {
    private lateinit var viewBinding: FragmentOcrCaptureBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var incrementalOCR = false

    private val cropActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultUri: Uri? = UCrop.getOutput(result.data!!)
                resultUri?.let {
                    // Handle the cropped image Uri
                    handleCroppedImage(it)
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                cropError?.let {
                    Log.e(TAG, "Crop failed: $it")
                }
            }
        }

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

        incrementalOCR = arguments?.getBoolean("incrementalOCR", false) == true

        return viewBinding.root // Return the root view of the binding
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
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
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
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
            } else {
                startCamera()
            }
        }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            // Initialize image capture use case
            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
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
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}, starting recognition"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    startCropActivity(output.savedUri!!)
                }
            }
        )
    }

    private fun startCropActivity(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_image.jpg"))
        val options = UCrop.Options().apply {
            setCompressionQuality(100)
        }

        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(16f, 9f) // Set your desired aspect ratio
            .withMaxResultSize(1080, 1920) // Set max size for cropped image
            .withOptions(options)

        cropActivityResultLauncher.launch(uCrop.getIntent(requireContext()))
    }

    fun handleCroppedImage(uri: Uri) {
        Toast.makeText(requireContext(), "Cropped image: $uri", Toast.LENGTH_SHORT).show()

        try {
            // Convert the Uri to InputImage for OCR
            val inputImage = InputImage.fromFilePath(requireContext(), uri)

            // Call your OCR function
            recognizeText(inputImage)

            Toast.makeText(requireContext(), "Cropped image ready for OCR: $uri", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error processing cropped image", Toast.LENGTH_SHORT).show()
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun recognizeText(image: InputImage) {
        val options = ChineseTextRecognizerOptions.Builder()
            .build()

        val recognizer: TextRecognizer = TextRecognition.getClient(options)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                processTextRecognitionResult(visionText)
                Log.d(TAG, "Recognized text: ${visionText.text}")
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun processTextRecognitionResult(texts: Text) {
        val blocks: List<Text.TextBlock> = texts.textBlocks
        if (blocks.isEmpty()) {
            Toast.makeText(requireContext(), "No text found", Toast.LENGTH_SHORT).show()
            return
        }

        var concatText = ""
        for (i in blocks.indices) {
            val lines: List<Text.Line> = blocks[i].lines
            for (j in lines.indices) {
                val elements: List<Text.Element> = lines[j].elements
                for (k in elements.indices) {
                    Log.d(TAG, elements[k].text)
                    concatText += elements[k].text
                }
            }
            concatText += "\n\n"
        }

        val action = DisplayOCRFragmentDirections.displayOCR(concatText, incrementalOCR)
        findNavController().navigate(action)
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