package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MemoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<MemoryItem>>

    @Query("SELECT * FROM memories WHERE id = :id")
    fun getMemoryById(id: Long): Flow<MemoryItem?>

    @Query("SELECT * FROM memories WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getMemoriesByCategory(categoryId: String): Flow<List<MemoryItem>>

    @Query("SELECT * FROM memories WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR organization LIKE '%' || :query || '%' OR aiSummary LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchMemories(query: String): Flow<List<MemoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(item: MemoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(items: List<MemoryItem>)

    @Update
    suspend fun updateMemory(item: MemoryItem)

    @Delete
    suspend fun deleteMemory(item: MemoryItem)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM memories")
    suspend fun deleteAll()
}
