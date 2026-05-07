package com.clearread.utils

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import java.io.IOException

/**
 * Utility functions for PDF operations using Android's built-in PdfRenderer.
 */
object PdfUtils {

    /**
     * Gets the total page count of a PDF file from its URI.
     * Returns 0 if the file cannot be read.
     */
    fun getPageCount(context: Context, uri: Uri): Int {
        return try {
            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            fileDescriptor?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                val count = renderer.pageCount
                renderer.close()
                count
            } ?: 0
        } catch (_: IOException) {
            0
        } catch (_: SecurityException) {
            0
        }
    }

    /**
     * Validates that a URI points to a readable PDF file.
     */
    fun isValidPdf(context: Context, uri: Uri): Boolean {
        return try {
            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            fileDescriptor?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                val isValid = renderer.pageCount > 0
                renderer.close()
                isValid
            } ?: false
        } catch (_: Exception) {
            false
        }
    }
}
