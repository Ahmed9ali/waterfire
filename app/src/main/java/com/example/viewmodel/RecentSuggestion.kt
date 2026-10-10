package com.example.viewmodel

import android.net.Uri
import com.example.data.database.AppDatabase
import kotlinx.coroutines.flow.first

/**
 * Recent searches and visited pages, newest first, shown when the address bar is
 * focused with no text (like Brave).
 *
 * - DuckDuckGo result pages become a "recent search" (just the query text).
 * - Every other page becomes a "recent visit" (title + url).
 * Duplicates are removed, so a page visited ten times appears once.
 */
suspend fun loadRecentSuggestions(database: AppDatabase, limit: Int = 15): List<SearchSuggestion> {
    val history = try {
        database.historyDao().getAllHistory().first()
    } catch (e: Exception) {
        emptyList()
    }

    val result = ArrayList<SearchSuggestion>()
    val seen = HashSet<String>()

    for (item in history.sortedByDescending { it.visitedAt }) {
        if (item.url.isBlank() || item.url.startsWith("about:")) continue

        val uri = try { Uri.parse(item.url) } catch (e: Exception) { null }
        val host = uri?.host?.lowercase() ?: ""
        val query = if (host.endsWith("duckduckgo.com")) {
            try { uri?.getQueryParameter("q") } catch (e: Exception) { null }
        } else {
            null
        }

        val suggestion = if (!query.isNullOrBlank()) {
            SearchSuggestion(title = query.trim(), isRecent = true)
        } else {
            SearchSuggestion(
                title = item.title.ifBlank { item.url },
                url = item.url,
                isBookmarkOrHistory = true,
                isRecent = true
            )
        }

        val key = suggestion.url ?: ("q:" + suggestion.title.lowercase())
        if (seen.add(key)) {
            result.add(suggestion)
        }
        if (result.size >= limit) break
    }

    return result
}
