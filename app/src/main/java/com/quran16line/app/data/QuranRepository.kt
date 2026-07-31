package com.quran16line.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    private var pageMeta: Map<Int, QuranPage>? = null

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
        if (pageMeta == null && context.assets.list("")?.contains("quran_pages.json") == true) {
            runCatching {
                val json = context.assets.open("quran_pages.json").bufferedReader().use { it.readText() }
                val bundle = gson.fromJson(json, QuranBundle::class.java)
                pageMeta = bundle.pages.associateBy { it.page }
            }
        }
    }

    fun pageCount(): Int = pdfSource.pageCount()

    fun pdf(): PdfMushafSource = pdfSource

    fun page(pageNumber: Int): QuranPage? = pageMeta?.get(pageNumber)

    fun surahList(): List<SurahInfo> = surahs.orEmpty()

    fun surahById(id: Int): SurahInfo? = surahs?.firstOrNull { it.id == id }

    fun pageForSurah(surahId: Int): Int? = surahs?.firstOrNull { it.id == surahId }?.page

    fun pageForAyah(surahId: Int, ayah: Int): Int? = ayahIndex?.get("$surahId:$ayah")

    fun labelForPage(pageNumber: Int): String {
        val meta = page(pageNumber)
        val surah = meta?.surahStart?.let { surahById(it) }
            ?: surahs?.lastOrNull { it.page <= pageNumber }
        return if (surah != null) {
            val ayahPart = when {
                meta?.ayahStart != null && meta.ayahEnd != null && meta.ayahStart != meta.ayahEnd ->
                    " · Ayah ${meta.ayahStart}–${meta.ayahEnd}"
                meta?.ayahStart != null -> " · Ayah ${meta.ayahStart}"
                else -> ""
            }
            "${surah.transliteration}$ayahPart"
        } else {
            "Page $pageNumber"
        }
    }

    fun close() = pdfSource.close()
}
