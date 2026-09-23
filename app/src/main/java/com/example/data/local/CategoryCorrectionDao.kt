package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CategoryCorrection
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryCorrectionDao {
    @Query("SELECT * FROM category_corrections ORDER BY timestamp DESC LIMIT 20")
    fun getRecentCorrections(): Flow<List<CategoryCorrection>>

    @Query("SELECT * FROM category_corrections ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentCorrectionsSync(): List<CategoryCorrection>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrection(correction: CategoryCorrection)
}
