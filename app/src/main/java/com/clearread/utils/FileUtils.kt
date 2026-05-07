package com.clearread.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Utility functions for file operations using Storage Access Framework (SAF).
 */
object FileUtils {

    /**
     * Extracts the display name from a content URI using SAF.
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var fileName: String? = null
        try {
            if (uri.scheme == "content") {
                val cursor: Cursor? = context.contentResolver.query(
                    uri, null, null, null, null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            fileName = it.getString(nameIndex)
                        }
                    }
                }
            } else if (uri.scheme == "file") {
                fileName = uri.lastPathSegment
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Final fallbacks
        return fileName ?: uri.lastPathSegment ?: "Document.pdf"
    }

    /**
     * Gets the file size in bytes from a content URI.
     */
    fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        var fileSize = 0L
        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri, null, null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        fileSize = it.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return fileSize
    }

    /**
     * Formats file size to human-readable string.
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Checks if a URI points to a PDF file by MIME type.
     */
    fun isPdfUri(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType == "application/pdf"
    }

    /**
     * Takes persistent read permission for a URI so it can be reopened later.
     */
    fun takePersistablePermission(context: Context, uri: Uri) {
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Permission may not be persistable
        }
    }

    /**
     * Copies a file from a content URI to internal storage and returns the new file URI.
     * This is useful for saving files from external intents that don't grant persistable permissions.
     */
    fun copyToInternalStorage(context: Context, uri: Uri, originalName: String): Uri? {
        return try {
            val dir = java.io.File(context.filesDir, "imported_pdfs")
            if (!dir.exists()) dir.mkdirs()
            
            val safeName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            
            val existing = findExistingImport(context, originalName)
            if (existing != null) {
                return existing
            }

            val destFile = java.io.File(dir, "${System.currentTimeMillis()}_$safeName")
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Checks if a file with the same name already exists in the imported_pdfs directory.
     */
    fun findExistingImport(context: Context, fileName: String): Uri? {
        val dir = java.io.File(context.filesDir, "imported_pdfs")
        if (!dir.exists()) return null
        
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val existingFiles = dir.listFiles()
        val existing = existingFiles?.find { it.name.endsWith("_$safeName") }
        return existing?.let { Uri.fromFile(it) }
    }

    /**
     * Deletes a file if it exists in the internal imported_pdfs directory.
     */
    fun deleteIfInternal(context: Context, filePath: String) {
        try {
            val uri = Uri.parse(filePath)
            if (uri.scheme == "file") {
                val file = java.io.File(uri.path ?: return)
                val internalDir = java.io.File(context.filesDir, "imported_pdfs")
                if (file.absolutePath.startsWith(internalDir.absolutePath)) {
                    if (file.exists()) file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Checks if a URI is still accessible.
     */
    fun canAccessUri(context: Context, uri: Uri): Boolean {
        return try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { true } ?: false
            } else if (uri.scheme == "file") {
                java.io.File(uri.path ?: return false).exists()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Converts a file:// URI to a java.io.File object.
     */
    fun getFileFromUri(uri: Uri): java.io.File? {
        return if (uri.scheme == "file") {
            java.io.File(uri.path ?: return null)
        } else null
    }
}
