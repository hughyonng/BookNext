package com.booknext.app.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startTime DESC LIMIT 10")
    suspend fun getRecentSessions(bookId: String): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startTime DESC")
    fun observeSessions(bookId: String): Flow<List<ReadingSessionEntity>>

    @Query("DELETE FROM reading_sessions WHERE bookId = :bookId")
    suspend fun deleteByBook(bookId: String)

    @Query("SELECT AVG(wordsPerMinute) FROM reading_sessions WHERE bookId = :bookId AND wordsPerMinute > 0")
    suspend fun getAvgWpm(bookId: String): Float?
}
