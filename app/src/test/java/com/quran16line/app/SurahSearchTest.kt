package com.quran16line.app

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quran16line.app.data.SurahInfo
import com.quran16line.app.ui.search.rankSurahMatches
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurahSearchTest {
    private val surahs: List<SurahInfo> by lazy {
        val dir = listOf(File("src/main/assets"), File("app/src/main/assets"))
            .first { File(it, "surahs.json").isFile }
        Gson().fromJson(File(dir, "surahs.json").readText(), object : TypeToken<List<SurahInfo>>() {}.type)
    }

    @Test
    fun anNasMapsToReaderPage549() {
        val nas = surahs.first { it.id == 114 }
        assertEquals(549, nas.page)
    }

    @Test
    fun searchNasPrefersAnNasOverAnNasr() {
        val ranked = rankSurahMatches(surahs, "Nas")
        assertTrue(ranked.isNotEmpty())
        assertEquals(114, ranked.first().id)
    }
}
