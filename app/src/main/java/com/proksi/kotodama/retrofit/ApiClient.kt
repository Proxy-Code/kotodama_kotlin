package com.proksi.kotodama.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://api.kotodama.app"
    private const val BASE_URL_IMAGES = "https://ftrmnba3p0.execute-api.eu-central-1.amazonaws.com/"


    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
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


    val imagesService = retrofitImages.create(ApiInterface.ImagesService::class.java)
    val cloneService = retrofitClone.create(ApiInterface.CloneService::class.java)




}