package com.example.loom.di

import android.content.Context
import androidx.room.Room
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
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "loom_database.db"
        )
            .createFromAsset("loom_database.db") // This copies your pre-packaged Kaggle dataset
            .fallbackToDestructiveMigration()
            .build()
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