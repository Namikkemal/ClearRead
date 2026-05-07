package com.clearread.data.repository

import com.clearread.data.local.RecentFileDao
import com.clearread.data.local.RecentFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RecentFilesRepository(private val recentFileDao: RecentFileDao) {

    val recentFiles: Flow<List<RecentFileEntity>> = recentFileDao.getAllRecentFiles()

    suspend fun addOrUpdateRecentFile(recentFile: RecentFileEntity) = withContext(Dispatchers.IO) {
        val existing = recentFileDao.getByFilePath(recentFile.filePath)
        if (existing != null) {
            // Update existing entry with new timestamp, preserve page progress
            recentFileDao.upsert(
                recentFile.copy(
                    id = existing.id,
                    lastPageRead = if (recentFile.lastPageRead > 0) recentFile.lastPageRead else existing.lastPageRead,
                    totalPages = if (recentFile.totalPages > 0) recentFile.totalPages else existing.totalPages
                )
            )
        } else {
            recentFileDao.upsert(recentFile)
        }
    }

    suspend fun updateReadProgress(filePath: String, lastPageRead: Int, totalPages: Int) = withContext(Dispatchers.IO) {
        val existing = recentFileDao.getByFilePath(filePath)
        if (existing != null) {
            recentFileDao.upsert(
                existing.copy(
                    lastPageRead = lastPageRead,
                    totalPages = totalPages,
                    lastOpenedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteRecentFile(context: android.content.Context, id: Int) = withContext(Dispatchers.IO) {
        val existing = recentFileDao.getById(id)
        if (existing != null) {
            com.clearread.utils.FileUtils.deleteIfInternal(context, existing.filePath)
            recentFileDao.deleteById(id)
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        recentFileDao.clearAll()
    }
}
