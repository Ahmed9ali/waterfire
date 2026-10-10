package com.example.automation

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random

data class FormDetectionInfo(
    val url: String = "",
    val detectedFieldCount: Int = 0
)

class AutomationManager(
    private val context: Context,
    val repository: AutomationRepository,
    private val scope: CoroutineScope
) {
    val settings: StateFlow<AutomationSettings> = repository.settingsFlow.stateIn(scope, SharingStarted.Eagerly, AutomationSettings())
    val datasets: StateFlow<List<Dataset>> = repository.datasetsFlow.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val records: StateFlow<List<DatasetRecord>> = repository.recordsFlow.stateIn(scope, SharingStarted.Eagerly, emptyList())

    // Active detection state for current page
    val currentFormDetection = MutableStateFlow<FormDetectionInfo?>(null)

    // Last filled record to mark used on submit
    var pendingUsedRecord: DatasetRecord? = null

    // Cache assigned record per page URL so multi-step forms use the same record until page navigates or "next record"
    private val pageRecordCache = mutableMapOf<String, DatasetRecord>()

    // Track used record IDs in current session for RANDOM_WITHOUT_REPEAT
    private val sessionUsedRandomIds = mutableSetOf<String>()

    fun getActiveDataset(): Dataset? {
        return datasets.value.firstOrNull { it.isActive } ?: datasets.value.firstOrNull()
    }

    fun getRecordsForDataset(datasetId: String): List<DatasetRecord> {
        return records.value.filter { it.datasetId == datasetId }.sortedBy { it.orderIndex }
    }

    fun getCachedRecordForUrl(url: String): DatasetRecord? {
        return pageRecordCache[url]
    }

    fun clearCachedRecordForUrl(url: String) {
        pageRecordCache.remove(url)
    }

    fun clearAllPageRecordCaches() {
        pageRecordCache.clear()
        sessionUsedRandomIds.clear()
    }

    fun getNextRecordToFill(dataset: Dataset, pageUrl: String, forceNext: Boolean = false): Pair<DatasetRecord?, String?> {
        val allRecords = getRecordsForDataset(dataset.id)
        if (allRecords.isEmpty()) {
            return Pair(null, "Dataset is empty")
        }

        // Check if page already has an assigned record and we are not forcing next
        if (!forceNext && pageUrl.isNotBlank()) {
            val cached = pageRecordCache[pageUrl]
            if (cached != null) {
                // Ensure cached record still exists in dataset
                val stillExists = allRecords.firstOrNull { it.id == cached.id }
                if (stillExists != null) {
                    return Pair(stillExists, null)
                }
            }
        }

        val available = if (dataset.skipAlreadyUsed) {
            allRecords.filter { !it.isUsed }
        } else {
            allRecords
        }

        if (available.isEmpty()) {
            return if (dataset.loopBackToStart) {
                scope.launch { repository.markAllRecordsUnused(dataset.id) }
                val first = allRecords.firstOrNull()
                if (first != null && pageUrl.isNotBlank()) {
                    pageRecordCache[pageUrl] = first
                }
                Pair(first, "Dataset exhausted. Looped back to start.")
            } else {
                Pair(null, "All records have been used. Please reset or choose another dataset.")
            }
        }

        val chosenRecord: DatasetRecord? = when (dataset.usageMode) {
            UsageMode.SEQUENTIAL_TOP -> {
                val idx = dataset.currentIndex.coerceIn(0, available.size - 1)
                available[idx]
            }
            UsageMode.SEQUENTIAL_BOTTOM -> {
                val idx = (available.size - 1 - dataset.currentIndex).coerceIn(0, available.size - 1)
                available[idx]
            }
            UsageMode.RANDOM -> {
                available[Random.nextInt(available.size)]
            }
            UsageMode.RANDOM_WITHOUT_REPEAT -> {
                val unpickedInSession = available.filter { !sessionUsedRandomIds.contains(it.id) }
                val pool = if (unpickedInSession.isNotEmpty()) unpickedInSession else {
                    sessionUsedRandomIds.clear()
                    available
                }
                val picked = pool[Random.nextInt(pool.size)]
                sessionUsedRandomIds.add(picked.id)
                picked
            }
            UsageMode.MANUAL -> {
                available.firstOrNull()
            }
        }

        if (chosenRecord != null && pageUrl.isNotBlank()) {
            pageRecordCache[pageUrl] = chosenRecord
        }

        return Pair(chosenRecord, null)
    }

    fun recordUsed(dataset: Dataset, record: DatasetRecord) {
        scope.launch(Dispatchers.IO) {
            if (dataset.deleteAfterUse) {
                repository.deleteRecord(record.id)
            } else {
                repository.updateRecord(record.copy(isUsed = true))
            }
            val nextIdx = (dataset.currentIndex + 1)
            repository.updatePointer(dataset.id, nextIdx)
        }
    }

    fun shouldWarnHttpPassword(url: String, record: DatasetRecord): Boolean {
        val isHttp = url.startsWith("http://", ignoreCase = true)
        val hasPassword = record.fields.containsKey("password") || record.fields.containsKey("confirm_password")
        return isHttp && hasPassword
    }

    fun buildFillScript(fields: Map<String, String>, highlight: Boolean, autoSubmit: Boolean): String {
        val jsonObj = JSONObject()
        fields.forEach { (k, v) -> jsonObj.put(k, v) }
        val rawJson = jsonObj.toString()
        // Escape for JS evaluation string literal safely
        val escaped = rawJson
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        return "if (window.__waterfox_fill_fields) { window.__waterfox_fill_fields('$escaped', $highlight, $autoSubmit); }"
    }

    fun buildUndoScript(): String {
        return "if (window.__waterfox_undo_fill) { window.__waterfox_undo_fill(); }"
    }
}
