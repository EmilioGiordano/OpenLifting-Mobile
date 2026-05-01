package com.openlifting.di

import android.content.Context
import androidx.room.Room
import com.openlifting.data.local.OpenLiftingDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OpenLiftingDatabase =
        Room.databaseBuilder(context, OpenLiftingDatabase::class.java, "openlifting.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: OpenLiftingDatabase) = db.userDao()
    @Provides fun provideAthleteProfileDao(db: OpenLiftingDatabase) = db.athleteProfileDao()
    @Provides fun provideSessionDao(db: OpenLiftingDatabase) = db.sessionDao()
    @Provides fun provideSetDao(db: OpenLiftingDatabase) = db.setDao()
}
