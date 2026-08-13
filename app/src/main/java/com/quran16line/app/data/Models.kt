package com.quran16line.app.data

data class QuranBundle(
    val pageCount: Int,
    val linesPerPage: Int,
    val pages: List<QuranPage>
)

data class QuranPage(
    val page: Int,
    val lines: List<PageLine>,
    val surahStart: Int?,
    val surahEnd: Int?,
    val ayahStart: Int?,
    val ayahEnd: Int?
)

data class PageLine(
    val type: String,
    val text: String,
    val surah: Int?,
    val ayahStart: Int?,
    val ayahEnd: Int?
)

data class SurahInfo(
    val id: Int,
    val name: String,
    val transliteration: String,
    val totalVerses: Int,
    val page: Int,
    /** 1-based visual line on [page] where the surah text begins (Taj 16-line). */
    val startLine: Int? = null,
    val aliases: List<String>? = null
)

data class Bookmark(
    val page: Int,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class HighlightPoint(
    val page: Int,
    val lineIndex: Int
)
