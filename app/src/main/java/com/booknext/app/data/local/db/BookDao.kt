package com.booknext.app.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadAt DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE bookId = :id")
    suspend fun getById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE title LIKE '%' || :q || '%' OR author LIKE '%' || :q || '%'")
    fun search(q: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE category = :cat ORDER BY uploadTime DESC")
    fun observeByCategory(cat: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE category = :category")
    suspend fun getByCategory(category: String): List<BookEntity>

    @Query("UPDATE books SET category = :category WHERE bookId = :bookId")
    suspend fun updateCategory(bookId: String, category: String)

    @Query("SELECT * FROM books WHERE pendingSync = 1")
    suspend fun getPendingSync(): List<BookEntity>

    @Upsert
    suspend fun upsert(book: BookEntity)

    @Upsert
    suspend fun upsertAll(books: List<BookEntity>)

    @Query("DELETE FROM books WHERE bookId = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE books SET progress = :progress, readingPercent = :percent, lastReadAt = :time, pendingSync = 1 WHERE bookId = :id")
    suspend fun updateProgress(id: String, progress: String, percent: Float, time: Long)

    @Query("UPDATE books SET progress = :progress, pendingSync = 1, lastReadAt = :time WHERE bookId = :bookId")
    suspend fun updateProgressNumeric(bookId: String, progress: String, time: Long)

    @Query("UPDATE books SET pendingSync = 0 WHERE bookId = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE books SET filePath = :path, isDownloaded = 1 WHERE bookId = :id")
    suspend fun markDownloaded(id: String, path: String)

    @Query("SELECT * FROM books WHERE lastReadAt > 0 ORDER BY lastReadAt DESC LIMIT 10")
    fun observeRecentlyRead(): Flow<List<BookEntity>>

    @Query("UPDATE books SET lastReadAt = :ts WHERE bookId = :id")
    suspend fun updateLastReadAt(id: String, ts: Long)

    @Query("UPDATE books SET totalReadingSeconds = totalReadingSeconds + :seconds WHERE bookId = :id")
    suspend fun addReadingTime(id: String, seconds: Long)

    @Query("SELECT SUM(totalReadingSeconds) FROM books")
    suspend fun getTotalReadingSeconds(): Long?

    @Query("SELECT * FROM books WHERE isFinished = 1 ORDER BY lastReadAt DESC")
    fun observeFinished(): Flow<List<BookEntity>>

    @Query("UPDATE books SET isFavorite = NOT isFavorite WHERE bookId = :id")
    suspend fun toggleFavorite(id: String)

    @Query("UPDATE books SET coverPath = :path, hasCover = 1 WHERE bookId = :id")
    suspend fun updateCoverPath(id: String, path: String)
}
