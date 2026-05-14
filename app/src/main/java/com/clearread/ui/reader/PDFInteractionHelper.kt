package com.clearread.ui.reader

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import com.clearread.ui.reader.PdfBookmark
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class PDFInteractionHelper(private val context: Context) {

    init {
        try {
            PDFBoxResourceLoader.init(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getTableOfContents(uri: Uri): List<PdfBookmark> = withContext(Dispatchers.IO) {
        // TOC is currently not displayed in the UI. 
        // This method can be implemented using PDFBox PDDocumentOutline when needed.
        emptyList()
    }

    suspend fun extractPageText(uri: Uri, pageIndex: Int): String = withContext(Dispatchers.IO) {
        var extractedText = ""
        try {
            val file = com.clearread.utils.FileUtils.getFileFromUri(uri)
            if (file != null && file.exists()) {
                PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly()).use { pdDoc ->
                    val stripper = PDFTextStripper()
                    stripper.startPage = pageIndex + 1
                    stripper.endPage = pageIndex + 1
                    extractedText = stripper.getText(pdDoc)
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    PDDocument.load(inputStream, MemoryUsageSetting.setupTempFileOnly()).use { pdDoc ->
                        val stripper = PDFTextStripper()
                        stripper.startPage = pageIndex + 1
                        stripper.endPage = pageIndex + 1
                        extractedText = stripper.getText(pdDoc)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        extractedText
    }

    suspend fun searchInPage(uri: Uri, pageIndex: Int, query: String): List<PdfSearchMatch> = withContext(Dispatchers.IO) {
        val matches = mutableListOf<PdfSearchMatch>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { pdDoc ->
                    val stripper = object : PDFTextStripper() {
                        init {
                            sortByPosition = true
                        }
                        
                        private fun normalize(s: String): String {
                            return s.lowercase(java.util.Locale.ROOT)
                                .replace('ç', 'c')
                                .replace('ğ', 'g')
                                .replace('ı', 'i')
                                .replace('i', 'i')
                                .replace('ö', 'o')
                                .replace('ş', 's')
                                .replace('ü', 'u')
                        }
                        
                        override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
                            val normalizedText = normalize(text)
                            val normalizedQuery = normalize(query)
                            
                            var index = normalizedText.indexOf(normalizedQuery)
                            while (index != -1) {
                                if (index + normalizedQuery.length <= textPositions.size) {
                                    val first = textPositions[index]
                                    val last = textPositions[index + normalizedQuery.length - 1]
                                    
                                    val page = pdDoc.getPage(pageIndex)
                                    val box = page.cropBox
                                    
                                    val left = first.xDirAdj / box.width
                                    val top = (first.yDirAdj - first.heightDir) / box.height
                                    val right = (last.xDirAdj + last.widthDirAdj) / box.width
                                    val bottom = (first.yDirAdj + (first.heightDir * 0.2f)) / box.height
                                    
                                    matches.add(PdfSearchMatch(pageIndex, RectF(left, top, right, bottom)))
                                }
                                index = normalizedText.indexOf(normalizedQuery, index + 1)
                            }
                        }
                    }
                    stripper.startPage = pageIndex + 1
                    stripper.endPage = pageIndex + 1
                    stripper.getText(pdDoc)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        matches
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

    /**
     * Performs a memory-efficient page-by-page search using chunking.
     * Opens the PDF for CHUNK_SIZE pages at a time, then closes it to flush
     * PDFBox's internal font/resource caches. This keeps RAM usage constant
     * regardless of document size (e.g. 50-page footprint for a 2000-page PDF).
     */
    suspend fun searchStreaming(
        uri: Uri, 
        query: String,
        cache: Map<Int, String>,
        onPageParsed: (Int, String) -> Unit,
        onMatch: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val CHUNK_SIZE = 50
        try {
            val normalizedQuery = normalizeForSearch(query)

            // First, determine total page count with a lightweight open
            val totalPages = run {
                val file = com.clearread.utils.FileUtils.getFileFromUri(uri)
                if (file != null && file.exists()) {
                    PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly()).use { it.numberOfPages }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        PDDocument.load(inputStream, MemoryUsageSetting.setupTempFileOnly()).use { it.numberOfPages }
                    } ?: 0
                }
            }
            if (totalPages == 0) return@withContext

            // Process in chunks of CHUNK_SIZE pages. Each chunk opens a fresh
            // PDDocument so that PDFBox's font/CMap caches are fully released
            // between chunks, preventing OOM on large documents.
            var chunkStart = 0
            while (chunkStart < totalPages) {
                yield() // cooperate with cancellation between chunks
                val chunkEnd = minOf(chunkStart + CHUNK_SIZE, totalPages)

                val scanChunk: suspend (PDDocument) -> Unit = { pdDoc ->
                    val stripper = PDFTextStripper()
                    stripper.sortByPosition = false
                    stripper.addMoreFormatting = false

                    for (pageIdx in chunkStart until chunkEnd) {
                        yield()
                        val pdfPage = pageIdx + 1 // PDFBox uses 1-based page numbers
                        val text = cache[pageIdx] ?: run {
                            stripper.startPage = pdfPage
                            stripper.endPage = pdfPage
                            val extracted = stripper.getText(pdDoc)
                            onPageParsed(pageIdx, extracted)
                            extracted
                        }

                        if (text.isNotEmpty()) {
                            val lowerText = normalizeForSearch(text)
                            var index = lowerText.indexOf(normalizedQuery)
                            while (index != -1) {
                                onMatch(pageIdx)
                                index = lowerText.indexOf(normalizedQuery, index + normalizedQuery.length)
                            }
                        }
                    }
                }

                // Open, scan chunk, close — this is the key to constant memory usage
                val file = com.clearread.utils.FileUtils.getFileFromUri(uri)
                if (file != null && file.exists()) {
                    PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly()).use { pdDoc ->
                        scanChunk(pdDoc)
                    }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        PDDocument.load(inputStream, MemoryUsageSetting.setupTempFileOnly()).use { pdDoc ->
                            scanChunk(pdDoc)
                        }
                    }
                }

                chunkStart = chunkEnd
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun extractAllText(uri: Uri, pageIndex: Int): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { pdDoc ->
                    val stripper = PDFTextStripper()
                    stripper.startPage = pageIndex + 1
                    stripper.endPage = pageIndex + 1
                    stripper.getText(pdDoc)
                }
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    data class SearchMatch(
        val pageIndex: Int,
        val text: String,
        val rects: List<RectF>
    )
}

/**
 * Custom PrintAdapter to handle PDF printing.
 */
class PdfPrintAdapter(
    private val context: Context,
    private val uri: Uri,
    private val fileName: String
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(fileName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        var input: FileInputStream? = null
        var output: FileOutputStream? = null

        try {
            input = FileInputStream(context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor)
            output = FileOutputStream(destination?.fileDescriptor)

            val buf = ByteArray(16384)
            var size: Int
            while (input.read(buf).also { size = it } >= 0 && cancellationSignal?.isCanceled == false) {
                output.write(buf, 0, size)
            }

            if (cancellationSignal?.isCanceled == true) {
                callback?.onWriteCancelled()
            } else {
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        } finally {
            try { input?.close(); output?.close() } catch (e: IOException) {}
        }
    }
}

data class PdfSearchMatch(val pageIndex: Int, val rect: RectF)

data class PdfBookmark(
    val title: String,
    val pageIdx: Long,
    val children: List<PdfBookmark> = emptyList(),
    val hasChildren: Boolean = children.isNotEmpty()
)
