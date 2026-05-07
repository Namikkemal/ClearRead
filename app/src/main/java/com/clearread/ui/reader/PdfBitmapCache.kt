package com.clearread.ui.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache

/**
 * Manages PDF rendering using Android's built-in PdfRenderer.
 * Renders pages as Bitmaps and caches them in an LruCache sized to
 * 20% of the app's available heap — Android automatically evicts old
 * pages when memory pressure rises.
 */
class PdfBitmapCache(
    private val context: Context,
    private val uri: Uri
) {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    
    var pageAspectRatios: List<Float> = emptyList()
        private set

    /** LruCache keyed by page index, sized by bitmap byte count */
    private val bitmapCache: LruCache<Int, Bitmap> = run {
        val maxMemory = Runtime.getRuntime().maxMemory()          // bytes
        val cacheSize = (maxMemory / 5).toInt()                   // 20 %
        object : LruCache<Int, Bitmap>(cacheSize) {
            override fun sizeOf(key: Int, value: Bitmap): Int =
                value.byteCount
        }
    }

    val pageCount: Int
        get() = renderer?.pageCount ?: 0

    /**
     * Opens the PDF for rendering. Must be called before rendering pages.
     * Returns null on success, or an error string on failure.
     */
    fun open(): String? {
        return try {
            fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (fileDescriptor != null) {
                renderer = PdfRenderer(fileDescriptor!!)
                scanPageAspectRatios()
                null // Success, no error
            } else {
                "File descriptor is null"
            }
        } catch (e: Exception) {
            e.message ?: e.javaClass.simpleName
        }
    }

    private fun scanPageAspectRatios() {
        val count = pageCount
        if (count == 0) return
        
        val ratios = FloatArray(count) { 1.414f } // A4 fallback
        try {
            for (i in 0 until count) {
                val page = renderer?.openPage(i)
                if (page != null) {
                    ratios[i] = page.height.toFloat() / page.width.toFloat()
                    page.close()
                }
            }
        } catch (e: Exception) {}

        pageAspectRatios = ratios.toList()
    }

    /**
     * Returns a cached bitmap if available, otherwise renders the page,
     * stores it in the LruCache, and returns it.
     *
     * @param pageIndex 0-based page index
     * @param screenWidth width to scale the rendered page to
     * @return Bitmap of the rendered page, or null on failure
     */
    @Synchronized
    fun renderPage(pageIndex: Int, screenWidth: Int): Bitmap? {
        if (pageIndex < 0 || pageIndex >= pageCount) return null

        // Return from cache when available
        bitmapCache.get(pageIndex)?.let { return it }

        return try {
            val page = renderer?.openPage(pageIndex) ?: return null
            val scale = screenWidth.toFloat() / page.width
            val bitmapWidth = screenWidth
            val bitmapHeight = (page.height * scale).toInt()

            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)

            // Draw a white background first (PDF backgrounds are transparent)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            bitmapCache.put(pageIndex, bitmap)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Releases renderer resources and clears the bitmap cache.
     */
    fun close() {
        bitmapCache.evictAll()
        try {
            renderer?.close()
        } catch (_: Exception) {}
        try {
            fileDescriptor?.close()
        } catch (_: Exception) {}
        renderer = null
        fileDescriptor = null
    }
}
