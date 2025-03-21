package com.example.ocraksarajawa.network

import com.example.ocraksarajawa.model.TranslationRequest
import com.example.ocraksarajawa.model.TranslationResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part




interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("translate")
    fun translateText(@Body request: TranslationRequest): Call<TranslationResponse>



    @Multipart
    @POST("ocr-hanacaraka") // Sesuai dengan endpoint OpenAPI
    fun uploadImage(
        @Part image: MultipartBody.Part
    ): Call<ResponseBody> // Response bisa disesuaikan
}
