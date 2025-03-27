package com.example.ocraksarajawa

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ocraksarajawa.databinding.ActivityMainBinding
import com.example.ocraksarajawa.model.TranslationRequest
import com.example.ocraksarajawa.model.TranslationResponse
import com.example.ocraksarajawa.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"
    private var dotCount = 0
    private val handler = android.os.Handler(Looper.getMainLooper())
    private lateinit var dotRunnable: Runnable

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity() // Menutup semua activity, keluar dari aplikasi
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sembunyikan translationResult di awal
        binding.translationResult.visibility = View.GONE
        binding.tvLoad.visibility = View.GONE

        binding.playButton.setOnClickListener {
            val inputText = binding.translationText.text.toString().trim()
            if (inputText.isNotEmpty()) {
                translateText(inputText)
                closeKeyboard()
            } else {
                Toast.makeText(this, "Masukkan teks untuk diterjemahkan", Toast.LENGTH_SHORT).show()
            }
        }



        binding.cameraButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        binding.btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.btnProfile.setOnClickListener {
            val intentProfile = Intent(this, ProfileActivity::class.java)
            startActivity(intentProfile)
        }
        binding.btnCopy.setOnClickListener {
            val textToCopy = binding.translationTextResult.text.toString()
            if (textToCopy.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Hasil Terjemahan", textToCopy)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(this, "Teks berhasil disalin ke clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Tidak ada teks untuk disalin", Toast.LENGTH_SHORT).show()
            }
        }
        binding.profileImage.setOnClickListener{
            val intentProfile = Intent(this, ProfileActivity::class.java)
            startActivity(intentProfile)
        }

    }
    private fun startTranslatingAnimation() {
        dotRunnable = object : Runnable {
            override fun run() {
                val dots = ".".repeat(dotCount % 4) //
                binding.tvLoad.text = "Translating$dots"
                dotCount++
                handler.postDelayed(this, 200) //
            }
        }
        handler.post(dotRunnable)
    }
    private fun stopTranslatingAnimation() {
        handler.removeCallbacks(dotRunnable)
        dotCount = 0
    }





    private fun translateText(text: String) {
        val request = TranslationRequest(text)

        // Tampilkan indikator loading dan sembunyikan hasil terjemahan sementara
        binding.tvLoad.visibility = View.VISIBLE
        binding.translationResult.visibility = View.GONE
        startTranslatingAnimation()

        ApiClient.instance.translateText(request).enqueue(object : Callback<TranslationResponse> {
            override fun onResponse(call: Call<TranslationResponse>, response: Response<TranslationResponse>) {
                binding.tvLoad.visibility = View.GONE // Sembunyikan loading
                stopTranslatingAnimation()

                if (response.isSuccessful) {
                    val translationResponse = response.body()
                    val translatedText = translationResponse?.cleaned_translation ?: "Gagal menerjemahkan"

                    // Tampilkan hasil terjemahan di EditText
                    binding.translationTextResult.setText(translatedText)

                    // Tampilkan translationResult dengan animasi
                    showTranslationResult()
                } else {
                    Toast.makeText(this@MainActivity, "Terjemahan gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Terjemahan gagal: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<TranslationResponse>, t: Throwable) {
                stopTranslatingAnimation()
                binding.tvLoad.visibility = View.GONE // Sembunyikan loading
                Toast.makeText(this@MainActivity, "Kesalahan jaringan: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Terjadi kesalahan: ${t.message}")
            }
        })
    }
    private fun closeKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }


    private fun showTranslationResult() {
        binding.translationResult.visibility = View.VISIBLE
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 500
        binding.translationResult.startAnimation(fadeIn)
    }
}
