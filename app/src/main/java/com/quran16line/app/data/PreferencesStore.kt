package com.quran16line.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "quran_prefs")

class PreferencesStore(private val context: Context) {
    private val gson = Gson()

    private val lastPageKey = intPreferencesKey("last_page")
    private val highlightPageKey = intPreferencesKey("highlight_page")
    private val highlightLineKey = intPreferencesKey("highlight_line")
    private val bookmarksKey = stringPreferencesKey("bookmarks_json")

    val lastPage: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[lastPageKey] ?: 2
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

    suspend fun setLastPage(page: Int) {
        context.dataStore.edit { it[lastPageKey] = page }
    }

    suspend fun setHighlight(point: HighlightPoint?) {
        context.dataStore.edit { prefs ->
            if (point == null) {
                prefs.remove(highlightPageKey)
                prefs.remove(highlightLineKey)
            } else {
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
}
