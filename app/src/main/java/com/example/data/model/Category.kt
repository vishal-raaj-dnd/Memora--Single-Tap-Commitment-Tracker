package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val displayOrder: Int = 0,
    val isArchived: Boolean = false
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            Category("hackathon", "Hackathon", "rocket", "#FFD2CE", 0),
            Category("discussion", "Discussion", "chat", "#FFE885", 1),
            Category("incubation", "Incubation", "science", "#C3F0D6", 2),
            Category("amet", "AMET Work", "school", "#E2D9FF", 3),
            Category("eduvia", "Eduvia", "book", "#CDE4FE", 4),
            Category("academics", "Academics", "graduation", "#FEDCC5", 5),
            Category("iitm", "IITM Deadline", "calendar", "#FFCAD0", 6),
            Category("grit", "GRIT Prep", "target", "#D4E1F8", 7),
            Category("mint", "MINT Exam Prep", "document", "#C9EEFF", 8),
            Category("personal", "Personal", "person", "#E9DAFE", 9)
        )
    }
}
