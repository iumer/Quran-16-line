package com.quran16line.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PageIndexBundle(
    val pageCount: Int,
    val pages: List<PageIndexEntry>
)

data class PageIndexEntry(
    val page: Int,
    val printedPage: Int?,
    val surahStart: Int?,
    val surahEnd: Int?,
    val ayahStart: Int?,
    val ayahEnd: Int?,
    val label: String
)

class QuranRepository(
    private val context: Context,
    private val pdfSource: PdfMushafSource = PdfMushafSource(context)
) {
    private val gson = Gson()

    @Volatile
    private var surahs: List<SurahInfo>? = null

    @Volatile
    private var ayahIndex: Map<String, Int>? = null

    @Volatile
    private var pageIndex: Map<Int, PageIndexEntry>? = null

    suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        pdfSource.ensureOpen()
        if (surahs == null) {
            val json = context.assets.open("surahs.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<SurahInfo>>() {}.type
            surahs = gson.fromJson(json, type)
        }
        if (ayahIndex == null) {
            val json = context.assets.open("ayah_index.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, Double>>() {}.type
            val raw: Map<String, Double> = gson.fromJson(json, type)
            ayahIndex = raw.mapValues { it.value.toInt() }
        }
        if (pageIndex == null) {
            val json = context.assets.open("page_index.json").bufferedReader().use { it.readText() }
            val bundle = gson.fromJson(json, PageIndexBundle::class.java)
            pageIndex = bundle.pages.associateBy { it.page }
        }
    }

    fun pageCount(): Int = pdfSource.pageCount().takeIf { it > 0 }
        ?: pageIndex?.size
        ?: 558

    fun pdf(): PdfMushafSource = pdfSource

    fun pageEntry(pageNumber: Int): PageIndexEntry? = pageIndex?.get(pageNumber)

    fun surahList(): List<SurahInfo> = surahs.orEmpty()

    fun surahById(id: Int): SurahInfo? = surahs?.firstOrNull { it.id == id }

    fun pageForSurah(surahId: Int): Int? = surahs?.firstOrNull { it.id == surahId }?.page

    fun pageForAyah(surahId: Int, ayah: Int): Int? {
        val direct = ayahIndex?.get("$surahId:$ayah")
        if (direct != null) return direct
        // Fallback: surah start if ayah is valid
        val surah = surahById(surahId) ?: return null
        if (ayah !in 1..surah.totalVerses) return null
        return surah.page
    }

    /**
     * Resolves a typed page query after the cover page was removed from the reader.
     * - App/reader pages: 1..pageCount (558)
     * - Printed mushaf page N maps 1:1 to reader page N
     *   (Title=1, Al-Fatihah=2 / printed 2, …)
     */
    fun resolvePageQuery(raw: Int, preferPrinted: Boolean = false): Int? {
        val count = pageCount()
        if (count < 1) return null
        // After cover removal, printed page N == reader page N.
        if (preferPrinted) {
            return raw.takeIf { it in 1..count }
        }
        return raw.takeIf { it in 1..count }
    }

    fun labelForPage(pageNumber: Int): String {
        pageEntry(pageNumber)?.label?.takeIf { it.isNotBlank() }?.let { return it }
        val surah = surahs?.lastOrNull { it.page <= pageNumber }
        return surah?.transliteration ?: "Page $pageNumber"
    }

    fun close() = pdfSource.close()
}
