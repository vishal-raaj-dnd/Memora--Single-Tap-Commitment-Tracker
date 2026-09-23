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
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.categoryDao(), database.memoryDao())
                    }
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.categoryDao(), database.memoryDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(categoryDao: CategoryDao, memoryDao: MemoryDao) {
            // Strictly zero mock memories. Only default categories with 0 items are seeded.
            categoryDao.insertCategories(Category.DEFAULT_CATEGORIES)
        }
    }
}

