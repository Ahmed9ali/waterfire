package com.example.browser

import android.content.Context
import com.example.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class TrackerBlocker(context: Context) {

    // Fast O(1) HashSet for blocked domains
    private val blockedDomains: HashSet<String> = HashSet(2500)
    private val pageBlockedTrackerCache = ConcurrentHashMap<String, MutableSet<String>>()

    init {
        try {
            val inputStream = context.resources.openRawResource(R.raw.tracker_domains)
            BufferedReader(InputStreamReader(inputStream), 8192).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim()?.lowercase()
                    if (!trimmed.isNullOrEmpty() && !trimmed.startsWith("#")) {
                        blockedDomains.add(trimmed)
                    }
                }
            }
        } catch (e: Exception) {
            blockedDomains.addAll(
                listOf(
                    "doubleclick.net", "google-analytics.com", "googlesyndication.com",
                    "criteo.com", "adnxs.com", "facebook.net", "scorecardresearch.com",
                    "taboola.com", "outbrain.com", "quantserve.com", "rubiconproject.com"
                )
            )
        }
    }

    /**
     * Fast O(1) lookup without Regex or disk access.
     * Returns the matched tracker root domain or null if allowed.
     */
    fun isTracker(requestUrl: String, mainPageUrl: String): String? {
        val requestHost = extractHost(requestUrl) ?: return null
        val pageHost = extractHost(mainPageUrl)

        // Never block first-party requests
        if (pageHost != null && (requestHost == pageHost || requestHost.endsWith(".$pageHost"))) {
            return null
        }

        // Check host and parent domains with simple O(1) HashSet lookups
        var currentHost = requestHost
        while (true) {
            if (blockedDomains.contains(currentHost)) {
                recordBlocked(mainPageUrl, currentHost)
                return currentHost
            }
            val dotIndex = currentHost.indexOf('.')
            if (dotIndex != -1 && dotIndex < currentHost.length - 1) {
                currentHost = currentHost.substring(dotIndex + 1)
            } else {
                break
            }
        }
        return null
    }

    private fun extractHost(url: String): String? {
        if (url.isEmpty()) return null
        val schemeEnd = url.indexOf("://")
        val start = if (schemeEnd != -1) schemeEnd + 3 else 0
        if (start >= url.length) return null

        var end = url.indexOf('/', start)
        if (end == -1) end = url.length
        val colon = url.indexOf(':', start)
        if (colon != -1 && colon < end) end = colon
        val at = url.lastIndexOf('@', end)
        val hostStart = if (at != -1 && at > start) at + 1 else start

        if (hostStart >= end) return null
        return url.substring(hostStart, end).lowercase()
    }

    private fun recordBlocked(mainPageUrl: String, trackerDomain: String) {
        if (mainPageUrl.isBlank()) return
        val set = pageBlockedTrackerCache.computeIfAbsent(mainPageUrl) { ConcurrentHashMap.newKeySet() }
        set.add(trackerDomain)
    }

    fun getBlockedTrackersForPage(mainPageUrl: String): List<String> {
        return pageBlockedTrackerCache[mainPageUrl]?.toList() ?: emptyList()
    }

    fun clearPageCache(mainPageUrl: String) {
        pageBlockedTrackerCache.remove(mainPageUrl)
    }

    fun clearAllCache() {
        pageBlockedTrackerCache.clear()
    }
}
