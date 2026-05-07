package com.clearread.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a recently opened PDF file.
 * Matches spec data model exactly.
 */
@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val filePath: String,
    val fileName: String,
    val lastOpenedAt: Long,
    val lastPageRead: Int = 0,
    val totalPages: Int = 0
)
