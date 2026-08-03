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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val ready: Boolean = false,
    val pageCount: Int = 0,
    val currentPage: Int = PdfMushafSource.DEFAULT_START_PAGE,
    val pageEntry: PageIndexEntry? = null,
    val pageLabel: String = "",
    val highlight: HighlightPoint? = null,
    val bookmarks: List<Bookmark> = emptyList(),
    val surahs: List<SurahInfo> = emptyList(),
    val isBookmarked: Boolean = false,
    val showSearch: Boolean = false,
    val showBookmarks: Boolean = false,
    val showHome: Boolean = true,
    val resumePage: Int = PdfMushafSource.DEFAULT_START_PAGE,
    val resumeLabel: String = "",
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
            prefs.migratePageIndexIfNeeded()
            val pageCount = repository.pageCount()
            // Prefer the highlight mark (left-off page/line); fall back to last scrolled page.
            val savedHighlight = prefs.highlight.first()
            val savedLastPage = prefs.lastPage.first()
            val resumePage = resumeReaderPage(savedLastPage, savedHighlight, pageCount)
            val savedBookmarks = prefs.bookmarks.first()
            _state.update {
                it.copy(
                    ready = true,
                    pageCount = pageCount,
                    currentPage = resumePage,
                    pageEntry = repository.pageEntry(resumePage),
                    pageLabel = repository.labelForPage(resumePage),
                    surahs = repository.surahList(),
                    highlight = savedHighlight,
                    bookmarks = savedBookmarks,
                    isBookmarked = savedBookmarks.any { b -> b.page == resumePage },
                    showHome = true,
                    resumePage = resumePage,
                    resumeLabel = "Page $resumePage · ${repository.labelForPage(resumePage)}"
                )
            }

            // After startup, never overwrite currentPage from DataStore (that races with swipes).
            combine(prefs.highlight, prefs.bookmarks) { highlight, bookmarks ->
                highlight to bookmarks
            }.collect { (highlight, bookmarks) ->
                _state.update { current ->
                    current.copy(
                        highlight = highlight,
                        bookmarks = bookmarks,
                        isBookmarked = bookmarks.any { it.page == current.currentPage }
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
            val previous = _state.value.currentPage
            _state.update {
                it.copy(
                    currentPage = safe,
                    pageEntry = repository.pageEntry(safe),
                    pageLabel = repository.labelForPage(safe),
                    isBookmarked = it.bookmarks.any { b -> b.page == safe }
                )
            }
            if (previous != safe) {
                prefs.setLastPage(safe)
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
            // Highlight is the "left off here" mark — persist page + line together.
            _state.update {
                it.copy(
                    highlight = next,
                    currentPage = page,
                    pageEntry = repository.pageEntry(page),
                    pageLabel = repository.labelForPage(page),
                    isBookmarked = it.bookmarks.any { b -> b.page == page }
                )
            }
            prefs.saveReadingMark(page = page, highlight = next)
        }
    }

    /** Flush page (and keep highlight) when the app goes to background. */
    fun persistReadingPosition() {
        viewModelScope.launch {
            val s = _state.value
            if (!s.ready || s.pageCount < 1) return@launch
            prefs.saveReadingMark(
                page = normalizeReaderPage(s.currentPage, s.pageCount),
                highlight = s.highlight
            )
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

    fun resumeReading() {
        val page = _state.value.resumePage
        _state.update { it.copy(showHome = false) }
        jumpToPage(page)
    }

    fun startFromFirstPage() {
        _state.update { it.copy(showHome = false) }
        jumpToPage(1)
    }

    fun openSearchFromHome() {
        _state.update { it.copy(showHome = false, showSearch = true) }
    }

    fun openHome() = _state.update { it.copy(showHome = true, showSearch = false, showBookmarks = false) }

    /** Call when the app goes to background so the next open shows Resume / Start / Search. */
    fun prepareLaunchChooser() {
        val s = _state.value
        if (!s.ready) return
        _state.update {
            it.copy(
                showHome = true,
                showSearch = false,
                showBookmarks = false,
                resumePage = s.currentPage,
                resumeLabel = "Page ${s.currentPage} · ${s.pageLabel.ifBlank { repository.labelForPage(s.currentPage) }}"
            )
        }
    }

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

/** Prefer the saved line highlight page; otherwise the last scrolled page. */
internal fun resumeReaderPage(
    lastPage: Int,
    highlight: HighlightPoint?,
    pageCount: Int
): Int = normalizeReaderPage(highlight?.page ?: lastPage, pageCount)
