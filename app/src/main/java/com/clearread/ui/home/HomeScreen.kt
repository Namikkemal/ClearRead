package com.clearread.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.clearread.ui.home.FilePropertiesData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clearread.data.local.BookmarkEntity
import com.clearread.data.local.RecentFileEntity
import android.content.Context
import android.content.Intent
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContract
import com.clearread.R

class OpenPdfContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
    }
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToReader: (String, Int) -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToExplorer: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val recentFiles by viewModel.recentFiles.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val properties by viewModel.fileProperties.collectAsState()

    if (properties != null) {
        FilePropertiesDialog(
            data = properties!!,
            onDismiss = { viewModel.dismissFileProperties() }
        )
    }

    // SAF file picker launcher — opens system file picker for PDFs only
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = OpenPdfContract()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onFilePicked(it) { fileUri ->
                onNavigateToReader(fileUri, -1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToExplorer) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = stringResource(R.string.acc_explorer),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.acc_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filePickerLauncher.launch(Unit) },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.NoteAdd,
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(R.string.action_open)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val isSearchEmpty = searchQuery.isNotEmpty() && recentFiles.isEmpty() && bookmarks.isEmpty()
            val isDataEmpty = searchQuery.isEmpty() && recentFiles.isEmpty() && bookmarks.isEmpty()

            AnimatedVisibility(
                visible = isDataEmpty && !isLoading,
                enter = fadeIn(animationSpec = tween(500)),
                exit = fadeOut()
            ) {
                EmptyState(
                    onOpenFile = { filePickerLauncher.launch(Unit) }
                )
            }

            AnimatedVisibility(
                visible = (!isDataEmpty || isSearchEmpty) && !isLoading,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(400)
                ),
                exit = fadeOut()
            ) {
                val context = LocalContext.current
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp) // Space for FAB
                ) {
                    // --- Search Bar Section ---
                    item {
                        SearchBarSection(
                            query = searchQuery,
                            onQueryChange = { viewModel.updateSearchQuery(it) }
                        )
                    }

                    if (isSearchEmpty) {
                        item {
                            NoSearchResults(query = searchQuery)
                        }
                    }

                    // --- Bookmarked Files Section ---
                    if (bookmarks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.home_bookmarks),
                                icon = Icons.Outlined.BookmarkBorder,
                                actionText = stringResource(R.string.home_view_all),
                                onAction = onNavigateToBookmarks
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = bookmarks.take(10),
                                    key = { it.id }
                                ) { bookmark ->
                                    BookmarkChip(
                                        bookmark = bookmark,
                                        onClick = { onNavigateToReader(bookmark.filePath, bookmark.pageNumber) },
                                        onShare = { viewModel.shareFile(context, bookmark.filePath) },
                                        onPrint = { viewModel.printFile(context, bookmark.filePath, bookmark.fileName) },
                                        onProperties = { viewModel.showFileProperties(context, bookmark.filePath, bookmark.fileName, bookmark.createdAt, 0) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // --- Recent Files Section ---
                    if (recentFiles.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.home_recent_files),
                                icon = Icons.Outlined.Description
                            )
                        }
                        items(
                            items = recentFiles,
                            key = { it.id }
                        ) { recentFile ->
                            RecentFileCard(
                                recentFile = recentFile,
                                onClick = {
                                    viewModel.onRecentFileClicked(recentFile) { uri ->
                                        onNavigateToReader(uri, -1)
                                    }
                                },
                                onDelete = { viewModel.deleteRecentFile(recentFile.id) },
                                onShare = { viewModel.shareFile(context, recentFile.filePath) },
                                onPrint = { viewModel.printFile(context, recentFile.filePath, recentFile.fileName) },
                                onProperties = { viewModel.showFileProperties(context, recentFile.filePath, recentFile.fileName, recentFile.lastOpenedAt, recentFile.totalPages) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
// Search Components
// ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = { 
                Text(
                    stringResource(R.string.home_search),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                ) 
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.acc_clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            singleLine = true
        )
    }
}

@Composable
private fun NoSearchResults(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_no_results, query),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.home_check_spelling),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ──────────────────────────────────────────────────────────────────────
// Empty State
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(onOpenFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large illustration icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.home_no_files),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.home_no_files_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        ExtendedFloatingActionButton(
            onClick = onOpenFile,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.NoteAdd,
                    contentDescription = null
                )
            },
            text = { Text(stringResource(R.string.home_open_pdf)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}

// ──────────────────────────────────────────────────────────────────────
// Section Header
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
// Recent File Card
// ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentFileCard(
    recentFile: RecentFileEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onProperties: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PDF icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // File info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recentFile.fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatTimestamp(recentFile.lastOpenedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (recentFile.totalPages > 0) {
                            Text(
                                text = "  •  ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.nav_page_format, recentFile.lastPageRead + 1, recentFile.totalPages),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Reading progress bar
                    if (recentFile.totalPages > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = {
                                (recentFile.lastPageRead + 1).toFloat() / recentFile.totalPages.toFloat()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_share)) },
                    onClick = { 
                        onShare()
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Outlined.Share, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_print)) },
                    onClick = { 
                        onPrint()
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Outlined.Print, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_properties)) },
                    onClick = { 
                        onProperties()
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Outlined.Description, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.acc_remove_recent)) },
                    onClick = { 
                        onDelete()
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
// Bookmark Chip (horizontal scroll)
// ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkChip(
    bookmark: BookmarkEntity,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onProperties: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            Column {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = bookmark.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (bookmark.label != null) bookmark.label
                           else stringResource(R.string.bookmarks_page, bookmark.pageNumber + 1),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_share)) },
                    onClick = { 
                        onShare()
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Outlined.Share, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_print)) },
                    onClick = { 
                        onPrint()
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Outlined.Print, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_properties)) },
                    onClick = { 
                        onProperties()
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Outlined.Description, null) }
                )
            }
        }
    }
}

@Composable
private fun FilePropertiesDialog(
    data: FilePropertiesData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_properties)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PropertyItem(stringResource(R.string.prop_file_name), data.fileName)
                PropertyItem(stringResource(R.string.prop_file_path), data.filePath)
                PropertyItem(stringResource(R.string.prop_file_size), data.fileSize)
                PropertyItem(stringResource(R.string.prop_last_opened), formatTimestamp(data.lastOpened))
                if (data.pageCount > 0) {
                    PropertyItem(stringResource(R.string.prop_page_count), data.pageCount.toString())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun PropertyItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ──────────────────────────────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> stringResource(R.string.home_just_now)
        diff < 3_600_000 -> stringResource(R.string.home_min_ago, diff / 60_000)
        diff < 86_400_000 -> stringResource(R.string.home_h_ago, diff / 3_600_000)
        diff < 172_800_000 -> stringResource(R.string.home_yesterday)
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
