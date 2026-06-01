package com.booknext.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val locatorJson: String,
    val type: String,             // highlight / underline / note / quote
    val color: Int = 0xFFFFD700.toInt(),
    val note: String = "",
    val selectedText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long = 0L,
)
