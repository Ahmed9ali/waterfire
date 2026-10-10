package com.example.browser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object DuckDuckGoApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    fun buildSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return "https://duckduckgo.com/?q=$encoded"
    }

    suspend fun getAutocompleteSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://duckduckgo.com/ac/?q=$encodedQuery&type=list"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()

                val rootArray = JSONArray(body)
                if (rootArray.length() >= 2) {
                    val suggestionsArray = rootArray.getJSONArray(1)
                    val suggestions = mutableListOf<String>()
                    for (i in 0 until suggestionsArray.length()) {
                        val suggestion = suggestionsArray.optString(i)
                        if (!suggestion.isNullOrBlank()) {
                            suggestions.add(suggestion)
                        }
                    }
                    return@withContext suggestions
                }
            }
        } catch (e: Exception) {
            // Network issue or offline
        }
        emptyList()
    }
}
