package com.example.literakiapp.data

import android.content.Context
import com.example.literakiapp.BuildConfig
import com.example.literakiapp.data.api.LiterakiApiService
import com.example.literakiapp.data.model.ApiMoveTileJsonAdapter
import com.example.literakiapp.data.repository.LiterakiRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AppContainer(context: Context) {
    val sessionStore = SessionStore(context)

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        sessionStore.getSession()?.token?.takeIf { it.isNotBlank() }?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val moshi = Moshi.Builder()
        .add(ApiMoveTileJsonAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(LiterakiApiService::class.java)

    val repository = LiterakiRepository(
        apiService = apiService,
        sessionStore = sessionStore
    )
}

