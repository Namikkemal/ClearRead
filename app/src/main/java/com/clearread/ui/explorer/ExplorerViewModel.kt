package com.clearread.ui.explorer

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearread.data.local.PreferencesManager
import com.clearread.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PdfFileInfo(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModified: Long
)

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager.getInstance(application)

    private val _recentFolder = MutableStateFlow<Pair<String, String>?>(null)
    val recentFolder: StateFlow<Pair<String, String>?> = _recentFolder.asStateFlow()

    init {
        val uri = prefs.getRecentFolderUri()
        val name = prefs.getRecentFolderName()
        if (uri != null && name != null) {
            _recentFolder.value = uri to name
        }
    }

    private val _rawPdfFiles = MutableStateFlow<List<PdfFileInfo>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val pdfFiles: StateFlow<List<PdfFileInfo>> = combine(
        _rawPdfFiles,
        _searchQuery
    ) { files, query ->
        if (query.isEmpty()) files
        else files.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _folderName = MutableStateFlow<String?>(null)
    val folderName: StateFlow<String?> = _folderName.asStateFlow()

    fun loadFolder(treeUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _searchQuery.value = ""
            _rawPdfFiles.value = emptyList()

            withContext(Dispatchers.IO) {
                try {
                    val context = getApplication<Application>()

                    // Take persistent permission
                    try {
                        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        context.contentResolver.takePersistableUriPermission(treeUri, flags)
                    } catch (_: SecurityException) {}

                    val docFile = DocumentFile.fromTreeUri(context, treeUri)
                    val name = docFile?.name ?: "Files"
                    _folderName.value = name
                    
                    // Save as recent
                    prefs.saveRecentFolder(treeUri.toString(), name)
                    _recentFolder.value = treeUri.toString() to name

                    val pdfs = mutableListOf<PdfFileInfo>()
                    scanForPdfs(docFile, pdfs)

                    pdfs.sortByDescending { it.lastModified }
                    _rawPdfFiles.value = pdfs
                } catch (_: Exception) {
                    _rawPdfFiles.value = emptyList()
                }
            }

            _isLoading.value = false
        }
    }

    private fun scanForPdfs(dir: DocumentFile?, results: MutableList<PdfFileInfo>) {
        dir?.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanForPdfs(file, results)
            } else if (file.type == "application/pdf" || file.name?.endsWith(".pdf", true) == true) {
                results.add(
                    PdfFileInfo(
                        uri = file.uri,
                        name = file.name ?: "Unknown.pdf",
                        size = file.length(),
                        lastModified = file.lastModified()
                    )
                )
            }
        }
    }
}
