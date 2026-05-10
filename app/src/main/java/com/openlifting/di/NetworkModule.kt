package com.openlifting.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.openlifting.BuildConfig
import com.openlifting.data.remote.AuthInterceptor
import com.openlifting.data.remote.api.VortexApi
import com.openlifting.data.remote.api.VortexAthleteApi
import com.openlifting.data.remote.api.VortexSessionApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "http://10.0.2.2:8000/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideVortexApi(retrofit: Retrofit): VortexApi =
        retrofit.create(VortexApi::class.java)

    @Provides
    @Singleton
    fun provideVortexAthleteApi(retrofit: Retrofit): VortexAthleteApi =
        retrofit.create(VortexAthleteApi::class.java)

    @Provides
    @Singleton
    fun provideVortexSessionApi(retrofit: Retrofit): VortexSessionApi =
        retrofit.create(VortexSessionApi::class.java)
}
