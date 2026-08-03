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
    fun mushafPdfAssetIsPresent() {
        val pdf = File(assetDir(), "quran_16_lines.pdf")
        assertTrue("PDF missing at ${pdf.absolutePath}", pdf.isFile)
        assertTrue("PDF too small", pdf.length() > 1_000_000)
        // PDF header
        val header = pdf.inputStream().use { it.readNBytes(5).toString(Charsets.ISO_8859_1) }
        assertEquals("%PDF-", header)
    }

    @Test
    fun quranPagesAssetHasExpectedShape() {
        val bundle = readJson<QuranBundle>("quran_pages.json")
        assertEquals("pageCount", 558, bundle.pageCount)
        assertEquals("linesPerPage", 16, bundle.linesPerPage)
        assertEquals("pages size", bundle.pageCount, bundle.pages.size)
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
        assertEquals(6236, ayahIndex.size)
        assertTrue(expectedKeys.all(ayahIndex::containsKey))
        assertTrue(ayahIndex.values.all { it in 1..558 })
    }

    @Test
    fun readerPageNormalizationClampsSafely() {
        assertEquals(1, normalizeReaderPage(-10, 558))
        assertEquals(558, normalizeReaderPage(560, 558))
        assertEquals(1, normalizeReaderPage(2, 0))
    }

    private inline fun <reified T> readJson(fileName: String): T {
        val file = File(assetDir(), fileName)
        return gson.fromJson(file.readText(), object : TypeToken<T>() {}.type)
    }

    private fun assetDir(): File {
        val candidates = listOf(File("src/main/assets"), File("app/src/main/assets"))
        return candidates.firstOrNull { File(it, "quran_pages.json").isFile }
            ?: error("Unable to locate assets from ${File(".").absolutePath}")
    }
}
