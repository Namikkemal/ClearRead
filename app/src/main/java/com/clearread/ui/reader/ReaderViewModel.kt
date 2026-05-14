package com.clearread.ui.reader

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.print.PrintManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearread.data.local.AppDatabase
import com.clearread.data.local.BookmarkEntity
import com.clearread.data.repository.BookmarkRepository
import com.clearread.data.local.PreferencesManager
import com.clearread.data.repository.RecentFilesRepository
import com.clearread.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import com.clearread.ui.reader.ReadingMode
import com.clearread.ui.reader.ImportDialogData
import com.clearread.ui.reader.ReaderUiState
import com.clearread.ui.reader.PdfBitmapCache
import com.clearread.ui.reader.PdfBookmark

/** The three visual modes the reader supports. */
enum class ReadingMode { NORMAL, DARK, SEPIA }

data class ImportDialogData(
    val uri: Uri,
    val fileName: String,
    val fileSize: Long
)

data class ReaderUiState(
    val fileName: String = "",
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val readingMode: ReadingMode = ReadingMode.NORMAL,
    val scrollDirection: Int = PreferencesManager.SCROLL_VERTICAL,
    val isFullscreen: Boolean = false,
    val isBookmarked: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val importDialogData: ImportDialogData? = null,
    val pageAspectRatios: List<Float> = emptyList(),
    val textContent: String? = null,
    val toc: List<PdfBookmark> = emptyList(),
    val isSearchSupported: Boolean = true,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val matchingPages: List<Int> = emptyList(),
    val currentSearchIndex: Int = -1,
    val totalMatchesCount: Int = 0,
    val occurrencePageIndices: List<Int> = emptyList(),
    val searchResults: List<PdfSearchMatch> = emptyList(),
    val targetScrollOffset: Int = 0,
    val searchScrollTrigger: Long = 0L,
    val jumpScrollTrigger: Long = 0L
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val recentFilesRepository = RecentFilesRepository(database.recentFileDao())
    private val bookmarkRepository = BookmarkRepository(database.bookmarkDao())
    private val preferencesManager = PreferencesManager.getInstance(application)

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    // Cache for search results to avoid redundant PDF parsing
    private val searchCache = mutableMapOf<Pair<Int, String>, List<PdfSearchMatch>>()

    // Text cache lives outside UI state to avoid GC thrashing during search.
    // ConcurrentHashMap is thread-safe for reads from IO dispatchers.
    private val pageTextCache = ConcurrentHashMap<Int, String>()

    private val _pageBitmaps = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val pageBitmaps: StateFlow<Map<Int, Bitmap>> = _pageBitmaps.asStateFlow()

    private val pdfInteractionHelper = PDFInteractionHelper(application)
    private var pdfCache: PdfBitmapCache? = null
    private var fileUri: String = ""
    private var searchJob: Job? = null
    private var dbUpdateJob: Job? = null

    init {
        viewModelScope.launch {
            preferencesManager.scrollDirection.collect { dir ->
                _uiState.value = _uiState.value.copy(scrollDirection = dir)
            }
        }
    }

    fun loadPdf(uriString: String, screenWidth: Int, initialPage: Int = -1) {
        if (fileUri == uriString && _uiState.value.pageCount > 0) return
        fileUri = uriString

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            withContext(Dispatchers.IO) {
                try {
                    val context = getApplication<Application>()
                    var uri = Uri.parse(uriString)
                    var finalUriString = uriString
                    val fileName = FileUtils.getFileNameFromUri(context, uri)
                    
                    var saveToRecent = true
                    
                    // Try to get persistable permission. If it fails, the file explorer only granted
                    // temporary access. We must copy it to internal storage to save it to recent files.
                    if (uri.scheme == "content") {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: SecurityException) {
                            // If it's a tree-child URI (from our built-in explorer), we already have 
                            // persistent access via the tree permission. No need to copy.
                            val uriString = uri.toString()
                            val isTreeChild = uriString.contains("/tree/") && uriString.contains("/document/")
                            val treeId = if (isTreeChild) uriString.split("/tree/").getOrNull(1)?.split("/")?.getOrNull(0) else null
                            
                            val hasPersistentAccess = treeId != null && context.contentResolver.persistedUriPermissions.any { p ->
                                p.uri.authority == uri.authority && p.uri.toString().endsWith("/tree/$treeId")
                            }

                            if (!hasPersistentAccess) {
                                val fileSize = FileUtils.getFileSizeFromUri(context, uri)
                                
                                // Check if we already have a copy of this file to avoid duplicates or redundant prompts
                                val existingImport = FileUtils.findExistingImport(context, fileName)
                                
                                if (existingImport != null) {
                                    uri = existingImport
                                    finalUriString = existingImport.toString()
                                    fileUri = finalUriString
                                } else {
                                    // Fallback: Check recent files DB for any entry with the same name that is still accessible
                                    val existingRecent = database.recentFileDao().getByFileName(fileName)
                                    if (existingRecent != null && FileUtils.canAccessUri(context, Uri.parse(existingRecent.filePath))) {
                                        uri = Uri.parse(existingRecent.filePath)
                                        finalUriString = existingRecent.filePath
                                        fileUri = finalUriString
                                    } else {
                                        if (fileSize > 50 * 1024 * 1024) { // 50MB
                                            _uiState.value = _uiState.value.copy(
                                                isLoading = false,
                                                importDialogData = ImportDialogData(uri, fileName, fileSize)
                                            )
                                            return@withContext
                                        }
                                        
                                        val copiedUri = FileUtils.copyToInternalStorage(context, uri, fileName)
                                        if (copiedUri != null) {
                                            uri = copiedUri
                                            finalUriString = copiedUri.toString()
                                            fileUri = finalUriString
                                        } else {
                                            saveToRecent = false
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (fileName.endsWith(".md", ignoreCase = true) || fileName.endsWith(".txt", ignoreCase = true)) {
                        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            fileName = fileName,
                            textContent = text,
                            pageCount = 0
                        )
                        // Also add to recent files for convenience
                        if (saveToRecent) {
                            val newRecent = com.clearread.data.local.RecentFileEntity(
                                filePath = finalUriString,
                                fileName = fileName,
                                lastPageRead = 0,
                                totalPages = 0,
                                lastOpenedAt = System.currentTimeMillis()
                            )
                            recentFilesRepository.addOrUpdateRecentFile(newRecent)
                        }
                        return@withContext
                    }
 
                    finishLoadingPdf(uri, finalUriString, fileName, saveToRecent, screenWidth, initialPage)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun handleImportDialog(copy: Boolean, screenWidth: Int, initialPage: Int = -1) {
        val data = _uiState.value.importDialogData ?: return
        _uiState.value = _uiState.value.copy(importDialogData = null, isLoading = true)
        
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            var uri = data.uri
            var finalUriString = data.uri.toString()
            var saveToRecent = false

            if (copy) {
                val copiedUri = FileUtils.copyToInternalStorage(context, data.uri, data.fileName)
                if (copiedUri != null) {
                    uri = copiedUri
                    finalUriString = copiedUri.toString()
                    fileUri = finalUriString
                    saveToRecent = true
                }
            } else {
                saveToRecent = false
                fileUri = finalUriString
            }
            
            finishLoadingPdf(uri, finalUriString, data.fileName, saveToRecent, screenWidth, initialPage)
        }
    }

    private suspend fun finishLoadingPdf(
        uri: Uri, 
        finalUriString: String, 
        fileName: String, 
        saveToRecent: Boolean,
        screenWidth: Int,
        initialPage: Int = -1
    ) {
        val context = getApplication<Application>()
        val cache = PdfBitmapCache(context, uri)
        val openError = cache.open()
        if (openError != null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Cannot open this PDF file: $openError\nURI: $finalUriString"
            )
            return
        }

        pdfCache?.close()
        pdfCache = cache

        // Page logic: use requested initial page if valid, otherwise resume from DB
        val existing = database.recentFileDao().getByFilePath(finalUriString)
        val resumePage = existing?.lastPageRead ?: 0
        val targetPage = if (initialPage >= 0) initialPage else resumePage

        // Update timestamp now that it's opened in Reader
        if (saveToRecent) {
            if (existing != null) {
                recentFilesRepository.updateReadProgress(finalUriString, targetPage, cache.pageCount)
            } else {
                // It's a newly opened file (e.g. from intent), add it to recent files
                val newRecent = com.clearread.data.local.RecentFileEntity(
                    filePath = finalUriString,
                    fileName = fileName,
                    lastPageRead = targetPage,
                    totalPages = cache.pageCount,
                    lastOpenedAt = System.currentTimeMillis()
                )
                recentFilesRepository.addOrUpdateRecentFile(newRecent)
            }
        }

        // Check if current page is bookmarked
        val isBookmarked = database.bookmarkDao()
            .isPageBookmarked(finalUriString, targetPage) > 0

        _uiState.value = _uiState.value.copy(
            fileName = existing?.fileName ?: fileName,
            pageCount = cache.pageCount,
            currentPage = targetPage,
            isBookmarked = isBookmarked,
            isLoading = false,
            pageAspectRatios = cache.pageAspectRatios
        )

        // Pre-render pages
        renderPagesAround(targetPage, screenWidth)

        // Background TOC parsing
        loadTableOfContents(uri)
        
        // Probe document to see if it supports text search (e.g. not a scanned image)
        checkSearchSupport(uri)
    }

    private fun checkSearchSupport(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = _uiState.value.pageCount
            if (count == 0) {
                _uiState.value = _uiState.value.copy(isSearchSupported = false)
                return@launch
            }

            // Sample 5 pages distributed across the doc to find ANY text
            val pagesToProbe = if (count <= 5) {
                (0 until count).toList()
            } else {
                listOf(0, count / 4, count / 2, (count * 3) / 4, count - 1).distinct()
            }
            
            var hasText = false
            for (page in pagesToProbe) {
                val text = pdfInteractionHelper.extractPageText(uri, page)
                // If we find even a little bit of text, enable search
                if (text.trim().isNotEmpty()) {
                    hasText = true
                    break
                }
            }

            if (!hasText) {
                _uiState.value = _uiState.value.copy(isSearchSupported = false)
            } else {
                _uiState.value = _uiState.value.copy(isSearchSupported = true)
            }
        }
    }

    private fun loadTableOfContents(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val toc = pdfInteractionHelper.getTableOfContents(uri)
            _uiState.value = _uiState.value.copy(toc = toc)
        }
    }

    fun search(query: String, screenWidth: Int) {
        searchJob?.cancel()
        
        if (query.trim().length < 3) {
            _uiState.value = _uiState.value.copy(
                searchQuery = query, 
                matchingPages = emptyList(),
                searchResults = emptyList(),
                currentSearchIndex = -1,
                totalMatchesCount = 0,
                occurrencePageIndices = emptyList(),
                isSearching = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            searchQuery = query, 
            isSearching = true,
            matchingPages = emptyList(),
            totalMatchesCount = 0,
            occurrencePageIndices = emptyList()
        )
        
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            // Debounce to avoid excessive disk I/O while typing
            delay(400)
            if (!isActive) return@launch
            
            val uri = Uri.parse(fileUri)
            val matchingPages = mutableListOf<Int>()
            val occurrencePageIndices = mutableListOf<Int>()
            
            var lastUpdate = System.currentTimeMillis()
            
            pdfInteractionHelper.searchStreaming(
                uri = uri, 
                query = query,
                cache = pageTextCache,
                onPageParsed = { index, text ->
                    // Write directly to ConcurrentHashMap — no UI state copy needed
                    pageTextCache[index] = text
                }
            ) { pageIndex ->
                matchingPages.add(pageIndex)
                occurrencePageIndices.add(pageIndex)
                
                // Throttle UI updates to 200ms to keep the app responsive
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 200) {
                    _uiState.value = _uiState.value.copy(
                        matchingPages = matchingPages.toList(),
                        totalMatchesCount = occurrencePageIndices.size,
                        occurrencePageIndices = occurrencePageIndices.toList()
                    )
                    lastUpdate = now
                }
            }
            
            // Final update to catch any remaining results
            _uiState.value = _uiState.value.copy(
                matchingPages = matchingPages.toList(),
                totalMatchesCount = occurrencePageIndices.size,
                occurrencePageIndices = occurrencePageIndices.toList(),
                isSearching = false,
                currentSearchIndex = if (occurrencePageIndices.isNotEmpty()) 0 else -1
            )
            refreshHighlights()
            
            // Auto-scroll to first match if found and we aren't already looking at it
            if (occurrencePageIndices.isNotEmpty()) {
                val firstMatchPage = occurrencePageIndices[0]
                if (_uiState.value.currentPage != firstMatchPage) {
                    nextSearchResult(screenWidth)
                }
            }
        }
    }

    private fun normalizeForSearch(s: String): String {
        return s.lowercase(java.util.Locale.ROOT)
            .replace('ç', 'c')
            .replace('ğ', 'g')
            .replace('ı', 'i')
            .replace('i', 'i')
            .replace('ö', 'o')
            .replace('ş', 's')
            .replace('ü', 'u')
    }

    fun nextSearchResult(screenWidth: Int) {
        val state = _uiState.value
        if (state.occurrencePageIndices.isEmpty()) return
        
        // If we don't have an active match yet, find the first one on or after current page.
        // Once we have an active match, just follow the literal sequence (predictable).
        val nextIndex = if (state.currentSearchIndex == -1) {
            val found = state.occurrencePageIndices.indexOfFirst { it >= state.currentPage }
            if (found == -1) 0 else found
        } else {
            (state.currentSearchIndex + 1) % state.occurrencePageIndices.size
        }
        
        val targetPage = state.occurrencePageIndices[nextIndex]
        
        viewModelScope.launch(Dispatchers.IO) {
            val query = state.searchQuery
            val cacheKey = targetPage to query
            val matches = searchCache.getOrPut(cacheKey) {
                pdfInteractionHelper.searchInPage(Uri.parse(fileUri), targetPage, query)
            }
            
            var occurrenceOnPage = 0
            for (i in 0 until nextIndex) {
                if (state.occurrencePageIndices[i] == targetPage) occurrenceOnPage++
            }
            val targetMatch = matches.getOrNull(occurrenceOnPage)
            
            val aspectRatio = state.pageAspectRatios.getOrNull(targetPage) ?: 1.41f
            val pageHeight = (screenWidth * aspectRatio).toInt()
            val offset = if (targetMatch != null) {
                val rawOffset = (targetMatch.rect.top * pageHeight).toInt() - 250
                rawOffset.coerceIn(0, maxOf(0, pageHeight - 400))
            } else 0
            
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    currentSearchIndex = nextIndex, 
                    searchScrollTrigger = System.currentTimeMillis(),
                    searchResults = if (targetMatch != null) (state.searchResults + targetMatch).distinct() else state.searchResults
                )
                
                // If it's a different page, update but don't call jumpToPage (it fights with scrollTrigger)
                if (state.currentPage != targetPage) {
                    _uiState.value = _uiState.value.copy(currentPage = targetPage)
                    renderPagesAround(targetPage, screenWidth)
                } else {
                    refreshHighlights()
                }
            }
        }
    }

    fun prevSearchResult(screenWidth: Int) {
        val state = _uiState.value
        if (state.occurrencePageIndices.isEmpty()) return
        
        val prevIndex = if (state.currentSearchIndex == -1) {
            val found = state.occurrencePageIndices.indexOfLast { it <= state.currentPage }
            if (found == -1) state.occurrencePageIndices.size - 1 else found
        } else {
            if (state.currentSearchIndex <= 0) state.occurrencePageIndices.size - 1 else state.currentSearchIndex - 1
        }
        
        val targetPage = state.occurrencePageIndices[prevIndex]
        
        viewModelScope.launch(Dispatchers.IO) {
            val query = state.searchQuery
            val cacheKey = targetPage to query
            val matches = searchCache.getOrPut(cacheKey) {
                pdfInteractionHelper.searchInPage(Uri.parse(fileUri), targetPage, query)
            }
            
            var occurrenceOnPage = 0
            for (i in 0 until prevIndex) {
                if (state.occurrencePageIndices[i] == targetPage) occurrenceOnPage++
            }
            val targetMatch = matches.getOrNull(occurrenceOnPage)
            
            val aspectRatio = state.pageAspectRatios.getOrNull(targetPage) ?: 1.41f
            val pageHeight = (screenWidth * aspectRatio).toInt()
            val offset = if (targetMatch != null) {
                val rawOffset = (targetMatch.rect.top * pageHeight).toInt() - 250
                rawOffset.coerceIn(0, maxOf(0, pageHeight - 400))
            } else 0
            
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    currentSearchIndex = prevIndex, 
                    searchScrollTrigger = System.currentTimeMillis(),
                    searchResults = if (targetMatch != null) (state.searchResults + targetMatch).distinct() else state.searchResults
                )
                
                // If it's a different page, update but don't call jumpToPage (it fights with scrollTrigger)
                if (state.currentPage != targetPage) {
                    _uiState.value = _uiState.value.copy(currentPage = targetPage)
                    renderPagesAround(targetPage, screenWidth)
                } else {
                    refreshHighlights()
                }
            }
        }
    }

    private fun refreshHighlights() {
        val state = _uiState.value
        val query = state.searchQuery
        val currentPage = state.currentPage
        
        if (query.length < 3) return
        
        viewModelScope.launch(Dispatchers.IO) {
            val uri = Uri.parse(fileUri)
            val allMatches = mutableListOf<PdfSearchMatch>()
            
            val pagesToScan = listOf(currentPage, currentPage - 1, currentPage + 1)
                .filter { it in 0 until state.pageCount }
            
            pagesToScan.forEach { page ->
                val cacheKey = page to query
                val matches = searchCache.getOrPut(cacheKey) {
                    pdfInteractionHelper.searchInPage(uri, page, query)
                }
                allMatches.addAll(matches)
            }
            
            _uiState.value = _uiState.value.copy(searchResults = allMatches)
        }
    }


    fun copyPageText(context: Context, onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val text = pdfInteractionHelper.extractAllText(Uri.parse(fileUri), _uiState.value.currentPage)
            withContext(Dispatchers.Main) {
                onComplete(text)
            }
        }
    }


    fun setSearchOpen(open: Boolean) {
        if (!open) {
            _uiState.value = _uiState.value.copy(
                isSearchOpen = false,
                searchQuery = "",
                searchResults = emptyList(),
                currentSearchIndex = -1,
                targetScrollOffset = 0,
                occurrencePageIndices = emptyList()
            )
            searchCache.clear()
        } else {
            _uiState.value = _uiState.value.copy(isSearchOpen = true)
        }
    }

    fun jumpToPage(page: Int, screenWidth: Int) {
        _uiState.value = _uiState.value.copy(
            currentPage = page,
            jumpScrollTrigger = System.currentTimeMillis()
        )
        renderPagesAround(page, screenWidth)
    }

    private var renderJob: kotlinx.coroutines.Job? = null

    fun renderPagesAround(centerPage: Int, screenWidth: Int) {
        val cache = pdfCache ?: return
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.IO) {
            val start = maxOf(0, centerPage - 2)
            val end = minOf(cache.pageCount - 1, centerPage + 3)
            val currentBitmaps = _pageBitmaps.value.toMutableMap()
            var changed = false

            // 1. Immediate render of center page
            if (!currentBitmaps.containsKey(centerPage)) {
                cache.renderPage(centerPage, screenWidth)?.let {
                    currentBitmaps[centerPage] = it
                    _pageBitmaps.value = currentBitmaps.toMutableMap()
                }
            }

            // 2. Render neighbors
            for (i in start..end) {
                if (!isActive) return@launch
                if (!currentBitmaps.containsKey(i)) {
                    cache.renderPage(i, screenWidth)?.let {
                        currentBitmaps[i] = it
                        changed = true
                    }
                }
            }

            // 3. Cleanup far away bitmaps
            val keysToRemove = currentBitmaps.keys.filter { it < centerPage - 10 || it > centerPage + 10 }
            if (keysToRemove.isNotEmpty()) {
                for (key in keysToRemove) currentBitmaps.remove(key)
                changed = true
            }

            if (changed) {
                _pageBitmaps.value = currentBitmaps.toMap() // Explicitly convert to immutable Map to trigger state update
            }
        }
    }


    fun onPageChanged(page: Int, screenWidth: Int) {
        val prev = _uiState.value.currentPage
        if (page == prev) return
        
        // UPDATE STATE IMMEDIATELY to ensure UI synchronization and 
        // to allow the next snapshotFlow emission to be detected correctly.
        val state = _uiState.value
        _uiState.value = state.copy(
            currentPage = page,
            targetScrollOffset = 0 // Clear offset after processing to prevent jump loops
        )

        // 1. Trigger heavy rendering immediately (internally job-cancelled)
        renderPagesAround(page, screenWidth)

        // 2. Check bookmark state immediately for responsive UI
        viewModelScope.launch(Dispatchers.IO) {
            val isBookmarked = database.bookmarkDao().isPageBookmarked(fileUri, page) > 0
            withContext(Dispatchers.Main) {
                val currentState = _uiState.value
                if (currentState.currentPage == page) {
                    _uiState.value = currentState.copy(isBookmarked = isBookmarked)
                }
            }
        }

        // 3. Debounce database read progress updates (trailing debounce)
        // This prevents excessive disk I/O during rapid scrolls/jumps.
        dbUpdateJob?.cancel()
        dbUpdateJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300) // Stabilize for 300ms before committing to DB
            if (!isActive) return@launch
            
            // Save read progress
            recentFilesRepository.updateReadProgress(
                fileUri, page, _uiState.value.pageCount
            )
        }

        if (_uiState.value.searchQuery.length >= 3) {
            refreshHighlights()
        }
    }

    fun setReadingMode(mode: ReadingMode) {
        _uiState.value = _uiState.value.copy(readingMode = mode)
    }

    fun toggleFullscreen() {
        _uiState.value = _uiState.value.copy(isFullscreen = !_uiState.value.isFullscreen)
    }

    fun toggleBookmark() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val wasBookmarked = bookmarkRepository.togglePageBookmark(
                fileUri, state.fileName, state.currentPage
            )
            _uiState.value = _uiState.value.copy(isBookmarked = wasBookmarked)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pageTextCache.clear()
        pdfCache?.close()
    }
}
