package com.example.automation

import java.util.UUID

enum class UsageMode(val displayName: String) {
    SEQUENTIAL_TOP("From top (sequential)"),
    SEQUENTIAL_BOTTOM("From bottom (reverse)"),
    RANDOM("Random"),
    RANDOM_WITHOUT_REPEAT("Random without repeat"),
    MANUAL("Manual (pick each time)")
}

enum class ContentFormat(val displayName: String) {
    JSON("JSON"),
    NDJSON("JSON Lines"),
    KEY_VALUE("Key-Value"),
    DELIMITED("CSV / Delimited"),
    HTML_TABLE("HTML Table"),
    XML("XML"),
    FREE_TEXT("Free Text"),
    UNKNOWN("Unknown")
}

data class AutomationSettings(
    val detectionEnabled: Boolean = true,
    val showOmnibarIcon: Boolean = true,
    val highlightFilledFields: Boolean = true,
    val autoSubmitAfterFilling: Boolean = false,
    val disabledSites: Set<String> = emptySet(),
    val biometricLockEnabled: Boolean = false
)

data class Dataset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sourceFileName: String,
    val usageMode: UsageMode = UsageMode.SEQUENTIAL_TOP,
    val skipAlreadyUsed: Boolean = false,
    val deleteAfterUse: Boolean = false,
    val loopBackToStart: Boolean = true,
    val markAsUsedOnSubmit: Boolean = false,
    val currentIndex: Int = 0,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class DatasetRecord(
    val id: String = UUID.randomUUID().toString(),
    val datasetId: String,
    val fields: Map<String, String>,
    val isUsed: Boolean = false,
    val orderIndex: Int = 0
)

data class DetectedColumn(
    val rawKey: String,
    val normalizedType: String,
    val sampleValues: List<String> = emptyList(),
    val confidence: Float = 1.0f,
    val isIgnored: Boolean = false,
    val userLabel: String? = null
)

/** User-facing label helper for a column. */
val DetectedColumn.displayLabel: String
    get() = (userLabel?.takeIf { it.isNotBlank() }
        ?: normalizedType.replace('_', ' ')
            .split(' ')
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })

data class FileAnalysisResult(
    val detectedColumns: List<DetectedColumn>,
    val records: List<Map<String, String>>,
    val totalLines: Int,
    val validRecordCount: Int,
    val skippedLinesCount: Int
)

data class SmartAnalysisResult(
    val format: ContentFormat,
    val formatConfidence: Float,
    val detectedColumns: List<DetectedColumn>,
    val records: List<Map<String, String>>,
    val totalLines: Int,
    val validRecordCount: Int,
    val skippedLinesCount: Int,
    val needsUserInput: Boolean = false,
    val userPrompt: String? = null,
    val suggestedDatasetName: String? = null
) {
    fun toFileAnalysisResult() = FileAnalysisResult(
        detectedColumns = detectedColumns,
        records = records,
        totalLines = totalLines,
        validRecordCount = validRecordCount,
        skippedLinesCount = skippedLinesCount
    )
}