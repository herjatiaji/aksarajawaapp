package com.example.ocraksarajawa.model

data class TranslationResponse(
    val input: String,  // Teks yang dikirim
    val raw_response: String,  // Respon mentah dari API
    val cleaned_translation: String  // Terjemahan hasil akhir
)
