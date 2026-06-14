package com.aigenerator.app.di

import android.content.Context
import androidx.room.Room
import com.aigenerator.app.database.AppDatabase
import com.aigenerator.app.database.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "ai_generator_db")
            .fallbackToDestructiveMigration().build()

    @Provides @Singleton
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()
}
