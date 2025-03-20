package com.example.ocraksarajawa

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import com.example.ocraksarajawa.network.ApiService
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var buttonGallery: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        val buttonCapture: ImageButton = findViewById(R.id.buttonCapture)
        buttonGallery = findViewById(R.id.buttonGallery)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        buttonCapture.setOnClickListener {
            takePhoto()
        }

        buttonGallery.setOnClickListener {
            openGallery()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetRotation(windowManager.defaultDisplay.rotation) // Mengikuti rotasi layar
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }


            imageCapture = ImageCapture.Builder()
                .setTargetRotation(Surface.ROTATION_0)  // Paksa selalu dalam mode portrait
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraActivity", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                val croppedBitmap = cropToScannerArea(bitmap)
                showImagePopup(croppedBitmap)
                saveImageToGallery(croppedBitmap)
                image.close()
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraActivity", "Photo capture failed: ${exc.message}", exc)
            }
        })
    }
    private fun uploadImageToOCR(bitmap: Bitmap) {
        // Simpan gambar ke file sementara
        val file = File(cacheDir, "ocr_image.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()


        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())

        val imagePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

        // Retrofit Setup
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.0.108:8000/") // Ganti dengan IP server API-mu
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        // Kirim gambar ke API
        apiService.uploadImage(imagePart).enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.string()?.let { responseBody ->
                        try {
                            val jsonObject = JSONObject(responseBody)
                            val finalSentence = jsonObject.getString("final_sentence") // Ambil final_sentence

                            // Tampilkan hasil dalam Toast
                            runOnUiThread {
                                Toast.makeText(this@CameraActivity, "Hasil OCR: $finalSentence", Toast.LENGTH_LONG).show()
                            }

                            // Opsional: Tampilkan dalam Popup Dialog
                            showResultPopup(finalSentence)

                        } catch (e: Exception) {
                            Log.e("OCR Upload", "Error parsing JSON: ${e.message}")
                        }
                    }
                } else {
                    Log.e("OCR Upload", "Gagal: ${response.errorBody()?.string()}")
                    Toast.makeText(this@CameraActivity, "Gagal mengunggah gambar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("OCR Upload", "Error: ${t.message}")
                Toast.makeText(this@CameraActivity, "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_GALLERY && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val bitmap = uriToBitmap(uri)
                bitmap?.let {
                    val croppedBitmap = cropToScannerArea(it)
                    showImagePopup(croppedBitmap)
                }
            }
        }
    }
    private fun showResultPopup(text: String) {
        AlertDialog.Builder(this)
            .setTitle("Hasil OCR")
            .setMessage(text)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showImagePopup(bitmap: Bitmap) {
        val imageView = ImageView(this).apply {
            setImageBitmap(bitmap)
            adjustViewBounds = true
        }

        AlertDialog.Builder(this)
            .setTitle("Preview Gambar")
            .setView(imageView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                uploadImageToOCR(bitmap) // Kirim gambar ke API OCR setelah klik OK
            }
            .show()
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // Dapatkan rotasi gambar dari sensor kamera
        val rotation = image.imageInfo.rotationDegrees
        val matrix = android.graphics.Matrix()

        // Perbaiki rotasi agar sesuai orientasi normal
        matrix.postRotate(rotation.toFloat())

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }



    private fun cropToScannerArea(bitmap: Bitmap): Bitmap {
        // Dapatkan ukuran overlay dari previewView secara dinamis
        val overlayWidth = previewView.width * 0.5 // Misalnya 80% dari lebar preview
        val overlayHeight = previewView.height * 0.15 // Misalnya 30% dari tinggi preview

        // Hitung posisi crop di bitmap sesuai skala previewView terhadap gambar asli
        val scaleX = bitmap.width.toFloat() / previewView.width
        val scaleY = bitmap.height.toFloat() / previewView.height

        val cropX = ((previewView.width - overlayWidth) / 2 * scaleX).toInt()
        val cropY = ((previewView.height - overlayHeight) / 2 * scaleY).toInt()
        val cropWidth = (overlayWidth * scaleX).toInt()
        val cropHeight = (overlayHeight * scaleY).toInt()


        // Pastikan crop tidak keluar batas gambar
        return Bitmap.createBitmap(
            bitmap,
            cropX.coerceAtLeast(0),
            cropY.coerceAtLeast(0),
            cropWidth.coerceAtMost(bitmap.width - cropX),
            cropHeight.coerceAtMost(bitmap.height - cropY)
        )
    }
    private fun saveImageToGallery(bitmap: Bitmap) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "cropped_image_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OCR_AksaraJawa")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                Toast.makeText(this, "Gambar berhasil disimpan ke galeri", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show()
        }
    }



    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val REQUEST_CODE_GALLERY = 20
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
