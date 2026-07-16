package com.example.banca_en_linea.data.remote

import com.example.banca_en_linea.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Fábrica del cliente Retrofit.
 *
 * BASE_URL usa 10.0.2.2: es el alias que el emulador de Android le da al
 * localhost de tu PC (donde correrá uvicorn). En un teléfono físico habría
 * que usar la IP local de la PC (ej. 192.168.x.x).
 */
object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8000/"

    fun crear(tokenManager: TokenManager): ApiService {
        // Adjunta el JWT a cada request automáticamente, excepto al login.
        val authInterceptor = Interceptor { chain ->
            val request = chain.request()
            val token = tokenManager.accessToken
            val conAuth = if (token != null && !request.url.encodedPath.contains("/auth/")) {
                request.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                request
            }
            chain.proceed(conAuth)
        }

        // Loguea el tráfico HTTP en Logcat (nivel BODY solo para desarrollo;
        // en producción se baja a NONE para no filtrar datos sensibles).
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttp = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
