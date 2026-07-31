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
 */
class PdfMushafSource(private val context: Context) {
    private val mutex = Mutex()
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    private val bitmapCache = object : LruCache<String, Bitmap>(12) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            if (evicted && !oldValue.isRecycled && oldValue !== newValue) {
                oldValue.recycle()
            }
        }
    }

    suspend fun ensureOpen(): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (renderer != null) return@withLock renderer!!.pageCount
            val target = File(context.filesDir, "quran_16_lines.pdf")
            if (!target.exists() || target.length() == 0L) {
                context.assets.open(ASSET_NAME).use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
            }
            val pfd = ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
            descriptor = pfd
            renderer = PdfRenderer(pfd)
            renderer!!.pageCount
        }
    }

    fun pageCount(): Int = renderer?.pageCount ?: 0

    suspend fun renderPage(pageNumber: Int, targetWidthPx: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            if (targetWidthPx <= 0) return@withContext null
            val index = pageNumber - 1
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
        renderer?.close()
        descriptor?.close()
        renderer = null
        descriptor = null
        bitmapCache.evictAll()
    }

    companion object {
        const val ASSET_NAME = "quran_16_lines.pdf"
        const val LINES_PER_PAGE = 16
    }
}
