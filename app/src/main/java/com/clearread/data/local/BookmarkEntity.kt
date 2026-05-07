package com.clearread.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a bookmarked page or file.
 * Matches spec data model exactly.
 */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val filePath: String,
    val fileName: String,
    val pageNumber: Int,
    val createdAt: Long,
    val label: String? = null
)
