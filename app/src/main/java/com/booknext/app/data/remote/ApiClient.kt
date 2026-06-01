package com.booknext.app.data.remote

import com.booknext.app.data.local.prefs.UserPreferences
import com.booknext.app.data.remote.api.BookNextApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    private val prefs: UserPreferences,
    private val baseRetrofit: Retrofit,
    private val baseOkHttpClient: OkHttpClient,
) {
    fun api(): BookNextApi {
        val (url, key) = runBlocking {
            val u = prefs.serverUrl.first().trimEnd('/') + "/"
            val k = prefs.apiKey.first()
            Pair(u, k)
        }
        return baseRetrofit.newBuilder()
            .baseUrl(url)
            .client(
                baseOkHttpClient.newBuilder()
                    .addInterceptor(AuthInterceptor(key))
                    .build()
            )
            .build()
            .create(BookNextApi::class.java)
    }
}

class AuthInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
        return chain.proceed(req)
    }
}