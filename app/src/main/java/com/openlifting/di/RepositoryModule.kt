package com.openlifting.di

import com.openlifting.data.repository.AuthRepositoryImpl
import com.openlifting.data.repository.LocalCoachRepository
import com.openlifting.data.repository.SessionRepositoryImpl
import com.openlifting.data.websocket.EmgDataSourceWithFallback
import com.openlifting.domain.datasource.EmgDataSource
import com.openlifting.domain.repository.AuthRepository
import com.openlifting.domain.repository.CoachRepository
import com.openlifting.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    /**
     * NOTE: bound to a local Room-backed stand-in for the demo. When the Laravel backend
     * lands, swap this binding to a Retrofit-backed implementation — no ViewModel changes.
     */
    @Binds @Singleton
    abstract fun bindCoachRepository(impl: LocalCoachRepository): CoachRepository

    /**
     * EMG event source. Uses WebSocketEmgClient with fallback to Esp32Simulator.
     * The fallback is triggered when WS connection fails; UI can check
     * EmgDataSourceWithFallback.fallbackUsed() to display a banner.
     * See `docs/plan-realtime-measurement.md` §3.
     */
    @Binds @Singleton
    abstract fun bindEmgDataSource(impl: EmgDataSourceWithFallback): EmgDataSource
}
