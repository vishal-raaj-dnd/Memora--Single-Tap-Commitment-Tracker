package com.example.data.repository

import com.example.data.local.CategoryDao
import com.example.data.model.Category
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: Category) =
        categoryDao.insertCategory(category)

    suspend fun insertCategories(categories: List<Category>) =
        categoryDao.insertCategories(categories)

    suspend fun updateCategory(category: Category) =
        categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: Category) =
        categoryDao.deleteCategory(category)
}
