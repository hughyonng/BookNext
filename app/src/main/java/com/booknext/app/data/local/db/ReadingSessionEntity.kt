package com.booknext.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val startTime: Long,
    val durationSeconds: Long,
    val progressPercent: Float,
    val wordsPerMinute: Int = 0,
    val charsRead: Long = 0L,
)
