package com.openlifting.data.remote

import com.openlifting.data.local.preferences.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val builder = original.newBuilder()
            .header("Accept", "application/json")

        if (requiresAuth(original.url.encodedPath)) {
            tokenStore.read()?.let { token ->
                builder.header("Authorization", "Bearer $token")
            }
        }

        return chain.proceed(builder.build())
    }

    private fun requiresAuth(path: String): Boolean = when {
        path == "/up" -> false
        path.endsWith("/api/register") -> false
        path.endsWith("/api/login") -> false
        else -> true
    }
}
