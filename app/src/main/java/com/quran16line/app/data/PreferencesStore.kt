package com.quran16line.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "quran_prefs")

class PreferencesStore(private val context: Context) {
    private val gson = Gson()

    private val lastPageKey = intPreferencesKey("last_page")
    private val highlightPageKey = intPreferencesKey("highlight_page")
    private val highlightLineKey = intPreferencesKey("highlight_line")
    private val bookmarksKey = stringPreferencesKey("bookmarks_json")
    private val indexVersionKey = intPreferencesKey("page_index_version")

    val lastPage: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[lastPageKey] ?: PdfMushafSource.DEFAULT_START_PAGE
    }

    val highlight: Flow<HighlightPoint?> = context.dataStore.data.map { prefs ->
        val page = prefs[highlightPageKey]
        val line = prefs[highlightLineKey]
        if (page != null && line != null) HighlightPoint(page, line) else null
    }

    val bookmarks: Flow<List<Bookmark>> = context.dataStore.data.map { prefs ->
        val raw = prefs[bookmarksKey] ?: "[]"
        val type = object : TypeToken<List<Bookmark>>() {}.type
        gson.fromJson<List<Bookmark>>(raw, type) ?: emptyList()
    }

    /**
     * Migrates saved positions after the decorative cover was removed from reader numbering
     * (old page N → new page N-1). Safe to call on every launch.
     */
    suspend fun migratePageIndexIfNeeded() {
        context.dataStore.edit { prefs ->
            val version = prefs[indexVersionKey] ?: 1
            if (version >= PAGE_INDEX_VERSION) return@edit

            prefs[lastPageKey]?.let { old ->
                prefs[lastPageKey] = (old - 1).coerceAtLeast(1)
            }
            prefs[highlightPageKey]?.let { old ->
                prefs[highlightPageKey] = (old - 1).coerceAtLeast(1)
            }
            val raw = prefs[bookmarksKey]
            if (raw != null) {
                val type = object : TypeToken<List<Bookmark>>() {}.type
                val list = gson.fromJson<List<Bookmark>>(raw, type).orEmpty().map { bookmark ->
                    bookmark.copy(page = (bookmark.page - 1).coerceAtLeast(1))
                }
                prefs[bookmarksKey] = gson.toJson(list)
            }
            prefs[indexVersionKey] = PAGE_INDEX_VERSION
        }
        // Touch flow so collectors see migrated values.
        context.dataStore.data.first()
    }

    suspend fun setLastPage(page: Int) {
        context.dataStore.edit { it[lastPageKey] = page }
    }

    /**
     * Atomically save the reading mark: page to reopen + optional line highlight.
     * Highlight is the "left off here" marker for the next cold start.
     */
    suspend fun saveReadingMark(page: Int, highlight: HighlightPoint?) {
        context.dataStore.edit { prefs ->
            prefs[lastPageKey] = page
            if (highlight == null) {
                prefs.remove(highlightPageKey)
                prefs.remove(highlightLineKey)
            } else {
                prefs[highlightPageKey] = highlight.page
                prefs[highlightLineKey] = highlight.lineIndex
            }
        }
    }

    suspend fun setHighlight(point: HighlightPoint?) {
        context.dataStore.edit { prefs ->
            if (point == null) {
                prefs.remove(highlightPageKey)
                prefs.remove(highlightLineKey)
            } else {
                prefs[lastPageKey] = point.page
                prefs[highlightPageKey] = point.page
                prefs[highlightLineKey] = point.lineIndex
            }
        }
    }

    suspend fun toggleBookmark(bookmark: Bookmark): Boolean {
        var added = false
        context.dataStore.edit { prefs ->
            val current = gson.fromJson<List<Bookmark>>(
                prefs[bookmarksKey] ?: "[]",
                object : TypeToken<List<Bookmark>>() {}.type
            ).orEmpty().toMutableList()
            val existing = current.indexOfFirst { it.page == bookmark.page }
            if (existing >= 0) {
                current.removeAt(existing)
                added = false
            } else {
                current.add(0, bookmark)
                added = true
            }
            prefs[bookmarksKey] = gson.toJson(current)
        }
        return added
    }

    companion object {
        /** Bumped when reader page numbering changes (cover removed). */
        const val PAGE_INDEX_VERSION = 2
    }
}
