package com.example.data.repository

import com.example.data.local.MemoryDao
import com.example.data.model.MemoryItem
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {

    val allMemories: Flow<List<MemoryItem>> = memoryDao.getAllMemories()

    fun getMemoryById(id: Long): Flow<MemoryItem?> = memoryDao.getMemoryById(id)

    fun getMemoriesByCategory(categoryId: String): Flow<List<MemoryItem>> =
        memoryDao.getMemoriesByCategory(categoryId)

    fun searchMemories(query: String): Flow<List<MemoryItem>> =
        memoryDao.searchMemories(query)

    suspend fun insertMemory(item: MemoryItem): Long =
        memoryDao.insertMemory(item)

    suspend fun updateMemory(item: MemoryItem) =
        memoryDao.updateMemory(item)

    suspend fun deleteMemory(item: MemoryItem) =
        memoryDao.deleteMemory(item)

    suspend fun deleteById(id: Long) =
        memoryDao.deleteMemoryById(id)

    suspend fun deleteAll() =
        memoryDao.deleteAll()
}
