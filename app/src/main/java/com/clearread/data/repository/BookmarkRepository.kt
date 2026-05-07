package com.clearread.data.repository

import com.clearread.data.local.BookmarkDao
import com.clearread.data.local.BookmarkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {

    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    fun getBookmarksByFile(filePath: String): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getBookmarksByFile(filePath)
    }

    suspend fun addBookmark(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        bookmarkDao.insert(bookmark)
    }

    suspend fun removeBookmark(id: Int) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteById(id)
    }

    suspend fun togglePageBookmark(filePath: String, fileName: String, pageNumber: Int): Boolean = withContext(Dispatchers.IO) {
        val isBookmarked = bookmarkDao.isPageBookmarked(filePath, pageNumber) > 0
        if (isBookmarked) {
            bookmarkDao.deleteByFileAndPage(filePath, pageNumber)
            return@withContext false
        } else {
            bookmarkDao.insert(
                BookmarkEntity(
                    filePath = filePath,
                    fileName = fileName,
                    pageNumber = pageNumber,
                    createdAt = System.currentTimeMillis()
                )
            )
            return@withContext true
        }
    }
}
