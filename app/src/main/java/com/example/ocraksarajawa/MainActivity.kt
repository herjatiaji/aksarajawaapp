package com.example.ocraksarajawa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ocraksarajawa.databinding.ActivityMainBinding
import com.example.ocraksarajawa.network.ApiClient
import com.example.ocraksarajawa.network.TranslationRequest
import com.example.ocraksarajawa.network.TranslationResponse
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
            val inputText = binding.translationText.text.toString()
            if (inputText.isNotEmpty()) {
                translateText(inputText)
            } else {
                Toast.makeText(this, "Masukkan teks untuk diterjemahkan", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cameraButton.setOnClickListener{
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }
        binding.btnHome.setOnClickListener{
            Toast.makeText(this,"Home button Clicked",Toast.LENGTH_LONG).show()
        }

        binding.btnProfile.setOnClickListener{
            Toast.makeText(this,"Profile button Clicked",Toast.LENGTH_LONG).show()
        }
    }

    private fun translateText(text: String) {
        val request = TranslationRequest(text)
        ApiClient.instance.translateText(request).enqueue(object : Callback<TranslationResponse> {
            override fun onResponse(call: Call<TranslationResponse>, response: Response<TranslationResponse>) {
                if (response.isSuccessful) {
                    val translatedText = response.body()?.translatedText ?: "Gagal menerjemahkan"
                    binding.translationText.setText(translatedText)
                } else {
                    Toast.makeText(this@MainActivity, "Terjemahan gagal", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<TranslationResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Kesalahan jaringan: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Terjadi kesalahan: ${t.message}")
            }
        })
    }
}
