package com.example.loom.di

import android.content.Context
import com.example.loom.data.local.AppDatabase
import com.example.loom.data.local.dao.TrackerDao
import com.example.loom.data.repository.TrackerRepository
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideTrackerDao(database: AppDatabase): TrackerDao {
        return database.trackerDao
    }

    @Provides
    @Singleton
    fun provideTrackerRepository(dao: TrackerDao): TrackerRepository {
        return TrackerRepository(dao)
    }
}