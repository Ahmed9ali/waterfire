package com.example.browser

import android.content.Context
import android.net.Uri
import com.example.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class TrackerBlocker(context: Context) {

    private val blockedDomains: Set<String>
    private val pageBlockedTrackerCache = ConcurrentHashMap<String, MutableSet<String>>()

    init {
        val domains = mutableSetOf<String>()
        try {
            val inputStream = context.resources.openRawResource(R.raw.tracker_domains)
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim()?.lowercase()
                    if (!trimmed.isNullOrEmpty() && !trimmed.startsWith("#")) {
                        domains.add(trimmed)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback base trackers
            domains.addAll(
                listOf(
                    "doubleclick.net", "google-analytics.com", "googlesyndication.com",
                    "criteo.com", "adnxs.com", "facebook.net", "scorecardresearch.com",
                    "taboola.com", "outbrain.com", "quantserve.com", "rubiconproject.com"
                )
            )
        }
        blockedDomains = Collections.unmodifiableSet(domains)
    }

    fun isTracker(requestUrl: String, mainPageUrl: String): String? {
        val requestUri = try { Uri.parse(requestUrl) } catch (e: Exception) { return null }
        val host = requestUri.host?.lowercase() ?: return null

        val pageHost = try { Uri.parse(mainPageUrl).host?.lowercase() } catch (e: Exception) { null }

        // Do not block first-party requests
        if (pageHost != null && (host == pageHost || host.endsWith(".$pageHost"))) {
            return null
        }

        // Check host and all parent domains (e.g. ad.doubleclick.net -> doubleclick.net)
        var currentHost = host
        while (currentHost.contains(".")) {
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

    private fun recordBlocked(mainPageUrl: String, trackerDomain: String) {
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
