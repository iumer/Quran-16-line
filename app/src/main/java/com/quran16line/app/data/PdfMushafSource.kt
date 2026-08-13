package com.quran16line.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Renders pages from the bundled Taj Company 16-line mushaf PDF
 * (`assets/quran_16_lines.pdf`, sourced from the repo-uploaded original).
 *
 * The decorative cover (PDF page 1 / index 0) is skipped from the reader:
 * display page 1 = PDF page 2 (Title), Al-Fatihah = display page 2, etc.
 */
class PdfMushafSource(private val context: Context) {
    private val mutex = Mutex()
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    private val bitmapCache = object : LruCache<String, Bitmap>(24) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
        // Do not recycle on eviction — pages may still be on-screen and recycling causes flicker.
    }

    suspend fun ensureOpen(): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (renderer != null) return@withLock readerPageCountLocked()
            val target = File(context.filesDir, "quran_16_lines.pdf")
            fun copyFromAssets() {
                context.assets.open(ASSET_NAME).use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
            }
            if (!target.exists() || target.length() == 0L) {
                copyFromAssets()
            }
            try {
                openRendererLocked(target)
            } catch (_: Exception) {
                // Stale/corrupt cached copy from an older install — replace and retry once.
                closeLocked()
                if (target.exists()) target.delete()
                copyFromAssets()
                openRendererLocked(target)
            }
            readerPageCountLocked()
        }
    }

    private fun openRendererLocked(target: File) {
        val pfd = ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
        descriptor = pfd
        renderer = PdfRenderer(pfd)
    }

    private fun closeLocked() {
        try {
            renderer?.close()
        } catch (_: Exception) {
        }
        try {
            descriptor?.close()
        } catch (_: Exception) {
        }
        renderer = null
        descriptor = null
    }

    fun pageCount(): Int = renderer?.let { (it.pageCount - SKIP_LEADING_PDF_PAGES).coerceAtLeast(0) } ?: 0

    private fun readerPageCountLocked(): Int =
        renderer?.let { (it.pageCount - SKIP_LEADING_PDF_PAGES).coerceAtLeast(0) } ?: 0

    /** Maps a 1-based reader page to the underlying PDF page index. */
    fun pdfIndexForReaderPage(pageNumber: Int): Int =
        pageNumber - 1 + SKIP_LEADING_PDF_PAGES

    suspend fun renderPage(pageNumber: Int, targetWidthPx: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            if (targetWidthPx <= 0) return@withContext null
            val index = pdfIndexForReaderPage(pageNumber)
            mutex.withLock {
                val pdf = renderer ?: return@withLock null
                if (index !in 0 until pdf.pageCount) return@withLock null
                val key = "$pageNumber@$targetWidthPx"
                bitmapCache.get(key)?.let { cached ->
                    if (!cached.isRecycled) return@withLock cached
                }
                pdf.openPage(index).use { page ->
                    val scale = targetWidthPx.toFloat() / page.width.toFloat()
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(targetWidthPx, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmapCache.put(key, bitmap)
                    bitmap
                }
            }
        }

    fun close() {
        closeLocked()
        bitmapCache.evictAll()
    }

    companion object {
        const val ASSET_NAME = "quran_16_lines.pdf"
        const val LINES_PER_PAGE = 16
        /** Skip the decorative cover so reader page 1 is the Title page. */
        const val SKIP_LEADING_PDF_PAGES = 1
        const val DEFAULT_START_PAGE = 2 // Al-Fatihah after cover removal
    }
}
