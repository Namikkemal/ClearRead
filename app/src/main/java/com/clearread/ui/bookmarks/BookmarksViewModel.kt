package com.clearread.ui.bookmarks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearread.data.local.AppDatabase
import com.clearread.data.local.BookmarkEntity
import com.clearread.data.repository.BookmarkRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookmarksViewModel(application: Application) : AndroidViewModel(application) {

    private val bookmarkRepository = BookmarkRepository(
        AppDatabase.getInstance(application).bookmarkDao()
    )

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkRepository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeBookmark(id: Int) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(id)
        }
    }
}
