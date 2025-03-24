package com.example.ocraksarajawa

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playButton.setOnClickListener {
            val inputText = binding.translationText.text.toString().trim()
            if (inputText.isNotEmpty()) {
                translateText(inputText)
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
    }

    private fun translateText(text: String) {
        val request = TranslationRequest(text)
        ApiClient.instance.translateText(request).enqueue(object : Callback<TranslationResponse> {
            override fun onResponse(call: Call<TranslationResponse>, response: Response<TranslationResponse>) {
                if (response.isSuccessful) {
                    val translationResponse = response.body()
                    val translatedText = translationResponse?.cleaned_translation ?: "Gagal menerjemahkan"

                    // Menampilkan hasil terjemahan di EditText
                    binding.translationText.setText(translatedText)
                } else {
                    Toast.makeText(this@MainActivity, "Terjemahan gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Terjemahan gagal: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<TranslationResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Kesalahan jaringan: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Terjadi kesalahan: ${t.message}")
            }
        })
    }
}
