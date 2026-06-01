package com.booknext.app.di

import android.content.Context
import androidx.room.Room
import com.booknext.app.data.local.db.AppDatabase
import com.booknext.app.data.local.db.BookDao
import com.booknext.app.data.local.db.AnnotationDao
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
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "booknext.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBookDao(db: AppDatabase): BookDao = db.bookDao()
    @Provides fun provideAnnotationDao(db: AppDatabase): AnnotationDao = db.annotationDao()
}
