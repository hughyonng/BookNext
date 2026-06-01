package com.booknext.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val bookId: String,
    val title: String,
    val author: String,
    val format: String,           // epub / pdf / txt
    val coverPath: String? = null,
    val filePath: String? = null, // null = 未下载到本地
    val fileSize: Long = 0L,
    val uploadTime: Long = 0L,
    val category: String = "",
    val isDownloaded: Boolean = false,
    val lastSyncTime: Long = 0L,
    val progress: String = "",    // Locator JSON 或 "page=N"
    val readingPercent: Float = 0f,
    val lastReadTime: Long = 0L,
    val isFinished: Boolean = false,
    val isFavorite: Boolean = false,
    val pendingSync: Boolean = false,
    val hasCover: Boolean = false,
    val status: String = "ready",
    val pageCount: Int? = null,
    val lastReadAt: Long = 0L,
    val totalReadingSeconds: Long = 0L,
    val readingSessionStart: Long = 0L,
)
