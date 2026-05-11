package com.openlifting.di

import com.openlifting.data.repository.AthleteProfileRepositoryImpl
import com.openlifting.data.repository.AuthRepositoryImpl
import com.openlifting.data.repository.SessionRepositoryImpl
import com.openlifting.data.repository.VortexCoachRepository
import com.openlifting.data.websocket.EmgDataSourceWithFallback
import com.openlifting.domain.datasource.EmgDataSource
import com.openlifting.domain.repository.AthleteProfileRepository
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
    abstract fun bindAthleteProfileRepository(
        impl: AthleteProfileRepositoryImpl
    ): AthleteProfileRepository

    @Binds @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds @Singleton
    abstract fun bindCoachRepository(impl: VortexCoachRepository): CoachRepository

    /**
     * EMG event source. Uses WebSocketEmgClient with fallback to Esp32Simulator.
     * The fallback is triggered when WS connection fails; UI can check
     * EmgDataSourceWithFallback.fallbackUsed() to display a banner.
     * See `docs/plan-realtime-measurement.md` §3.
     */
    @Binds @Singleton
    abstract fun bindEmgDataSource(impl: EmgDataSourceWithFallback): EmgDataSource
}
