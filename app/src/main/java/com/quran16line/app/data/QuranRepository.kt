package com.quran16line.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuranRepository(private val context: Context) {
    private val gson = Gson()

    @Volatile
    private var bundle: QuranBundle? = null

    @Volatile
    private var surahs: List<SurahInfo>? = null

    @Volatile
    private var ayahIndex: Map<String, Int>? = null

    suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        if (bundle == null) {
            val json = context.assets.open("quran_pages.json").bufferedReader().use { it.readText() }
            bundle = gson.fromJson(json, QuranBundle::class.java)
        }
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
    }

    fun pageCount(): Int = bundle?.pageCount ?: 0

    fun page(pageNumber: Int): QuranPage? {
        val pages = bundle?.pages ?: return null
        if (pageNumber < 1 || pageNumber > pages.size) return null
        return pages[pageNumber - 1]
    }

    fun surahList(): List<SurahInfo> = surahs.orEmpty()

    fun surahById(id: Int): SurahInfo? = surahs?.firstOrNull { it.id == id }

    fun pageForSurah(surahId: Int): Int? = surahs?.firstOrNull { it.id == surahId }?.page

    fun pageForAyah(surahId: Int, ayah: Int): Int? = ayahIndex?.get("$surahId:$ayah")

    fun labelForPage(pageNumber: Int): String {
        val page = page(pageNumber) ?: return "Page $pageNumber"
        val surah = page.surahStart?.let { surahById(it) }
        return if (surah != null) {
            val ayahPart = when {
                page.ayahStart != null && page.ayahEnd != null && page.ayahStart != page.ayahEnd ->
                    " · Ayah ${page.ayahStart}–${page.ayahEnd}"
                page.ayahStart != null -> " · Ayah ${page.ayahStart}"
                else -> ""
            }
            "${surah.transliteration}$ayahPart"
        } else {
            "Page $pageNumber"
        }
    }
}
