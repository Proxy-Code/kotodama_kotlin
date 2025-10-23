package com.proksi.kotodama.retrofit

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://api.kotodama.app"
    private const val BASE_URL_IMAGES = "https://de6hm0frk8.execute-api.eu-central-1.amazonaws.com/"


    private val retrofitCreate: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)   // cevabı beklerken
        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)  // upload ederken
        .callTimeout(0, java.util.concurrent.TimeUnit.SECONDS)     // tüm çağrı için sınırsız
        .retryOnConnectionFailure(true)
        .build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }


    private val retrofitImages: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL_IMAGES)
        .addConverterFactory(GsonConverterFactory.create())
        .build()


    private val retrofitClone = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()


    val apiService: ApiInterface by lazy {
        retrofit.create(ApiInterface::class.java)
    }

    val createService: ApiInterface by lazy {
        retrofitCreate.create(ApiInterface::class.java)
    }




    val imagesService = retrofitImages.create(ApiInterface.ImagesService::class.java)
    val cloneService = retrofitClone.create(ApiInterface.CloneService::class.java)




}