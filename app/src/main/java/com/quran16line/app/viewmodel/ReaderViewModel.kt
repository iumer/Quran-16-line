package com.quran16line.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quran16line.app.data.Bookmark
import com.quran16line.app.data.HighlightPoint
import com.quran16line.app.data.PageIndexEntry
import com.quran16line.app.data.PdfMushafSource
import com.quran16line.app.data.PreferencesStore
import com.quran16line.app.data.QuranRepository
import com.quran16line.app.data.SurahInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val ready: Boolean = false,
    val pageCount: Int = 0,
    val currentPage: Int = 3,
    val pageEntry: PageIndexEntry? = null,
    val pageLabel: String = "",
    val highlight: HighlightPoint? = null,
    val bookmarks: List<Bookmark> = emptyList(),
    val surahs: List<SurahInfo> = emptyList(),
    val isBookmarked: Boolean = false,
    val showSearch: Boolean = false,
    val showBookmarks: Boolean = false,
    val message: String? = null,
    val linesPerPage: Int = PdfMushafSource.LINES_PER_PAGE
)

class ReaderViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = QuranRepository(app)
    private val prefs = PreferencesStore(app)

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureLoaded()
            val pageCount = repository.pageCount()
            val initialPage = normalizeReaderPage(_state.value.currentPage, pageCount)
            _state.update {
                it.copy(
                    ready = true,
                    pageCount = pageCount,
                    currentPage = initialPage,
                    pageEntry = repository.pageEntry(initialPage),
                    pageLabel = repository.labelForPage(initialPage),
                    surahs = repository.surahList()
                )
            }

            combine(prefs.lastPage, prefs.highlight, prefs.bookmarks) { last, highlight, bookmarks ->
                Triple(last, highlight, bookmarks)
            }.collect { (last, highlight, bookmarks) ->
                val persistedPage = normalizeReaderPage(last, repository.pageCount())
                _state.update { current ->
                    current.copy(
                        currentPage = persistedPage,
                        pageEntry = repository.pageEntry(persistedPage),
                        pageLabel = repository.labelForPage(persistedPage),
                        highlight = highlight,
                        bookmarks = bookmarks,
                        isBookmarked = bookmarks.any { it.page == persistedPage }
                    )
                }
            }
        }
    }

    fun onPageChanged(page: Int) {
        viewModelScope.launch {
            val pageCount = repository.pageCount()
            if (pageCount < 1) return@launch
            val safe = normalizeReaderPage(page, pageCount)
            prefs.setLastPage(safe)
            _state.update {
                it.copy(
                    currentPage = safe,
                    pageEntry = repository.pageEntry(safe),
                    pageLabel = repository.labelForPage(safe),
                    isBookmarked = it.bookmarks.any { b -> b.page == safe }
                )
            }
        }
    }

    fun onLineTapped(page: Int, lineIndex: Int) {
        viewModelScope.launch {
            val current = _state.value.highlight
            val next = if (current?.page == page && current.lineIndex == lineIndex) {
                null
            } else {
                HighlightPoint(page, lineIndex.coerceIn(0, PdfMushafSource.LINES_PER_PAGE - 1))
            }
            prefs.setHighlight(next)
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val page = _state.value.currentPage
            val bookmark = Bookmark(page = page, label = repository.labelForPage(page))
            val added = prefs.toggleBookmark(bookmark)
            _state.update {
                it.copy(message = if (added) "Bookmark saved" else "Bookmark removed")
            }
        }
    }

    fun openSearch(open: Boolean) = _state.update { it.copy(showSearch = open) }
    fun openBookmarks(open: Boolean) = _state.update { it.copy(showBookmarks = open) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun jumpToPage(page: Int) {
        onPageChanged(page)
        openSearch(false)
        openBookmarks(false)
    }

    fun jumpToSurah(surahId: Int) {
        val page = repository.pageForSurah(surahId)
        if (page == null) {
            _state.update { it.copy(message = "Surah not found") }
            return
        }
        jumpToPage(page)
    }

    fun jumpToAyah(surahId: Int, ayah: Int) {
        val page = repository.pageForAyah(surahId, ayah)
        if (page == null) {
            _state.update { it.copy(message = "Ayat not found") }
            return
        }
        jumpToPage(page)
    }

    fun surahTotalVerses(surahId: Int): Int =
        repository.surahById(surahId)?.totalVerses ?: 1

    fun previewPageLabel(page: Int): String = repository.labelForPage(page)

    fun resolvePageQuery(raw: Int, preferPrinted: Boolean): Int? =
        repository.resolvePageQuery(raw, preferPrinted)

    fun pageForAyah(surahId: Int, ayah: Int): Int? =
        repository.pageForAyah(surahId, ayah)

    suspend fun renderPage(pageNumber: Int, widthPx: Int): Bitmap? =
        repository.pdf().renderPage(pageNumber, widthPx)

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }
}

internal fun normalizeReaderPage(page: Int, pageCount: Int): Int =
    if (pageCount < 1) 1 else page.coerceIn(1, pageCount)
