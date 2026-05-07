package com.clearread.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE filePath = :filePath ORDER BY pageNumber ASC")
    fun getBookmarksByFile(filePath: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    fun deleteById(id: Int)

    @Query("DELETE FROM bookmarks WHERE filePath = :filePath AND pageNumber = :pageNumber")
    fun deleteByFileAndPage(filePath: String, pageNumber: Int)

    @Query("SELECT COUNT(*) FROM bookmarks WHERE filePath = :filePath AND pageNumber = :pageNumber")
    fun isPageBookmarked(filePath: String, pageNumber: Int): Int
}
