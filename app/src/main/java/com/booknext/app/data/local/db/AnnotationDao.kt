package com.booknext.app.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY createdAt ASC")
    fun observeByBook(bookId: String): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AnnotationEntity>>

    @Upsert
    suspend fun upsert(annotation: AnnotationEntity)

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun deleteById(id: String)
}
