package com.example.automation

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val settings: StateFlow<AutomationSettings> =
        repository.settingsFlow.stateIn(scope, SharingStarted.Eagerly, AutomationSettings())
    val datasets: StateFlow<List<Dataset>> =
        repository.datasetsFlow.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val records: StateFlow<List<DatasetRecord>> =
        repository.recordsFlow.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** When true: if a form asks for email or username but the record has none,
     *  the phone value is used as a fallback. Toggle from Automation screen. */
    val fallbackToPhone = MutableStateFlow(true)

    val currentFormDetection = MutableStateFlow<FormDetectionInfo?>(null)
    var pendingUsedRecord: DatasetRecord? = null

    private val pageRecordCache = mutableMapOf<String, DatasetRecord>()
    private val sessionUsedRandomIds = mutableSetOf<String>()

    // ---------------- Import helpers ----------------

    fun analyzeContent(content: String, hint: ContentFormat? = null): SmartAnalysisResult =
        AutomationFileAnalyzer.analyzeSmart(content, hint)

    fun analyzeBytes(bytes: ByteArray): SmartAnalysisResult {
        val text = AutomationFileAnalyzer.detectEncodingAndRead(bytes)
        return if (text.isBlank()) SmartAnalysisResult(
            format = ContentFormat.UNKNOWN,
            formatConfidence = 0f,
            columns = emptyList(),
            records = emptyList(),
            totalLines = 0,
            validRecordCount = 0,
            skippedLinesCount = 0,
            needsUserInput = true,
            userPrompt = "This file looks binary or is too large to read as text."
        ) else AutomationFileAnalyzer.analyzeSmart(text)
    }

    suspend fun saveImportedDataset(
        name: String,
        sourceFileName: String,
        columns: List<DetectedColumn>,
        rows: List<Map<String, String>>,
        usageMode: UsageMode = UsageMode.SEQUENTIAL_TOP
    ): Dataset {
        val dataset = Dataset(
            name = name.ifBlank { "Untitled dataset" },
            sourceFileName = sourceFileName,
            usageMode = usageMode
        )
        val activeCols = columns.filter { !it.isIgnored }
        val typeByRaw = activeCols.associate { it.rawKey to it.normalizedType }

        val mappedRows: List<Map<String, String>> = rows.map { row ->
            val out = mutableMapOf<String, String>()
            for ((rawKey, value) in row) {
                val type = typeByRaw[rawKey] ?: rawKey
                if (value.isNotBlank()) out[type] = value
            }
            AutomationFileAnalyzer.deriveMissingFields(out)
            out
        }.filter { it.isNotEmpty() }

        val recordEntities = mappedRows.mapIndexed { i, fields ->
            DatasetRecord(datasetId = dataset.id, fields = fields, orderIndex = i)
        }

        repository.saveDatasetWithRecords(dataset, recordEntities)
        repository.setActiveDataset(dataset.id)
        return dataset
    }

    // ---------------- Fill helpers ----------------

    fun getActiveDataset(): Dataset? =
        datasets.value.firstOrNull { it.isActive } ?: datasets.value.firstOrNull()

    fun getRecordsForDataset(datasetId: String): List<DatasetRecord> =
        records.value.filter { it.datasetId == datasetId }.sortedBy { it.orderIndex }

    fun getCachedRecordForUrl(url: String) = pageRecordCache[url]

    fun clearCachedRecordForUrl(url: String) {
        pageRecordCache.remove(url)
    }

    fun clearAllPageRecordCaches() {
        pageRecordCache.clear()
        sessionUsedRandomIds.clear()
    }

    fun getNextRecordToFill(
        dataset: Dataset,
        pageUrl: String,
        forceNext: Boolean = false
    ): Pair<DatasetRecord?, String?> {
        val all = getRecordsForDataset(dataset.id)
        if (all.isEmpty()) return null to "Dataset is empty"

        if (!forceNext && pageUrl.isNotBlank()) {
            pageRecordCache[pageUrl]?.let { cached ->
                all.firstOrNull { it.id == cached.id }?.let { return it to null }
            }
        }

        val available = if (dataset.skipAlreadyUsed) all.filter { !it.isUsed } else all
        if (available.isEmpty()) {
            return if (dataset.loopBackToStart) {
                scope.launch { repository.markAllRecordsUnused(dataset.id) }
                val first = all.firstOrNull()
                if (first != null && pageUrl.isNotBlank()) pageRecordCache[pageUrl] = first
                first to "Dataset exhausted. Looped back to start."
            } else {
                null to "All records used. Reset or pick another dataset."
            }
        }

        val chosen = when (dataset.usageMode) {
            UsageMode.SEQUENTIAL_TOP ->
                available[dataset.currentIndex.coerceIn(0, available.size - 1)]
            UsageMode.SEQUENTIAL_BOTTOM ->
                available[(available.size - 1 - dataset.currentIndex).coerceIn(0, available.size - 1)]
            UsageMode.RANDOM ->
                available[Random.nextInt(available.size)]
            UsageMode.RANDOM_WITHOUT_REPEAT -> {
                val pool = available
                    .filter { !sessionUsedRandomIds.contains(it.id) }
                    .ifEmpty { sessionUsedRandomIds.clear(); available }
                pool[Random.nextInt(pool.size)].also { sessionUsedRandomIds.add(it.id) }
            }
            UsageMode.MANUAL -> available.firstOrNull()
        }
        if (chosen != null && pageUrl.isNotBlank()) pageRecordCache[pageUrl] = chosen
        return chosen to null
    }

    fun recordUsed(dataset: Dataset, record: DatasetRecord) {
        scope.launch(Dispatchers.IO) {
            if (dataset.deleteAfterUse) repository.deleteRecord(record.id)
            else repository.updateRecord(record.copy(isUsed = true))
            repository.updatePointer(dataset.id, dataset.currentIndex + 1)
        }
    }

    fun shouldWarnHttpPassword(url: String, record: DatasetRecord): Boolean {
        val http = url.startsWith("http://", true)
        val hasPw = record.fields.containsKey("password") ||
                record.fields.containsKey("confirm_password")
        return http && hasPw
    }

    /**
     * Builds the JS that will run inside the page. When [fallbackToPhone] is true
     * (or the manager's fallbackToPhone StateFlow is true), email/username
     * fields that are missing from the record are set to the phone value,
     * so any form field named email/username receives the phone number.
     *
     * The JS side should also handle <select> elements by matching the value
     * against option text (case-insensitive) — that covers country/state/etc.
     * dropdowns in the page.
     */
    fun buildFillScript(
        fields: Map<String, String>,
        highlight: Boolean,
        autoSubmit: Boolean,
        usePhoneFallback: Boolean = fallbackToPhone.value
    ): String {
        val enriched = fields.toMutableMap()
        if (usePhoneFallback) {
            val phone = fields["phone"]?.takeIf { it.isNotBlank() }
            if (phone != null) {
                if (enriched["email"].isNullOrBlank()) enriched["email"] = phone
                if (enriched["username"].isNullOrBlank()) enriched["username"] = phone
            }
        }

        val obj = JSONObject()
        enriched.forEach { (k, v) -> obj.put(k, v) }
        val escaped = obj.toString()
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        return "if (window.__waterfox_fill_fields) { window.__waterfox_fill_fields('$escaped', $highlight, $autoSubmit); }"
    }

    fun buildUndoScript(): String =
        "if (window.__waterfox_undo_fill) { window.__waterfox_undo_fill(); }"
}