package com.tankermanager.app.data.api

import com.tankermanager.app.BuildConfig
import com.tankermanager.app.data.repo.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    @Volatile
    private var api: TankerApi? = null

    fun get(sessionStore: SessionStore): TankerApi {
        return api ?: synchronized(this) {
            api ?: build(sessionStore).also { api = it }
        }
    }

    fun reset() {
        api = null
    }

    private fun build(sessionStore: SessionStore): TankerApi {
        val auth = Interceptor { chain ->
            val token = runBlocking { sessionStore.token.first() }
            val req = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else chain.request()
            chain.proceed(req)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TankerApi::class.java)
    }
}
