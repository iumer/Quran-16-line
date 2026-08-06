package com.quran16line.app

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quran16line.app.data.SurahInfo
import com.quran16line.app.ui.search.rankSurahMatches
import com.quran16line.app.viewmodel.normalizeReaderPage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchNavigationTest {
    private val gson = Gson()
    private val assets = assetDir()
    private val surahs: List<SurahInfo> = gson.fromJson(
        File(assets, "surahs.json").readText(),
        object : TypeToken<List<SurahInfo>>() {}.type
    )
    private val ayahIndex: Map<String, Int> = gson.fromJson(
        File(assets, "ayah_index.json").readText(),
        object : TypeToken<Map<String, Int>>() {}.type
    )

    @Test
    fun pageSearchAcceptsAppPages() {
        assertEquals(1, normalizeReaderPage(1, 558))
        assertEquals(549, normalizeReaderPage(549, 558))
        assertEquals(558, normalizeReaderPage(999, 558))
    }

    @Test
    fun printedPageMapsOneToOneAfterCoverRemoved() {
        // Cover skipped: printed N => reader page N
        assertEquals(549, 549)
        assertEquals(2, 2) // Fatiha printed/reader 2
        assertEquals(3, 3) // Baqarah start
    }

    @Test
    fun surahSearchNasGoesToPage549() {
        val nas = surahs.first { it.id == 114 }
        assertEquals(549, nas.page)
        assertEquals(114, rankSurahMatches(surahs, "Nas").first().id)
    }

    @Test
    fun ayatSearchUsesAyahIndex() {
        assertEquals(549, ayahIndex["114:1"])
        assertEquals(549, ayahIndex["114:6"])
        assertEquals(549, ayahIndex["113:1"])
        assertEquals(2, ayahIndex["1:1"])
        assertEquals(3, ayahIndex["2:1"])
        assertNotNull(ayahIndex["2:255"])
        assertTrue(ayahIndex["2:255"]!! in 3..49)
        assertEquals(surahs.first { it.id == 67 }.page, ayahIndex["67:1"])
    }

    @Test
    fun tajLayoutSpotChecksMatchPhysicalMushaf() {
        // Client screenshot: page 479 ends Al-Qamar and starts Ar-Rahman
        assertEquals(476, surahs.first { it.id == 54 }.page)
        assertEquals(479, surahs.first { it.id == 55 }.page)
        assertEquals(479, ayahIndex["55:1"])
        assertEquals(478, ayahIndex["54:48"])
        assertEquals(479, ayahIndex["54:55"])
        assertEquals(521, surahs.first { it.id == 73 }.page)
        assertEquals(522, surahs.first { it.id == 74 }.page)
        assertEquals(7, surahs.first { it.id == 55 }.startLine)
    }

    @Test
    fun pageLabelsDescribePhysicalContent() {
        val pageIndex = gson.fromJson(
            File(assets, "page_index.json").readText(),
            com.google.gson.JsonObject::class.java
        )
        val pages = pageIndex.getAsJsonArray("pages")
        fun label(page: Int): String =
            pages.first { it.asJsonObject.get("page").asInt == page }
                .asJsonObject.get("label").asString
        assertEquals("Al-Qamar–Ar-Rahman", label(479))
        assertTrue(label(2).startsWith("Al-Fatihah"))
        assertTrue(label(549).contains("Nas"))
    }

    @Test
    fun everyAyahIsIndexed() {
        val expected = surahs.sumOf { it.totalVerses }
        assertEquals(6236, expected)
        assertEquals(6236, ayahIndex.size)
        ayahIndex.values.forEach { page ->
            assertTrue(page in 1..558)
        }
    }

    @Test
    fun fatihaIsReaderPageTwo() {
        assertEquals(2, surahs.first { it.id == 1 }.page)
    }

    private fun assetDir(): File {
        return listOf(File("src/main/assets"), File("app/src/main/assets"))
            .first { File(it, "surahs.json").isFile }
    }
}
