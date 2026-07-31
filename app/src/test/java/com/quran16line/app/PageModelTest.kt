package com.quran16line.app

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quran16line.app.data.QuranBundle
import com.quran16line.app.data.SurahInfo
import com.quran16line.app.viewmodel.normalizeReaderPage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageModelTest {
    private val gson = Gson()

    @Test
    fun quranPagesAssetHasExpectedShape() {
        val bundle = readJson<QuranBundle>("quran_pages.json")

        assertEquals("pageCount", 559, bundle.pageCount)
        assertEquals("linesPerPage", 16, bundle.linesPerPage)
        assertEquals("pages size", bundle.pageCount, bundle.pages.size)
        assertEquals("page numbers", (1..559).toList(), bundle.pages.map { it.page })

        bundle.pages.forEach { page ->
            assertEquals("page ${page.page} line count", 16, page.lines.size)
        }
    }

    @Test
    fun surahsAssetContainsAllSurahs() {
        val surahs = readJson<List<SurahInfo>>("surahs.json")

        assertEquals("surah count", 114, surahs.size)
        assertEquals("surah ids", (1..114).toList(), surahs.map { it.id }.sorted())
        assertEquals("Quran ayah total", 6236, surahs.sumOf { it.totalVerses })
    }

    @Test
    fun ayahIndexCoversEveryAyah() {
        val surahs = readJson<List<SurahInfo>>("surahs.json")
        val ayahIndex = readJson<Map<String, Int>>("ayah_index.json")
        val expectedKeys = surahs.flatMap { surah ->
            (1..surah.totalVerses).map { ayah -> "${surah.id}:$ayah" }
        }.toSet()

        assertEquals("ayah index entry count", 6236, ayahIndex.size)
        assertEquals("expected ayah key count", 6236, expectedKeys.size)
        assertTrue("missing ayahs: ${(expectedKeys - ayahIndex.keys).take(10)}", expectedKeys.all(ayahIndex::containsKey))
        assertTrue("unexpected ayahs: ${(ayahIndex.keys - expectedKeys).take(10)}", ayahIndex.keys.all(expectedKeys::contains))
        assertTrue("all indexed pages are within the 559-page mushaf", ayahIndex.values.all { it in 1..559 })
    }

    @Test
    fun readerPageNormalizationClampsSafely() {
        assertEquals(1, normalizeReaderPage(-10, 559))
        assertEquals(1, normalizeReaderPage(0, 559))
        assertEquals(2, normalizeReaderPage(2, 559))
        assertEquals(559, normalizeReaderPage(560, 559))
        assertEquals(1, normalizeReaderPage(2, 0))
    }

    private inline fun <reified T> readJson(fileName: String): T {
        val file = File(assetDir(), fileName)
        return gson.fromJson(file.readText(), object : TypeToken<T>() {}.type)
    }

    private fun assetDir(): File {
        val candidates = listOf(
            File("src/main/assets"),
            File("app/src/main/assets")
        )
        return candidates.firstOrNull { File(it, "quran_pages.json").isFile }
            ?: error("Unable to locate app/src/main/assets from ${File(".").absolutePath}")
    }
}
