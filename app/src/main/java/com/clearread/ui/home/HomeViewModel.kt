package com.clearread.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearread.data.local.AppDatabase
import com.clearread.data.local.BookmarkEntity
import com.clearread.data.local.RecentFileEntity
import com.clearread.data.repository.BookmarkRepository
import com.clearread.data.repository.RecentFilesRepository
import com.clearread.utils.FileUtils
import com.clearread.utils.PdfUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val recentFilesRepository = RecentFilesRepository(database.recentFileDao())
    private val bookmarkRepository = BookmarkRepository(database.bookmarkDao())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val recentFiles: StateFlow<List<RecentFileEntity>> = combine(
        recentFilesRepository.recentFiles,
        _searchQuery
    ) { files, query ->
        if (query.isEmpty()) files
        else files.filter { it.fileName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = combine(
        bookmarkRepository.allBookmarks,
        _searchQuery
    ) { items, query ->
        if (query.isEmpty()) items
        else items.filter { it.fileName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Called when user picks a PDF file via SAF.
     * Saves it to recent files and returns the URI string for navigation.
     */
    fun onFilePicked(uri: Uri, onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true

            val context = getApplication<Application>()

            // Take persistent permission so we can reopen the file later
            FileUtils.takePersistablePermission(context, uri)

            val fileName = FileUtils.getFileNameFromUri(context, uri)
            val pageCount = PdfUtils.getPageCount(context, uri)

            val recentFile = RecentFileEntity(
                filePath = uri.toString(),
                fileName = fileName,
                lastOpenedAt = System.currentTimeMillis(),
                lastPageRead = 0,
                totalPages = pageCount
            )

            recentFilesRepository.addOrUpdateRecentFile(recentFile)
            _isLoading.value = false
            onReady(uri.toString())
        }
    }

    /**
     * Re-open a recent file — updates timestamp.
     */
    fun onRecentFileClicked(recentFile: RecentFileEntity, onReady: (String) -> Unit) {
        onReady(recentFile.filePath)
    }

    /**
     * Delete a single recent file entry.
     */
    fun deleteRecentFile(id: Int) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            recentFilesRepository.deleteRecentFile(context, id)
        }
    }

    /**
     * Remove a bookmark.
     */
    fun removeBookmark(id: Int) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(id)
        }
    }

    /**
     * Share a file from the home screen.
     */
    fun shareFile(context: android.content.Context, filePath: String) {
        try {
            val uri = Uri.parse(filePath)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = if (filePath.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "text/plain"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share File"))
        } catch (e: Exception) {
            // Log or handle error
        }
    }

    /**
     * Print a file from the home screen.
     */
    fun printFile(context: android.content.Context, filePath: String, fileName: String) {
        try {
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
            val adapter = com.clearread.ui.reader.PdfPrintAdapter(context, Uri.parse(filePath), fileName)
            printManager.print("ClearRead - $fileName", adapter, null)
        } catch (e: Exception) {
            // Log or handle error
        }
    }

    private val _fileProperties = MutableStateFlow<FilePropertiesData?>(null)
    val fileProperties: StateFlow<FilePropertiesData?> = _fileProperties.asStateFlow()

    fun showFileProperties(context: android.content.Context, filePath: String, fileName: String, lastOpened: Long, pageCount: Int) {
        viewModelScope.launch {
            val sizeBytes = FileUtils.getFileSizeFromUri(context, Uri.parse(filePath))
            val fileSizeStr = FileUtils.formatFileSize(sizeBytes)
            
            _fileProperties.value = FilePropertiesData(
                fileName = fileName,
                filePath = filePath,
                fileSize = fileSizeStr,
                lastOpened = lastOpened,
                pageCount = pageCount
            )
        }
    }

    fun dismissFileProperties() {
        _fileProperties.value = null
    }
}

data class FilePropertiesData(
    val fileName: String,
    val filePath: String,
    val fileSize: String,
    val lastOpened: Long,
    val pageCount: Int
)
