package com.clearread.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {

    @Query("SELECT * FROM recent_files ORDER BY lastOpenedAt DESC")
    fun getAllRecentFiles(): Flow<List<RecentFileEntity>>

    @Query("SELECT * FROM recent_files WHERE filePath = :filePath LIMIT 1")
    fun getByFilePath(filePath: String): RecentFileEntity?

    @Query("SELECT * FROM recent_files WHERE id = :id LIMIT 1")
    fun getById(id: Int): RecentFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(recentFile: RecentFileEntity)

    @Query("DELETE FROM recent_files WHERE id = :id")
    fun deleteById(id: Int)

    @Query("SELECT * FROM recent_files WHERE fileName = :fileName ORDER BY lastOpenedAt DESC LIMIT 1")
    fun getByFileName(fileName: String): RecentFileEntity?

    @Query("DELETE FROM recent_files")
    fun clearAll()
}
