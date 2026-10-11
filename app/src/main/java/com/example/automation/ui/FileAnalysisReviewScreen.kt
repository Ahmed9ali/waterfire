package com.example.automation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.automation.AutomationFileAnalyzer
import com.example.automation.ContentFormat
import com.example.automation.DetectedColumn
import com.example.automation.SmartAnalysisResult
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileAnalysisReviewScreen(
    fileName: String,
    analysisResult: SmartAnalysisResult,
    onCancel: () -> Unit,
    onSwitchToManual: () -> Unit,
    onConfirmSave: (
        name: String,
        records: List<Map<String, String>>,
        columns: List<DetectedColumn>
    ) -> Unit
) {
    BackHandler { onCancel() }

    var datasetName by remember {
        mutableStateOf(
            fileName.substringBeforeLast('.')
                .ifBlank { analysisResult.suggestedName ?: "New dataset" }
        )
    }
    val labels = remember {
        mutableStateMapOf<String, String>().apply {
            analysisResult.columns.forEach { put(it.rawKey, it.normalizedType) }
        }
    }
    val ignored = remember { mutableStateMapOf<String, Boolean>() }
    var formatOverride by remember { mutableStateOf(analysisResult.format) }

    val commonTypes = listOf(
        "full_name", "first_name", "last_name", "username", "email", "password",
        "phone", "website", "address_line_1", "city", "state", "zip_code",
        "country", "date_of_birth", "message", "subject", "custom"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Review & map", fontWeight = FontWeight.Bold)
                        Text(
                            "${analysisResult.validRecordCount} records • ${analysisResult.columns.size} fields",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandOrange
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSwitchToManual) {
                        Text("Manual", color = BrandOrange)
                    }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ---------- Dataset name ----------
                item {
                    OutlinedTextField(
                        value = datasetName,
                        onValueChange = { datasetName = it },
                        label = { Text("Dataset name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---------- Format banner ----------
                item {
                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Detected format", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${(analysisResult.formatConfidence * 100).toInt()}%",
                                    color = if (analysisResult.formatConfidence > 0.7f) SuccessGreen else BrandOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                formatOverride.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (analysisResult.needsUserInput) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    analysisResult.userPrompt ?: "Confirm the format below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // ---------- Format override chips ----------
                item {
                    Text(
                        "Change format",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
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
                                    onClick = { formatOverride = f },
                                    label = { Text(f.displayName) }
                                )
                            }
                    }
                }

                // ---------- Fields header ----------
                item {
                    Text(
                        "Fields",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange
                    )
                }

                if (analysisResult.columns.isEmpty()) {
                    item {
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("No fields detected.", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "You can still save it, or tap \"Manual\" above to enter records by hand.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        analysisResult.columns,
                        key = { _, c -> c.rawKey }
                    ) { _, col ->
                        FieldMapCard(
                            column = col,
                            label = labels[col.rawKey] ?: col.normalizedType,
                            ignored = ignored[col.rawKey] == true,
                            commonTypes = commonTypes,
                            onChangeLabel = { labels[col.rawKey] = it },
                            onToggleIgnore = { ignored[col.rawKey] = it }
                        )
                    }
                }

                // ---------- Preview ----------
                if (analysisResult.records.isNotEmpty()) {
                    item {
                        Text(
                            "Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandOrange
                        )
                    }
                    itemsIndexed(analysisResult.records.take(5)) { i, rec ->
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "Record ${i + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandOrange,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                rec.entries.take(6).forEach { (k, v) ->
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
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                if (rec.size > 6) {
                                    Text(
                                        "+ ${rec.size - 6} more fields",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---------- Save bar ----------
            Column {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            val mapped = analysisResult.records.map { raw ->
                                val out = mutableMapOf<String, String>()
                                raw.forEach { (k, v) ->
                                    if (ignored[k] != true) {
                                        val target = labels[k] ?: k
                                        if (v.isNotBlank()) out[target] = v
                                    }
                                }
                                AutomationFileAnalyzer.deriveMissingFields(out)
                                out
                            }
                            val newColumns = analysisResult.columns.map { c ->
                                c.copy(
                                    normalizedType = labels[c.rawKey] ?: c.normalizedType,
                                    isIgnored = ignored[c.rawKey] == true,
                                    userLabel = labels[c.rawKey]
                                )
                            }
                            onConfirmSave(
                                datasetName.ifBlank { "New dataset" },
                                mapped,
                                newColumns
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                        modifier = Modifier.weight(1f)
                    ) { Text("Save dataset", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun FieldMapCard(
    column: DetectedColumn,
    label: String,
    ignored: Boolean,
    commonTypes: List<String>,
    onChangeLabel: (String) -> Unit,
    onToggleIgnore: (Boolean) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(label) { mutableStateOf(label) }
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ignored)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = {
                    if (editing) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column {
                            Text(
                                label.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "\"${column.rawKey}\" • ${(column.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                supportingContent = {
                    column.sampleValues.firstOrNull()?.let { s ->
                        Text(
                            "e.g. $s",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (editing) {
                            IconButton(onClick = {
                                if (draft.isNotBlank()) onChangeLabel(draft.trim())
                                editing = false
                            }) {
                                Icon(painterResource(R.drawable.ic_check), contentDescription = "Save")
                            }
                            IconButton(onClick = {
                                draft = label
                                editing = false
                            }) {
                                Icon(painterResource(R.drawable.ic_close), contentDescription = "Cancel")
                            }
                        } else {
                            IconButton(onClick = { editing = true }) {
                                Icon(painterResource(R.drawable.ic_edit), contentDescription = "Rename")
                            }
                            Switch(
                                checked = !ignored,
                                onCheckedChange = { onToggleIgnore(!it) }
                            )
                        }
                    }
                }
            )
            if (!editing) {
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable { expandedMenu = !expandedMenu },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Quick map:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandOrange,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (expandedMenu) "▲" else "▼",
                        color = BrandOrange,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (expandedMenu) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        commonTypes.chunked(3).forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                row.forEach { t ->
                                    AssistChip(
                                        onClick = {
                                            onChangeLabel(t)
                                            expandedMenu = false
                                        },
                                        label = {
                                            Text(t, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}