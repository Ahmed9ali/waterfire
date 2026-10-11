package com.example.automation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ImportMode(val label: String) { Upload("Upload file"), Paste("Paste text") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationImportScreen(
    manager: AutomationManager,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var mode by remember { mutableStateOf(ImportMode.Upload) }
    var datasetName by remember { mutableStateOf("") }
    var pastedText by remember { mutableStateOf("") }
    var sourceFileName by remember { mutableStateOf("") }
    var analysis by remember { mutableStateOf<SmartAnalysisResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var formatOverride by remember { mutableStateOf<ContentFormat?>(null) }

    // Column edits (user labels + ignored flags)
    val columnEdits = remember { mutableStateListOf<DetectedColumn>() }
    fun syncColumns(result: SmartAnalysisResult?) {
        columnEdits.clear()
        result?.detectedColumns?.let { columnEdits.addAll(it) }
    }

    // ---- File picker ----
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            isAnalyzing = true
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: ByteArray(0)
                val name = queryFileName(context, uri)
                val result = withContext(Dispatchers.Default) { manager.analyzeBytes(bytes) }
                sourceFileName = name
                datasetName = datasetName.ifBlank {
                    name.substringBeforeLast('.').ifBlank { result.suggestedDatasetName ?: "Imported dataset" }
                }
                analysis = result
                formatOverride = null
                syncColumns(result)
            } catch (e: Exception) {
                snackbar.showSnackbar("Failed to read file: ${e.message}")
            } finally {
                isAnalyzing = false
            }
        }
    }

    fun runAnalysis(hint: ContentFormat? = formatOverride) {
        if (pastedText.isBlank()) {
            scope.launch { snackbar.showSnackbar("Paste some text first.") }
            return
        }
        isAnalyzing = true
        scope.launch {
            val result = withContext(Dispatchers.Default) { manager.analyzeContent(pastedText, hint) }
            analysis = result
            if (datasetName.isBlank()) datasetName = result.suggestedDatasetName ?: ""
            syncColumns(result)
            isAnalyzing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import dataset", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            val enabled = analysis != null && columnEdits.any { !it.isIgnored } && !isSaving
            ExtendedFloatingActionButton(
                onClick = {
                    val result = analysis ?: return@ExtendedFloatingActionButton
                    scope.launch {
                        isSaving = true
                        try {
                            val ds = manager.saveImportedDataset(
                                name = datasetName.ifBlank { "Untitled dataset" },
                                sourceFileName = sourceFileName,
                                columns = columnEdits.toList(),
                                rows = result.records
                            )
                            snackbar.showSnackbar("Saved \"${ds.name}\" with ${result.records.size} records.")
                            onDone()
                        } catch (e: Exception) {
                            snackbar.showSnackbar("Save failed: ${e.message}")
                        } finally {
                            isSaving = false
                        }
                    }
                },
                icon = {
                    if (isSaving) CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    ) else Icon(Icons.Filled.Save, contentDescription = null)
                },
                text = { Text(if (isSaving) "Saving…" else "Save dataset") },
                expanded = enabled
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ---------- Mode selector ----------
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ImportMode.entries.forEachIndexed { index, m ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, ImportMode.entries.size),
                        selected = mode == m,
                        onClick = { mode = m },
                        icon = {
                            Icon(
                                when (m) {
                                    ImportMode.Upload -> Icons.Filled.FileUpload
                                    ImportMode.Paste -> Icons.Filled.ContentPaste
                                },
                                contentDescription = null
                            )
                        }
                    ) { Text(m.label) }
                }
            }

            // ---------- Dataset name ----------
            OutlinedTextField(
                value = datasetName,
                onValueChange = { datasetName = it },
                label = { Text("Dataset name") },
                placeholder = { Text("e.g. Customers batch 01") },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ---------- Mode content ----------
            when (mode) {
                ImportMode.Upload -> {
                    ElevatedButton(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Choose file")
                    }
                    if (sourceFileName.isNotBlank()) {
                        AssistChip(
                            onClick = { },
                            label = { Text(sourceFileName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
                ImportMode.Paste -> {
                    OutlinedTextField(
                        value = pastedText,
                        onValueChange = { pastedText = it },
                        label = { Text("Paste any text") },
                        placeholder = { Text("Paste CSV, JSON, HTML, key-value, or free text…") },
                        minLines = 6,
                        maxLines = 12,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { runAnalysis() },
                            enabled = !isAnalyzing,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isAnalyzing) CircularProgressIndicator(
                                modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                            ) else Text("Analyse")
                        }
                        OutlinedButton(
                            onClick = {
                                pastedText = ""
                                analysis = null
                                columnEdits.clear()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Clear") }
                    }
                }
            }

            // ---------- Analysis result ----------
            AnimatedVisibility(visible = analysis != null) {
                val result = analysis ?: return@AnimatedVisibility
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Format card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Detected format", style = MaterialTheme.typography.labelMedium)
                            Text(
                                result.format.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Confidence ${(result.formatConfidence * 100).toInt()}% • " +
                                        "${result.validRecordCount} records • ${result.detectedColumns.size} columns" +
                                        if (result.skippedLinesCount > 0) " • skipped ${result.skippedLinesCount}" else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (result.needsUserInput) {
                                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                Text(
                                    result.userPrompt ?: "Please confirm the format or rename fields below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Format override chips
                    Text("Force format (optional)", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        ContentFormat.entries
                            .filter { it != ContentFormat.UNKNOWN }
                            .forEach { f ->
                                FilterChip(
                                    selected = formatOverride == f,
                                    onClick = {
                                        formatOverride = if (formatOverride == f) null else f
                                        runAnalysis(formatOverride)
                                    },
                                    label = { Text(f.displayName) }
                                )
                            }
                    }

                    // Columns
                    Text("Fields", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (columnEdits.isEmpty()) {
                        Text(
                            "No fields detected. You can still save or paste different content.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                columnEdits.forEachIndexed { idx, col ->
                                    ColumnEditRow(
                                        column = col,
                                        onChangeLabel = { newLabel ->
                                            columnEdits[idx] = col.copy(userLabel = newLabel)
                                        },
                                        onToggleIgnored = { ignored ->
                                            columnEdits[idx] = col.copy(isIgnored = ignored)
                                        }
                                    )
                                    if (idx < columnEdits.lastIndex) {
                                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Preview
                    if (result.records.isNotEmpty()) {
                        Text("Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val previewRows = result.records.take(5)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((previewRows.size * 64).dp.coerceAtMost(340.dp)),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(previewRows) { i, row ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(
                                            "Record ${i + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        row.entries.take(6).forEach { (k, v) ->
                                            Row(Modifier.fillMaxWidth()) {
                                                Text(
                                                    "$k: ",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    v,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (row.size > 6) {
                                            Text(
                                                "+ ${row.size - 6} more fields",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(96.dp)) // room for FAB
        }
    }
}

@Composable
private fun ColumnEditRow(
    column: DetectedColumn,
    onChangeLabel: (String) -> Unit,
    onToggleIgnored: (Boolean) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(column.userLabel ?: column.displayLabel) }

    ListItem(
        headlineContent = {
            if (editing) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(column.displayLabel, fontWeight = FontWeight.SemiBold)
            }
        },
        supportingContent = {
            Text(
                buildString {
                    append(column.normalizedType)
                    if (column.sampleValues.isNotEmpty()) {
                        append(" • ")
                        append(column.sampleValues.take(2).joinToString(", ").take(60))
                    }
                    append(" • ${(column.confidence * 100).toInt()}%")
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            Icon(Icons.Filled.Check, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (editing) {
                    IconButton(onClick = {
                        onChangeLabel(draft.trim())
                        editing = false
                    }) { Icon(Icons.Filled.Save, contentDescription = "Save name") }
                    IconButton(onClick = {
                        draft = column.userLabel ?: column.displayLabel
                        editing = false
                    }) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
                } else {
                    IconButton(onClick = { editing = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename")
                    }
                    Switch(
                        checked = !column.isIgnored,
                        onCheckedChange = { onToggleIgnored(!it) }
                    )
                }
            }
        }
    )
}

private fun queryFileName(context: Context, uri: Uri): String {
    var name = ""
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) name = c.getString(idx) ?: ""
        }
    } catch (_: Exception) {}
    return name.ifBlank { uri.lastPathSegment?.substringAfterLast('/') ?: "imported.txt" }
}