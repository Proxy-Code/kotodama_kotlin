package com.proksi.kotodama.retrofit

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiInterface {

        @POST("/process")
        fun processRequest(@Body payload: ProcessRequest): Call<ProcessResponse>

    @Multipart
    @POST("transcribe_preview")
    suspend fun transcribePreview(
        @Part("idToken") idToken: RequestBody,
        @Part files: MultipartBody.Part
    ): TranscribePreviewResponse


    data class TranscribePreviewResponse(
        val text: String,
        @SerializedName("audio_hash") val audioHash: String
    )
    data class ProcessRequest(
        val text: String,
        val name: String,
        val language_code: String,
        val isDefault: Boolean,
        val sound_sample_id: String,
        val imageUrl: String,
        val idToken: String
    )

    data class ProcessResponse(
        val success: Boolean,
        val data: ProcessData?,
        val error: ErrorData?
    )

    data class ProcessData(
        val id: String,
        val name: String,
        val status: String,
        val message: String,
        val result: Any? // Sonuç verisi isteğe bağlı
    )

    data class ErrorData(
        val code: Int,
        val message: String
    )

    interface ImagesService {
        @GET("kotodama-avatars")
        fun getImages(): Call<List<String>>
    }

    interface CloneService {
        @Multipart
        @POST("/clone")
        fun cloneRequest(
            @Part("name") name: RequestBody,
            @Part("idToken") idToken: RequestBody,
            @Part("imageUrl") imageUrl: RequestBody,
            @Part files: List<MultipartBody.Part>
        ): Call<CloneResponse>
    }

    data class CloneResponse(
        val status: String,
        val data: Any 
    )


}

