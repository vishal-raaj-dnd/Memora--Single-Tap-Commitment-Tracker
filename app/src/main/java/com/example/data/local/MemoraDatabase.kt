package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Category
import com.example.data.model.MemoryItem
import com.example.data.model.MemoryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.data.model.CategoryCorrection

@Database(entities = [MemoryItem::class, Category::class, CategoryCorrection::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MemoraDatabase : RoomDatabase() {

    abstract fun memoryDao(): MemoryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryCorrectionDao(): CategoryCorrectionDao

    companion object {
        @Volatile
        private var INSTANCE: MemoraDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): MemoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoraDatabase::class.java,
                    "memora_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                scope.launch(Dispatchers.IO) {
                    try {
                        val existing = instance.categoryDao().getAllCategoriesSync()
                        if (existing.isEmpty()) {
                            instance.categoryDao().insertCategories(Category.DEFAULT_CATEGORIES)
                        }
                    } catch (_: Exception) {}
                }
                instance
            }
        }

        suspend fun populateInitialData(categoryDao: CategoryDao, memoryDao: MemoryDao) {
            // Strictly zero mock memories. Only default categories with 0 items are seeded.
            categoryDao.insertCategories(Category.DEFAULT_CATEGORIES)
        }
    }
}

