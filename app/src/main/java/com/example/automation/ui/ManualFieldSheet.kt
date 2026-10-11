package com.example.automation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.automation.AutomationFileAnalyzer
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.DangerRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualFieldSheet(
    initialName: String = "",
    onCancel: () -> Unit,
    onSave: (
        name: String,
        columns: List<String>,
        records: List<Map<String, String>>
    ) -> Unit
) {
    BackHandler { onCancel() }

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var datasetName by remember { mutableStateOf(initialName) }

    val fields = remember { mutableStateListOf<String>() }
    val rows = remember { mutableStateListOf<SnapshotStateMap<String, String>>() }

    var showAddFieldDialog by remember { mutableStateOf(false) }
    var newFieldDraft by remember { mutableStateOf("") }

    fun sanitizeKey(raw: String): String =
        raw.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    fun addField(raw: String) {
        val k = sanitizeKey(raw)
        if (k.isBlank()) return
        if (fields.contains(k)) {
            scope.launch { snackbar.showSnackbar("Field \"$k\" already exists") }
            return
        }
        fields.add(k)
        rows.forEach { it[k] = "" }
    }

    fun removeField(k: String) {
        fields.remove(k)
        rows.forEach { it.remove(k) }
    }

    fun addRow() {
        val m = mutableStateMapOf<String, String>()
        fields.forEach { m[it] = "" }
        rows.add(m)
    }

    fun removeRow(index: Int) {
        if (index in rows.indices) rows.removeAt(index)
    }

    fun quickPreset() {
        val preset = listOf("full_name", "email", "username", "password", "phone")
        preset.forEach { if (!fields.contains(it)) addField(it) }
        if (rows.isEmpty()) addRow()
    }

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
                        Text("Enter manually", fontWeight = FontWeight.Bold)
                        Text(
                            "${fields.size} field${if (fields.size == 1) "" else "s"} • " +
                                    "${rows.size} record${if (rows.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandOrange
                        )
                    }
                },
                actions = {
                    if (fields.isEmpty()) {
                        TextButton(onClick = { quickPreset() }) {
                            Text("Use preset", color = BrandOrange)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (fields.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { addRow() },
                    containerColor = BrandOrange,
                    contentColor = Color.White,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text("Add record") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ---------- Dataset name ----------
                item {
                    OutlinedTextField(
                        value = datasetName,
                        onValueChange = { datasetName = it },
                        label = { Text("Dataset name") },
                        placeholder = { Text("e.g. My batch 01") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---------- Intro card ----------
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Icon(
                                    painterResource(R.drawable.ic_edit),
                                    contentDescription = null,
                                    tint = BrandOrange
                                )
                            },
                            headlineContent = {
                                Text("How this works", fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Text(
                                    "Define the field names you want, then fill one card per record. " +
                                            "We'll map common names like email, password, phone automatically.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }

                // ---------- Fields header + Add ----------
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Fields",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandOrange,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            newFieldDraft = ""
                            showAddFieldDialog = true
                        }) {
                            Icon(
                                painterResource(R.drawable.ic_add),
                                contentDescription = null,
                                tint = BrandOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Add field", color = BrandOrange)
                        }
                    }
                }

                // ---------- Fields content ----------
                item {
                    if (fields.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("No fields yet.", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Add at least one field to start filling records.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            newFieldDraft = ""
                                            showAddFieldDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                                    ) { Text("Add field") }
                                    TextButton(onClick = { quickPreset() }) {
                                        Text("Use preset", color = BrandOrange)
                                    }
                                }
                            }
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            fields.toList().forEach { f ->
                                FieldChip(name = f, onRemove = { removeField(f) })
                            }
                        }
                    }
                }

                // ---------- Records section ----------
                if (fields.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Records",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandOrange,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                rows.size.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (rows.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("No records yet.", fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Tap \"Add record\" to start filling values.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(rows) { index, row ->
                            ManualRecordCard(
                                index = index,
                                fields = fields.toList(),
                                row = row,
                                canDelete = rows.size > 1,
                                onDelete = { removeRow(index) }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            // ---------- Bottom bar ----------
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        val cleanRecords = rows.map { row ->
                            val m = mutableMapOf<String, String>()
                            fields.forEach { k ->
                                val v = row[k]?.trim().orEmpty()
                                if (v.isNotBlank()) m[k] = v
                            }
                            if (m.isNotEmpty()) AutomationFileAnalyzer.deriveMissingFields(m)
                            m
                        }.filter { it.isNotEmpty() }

                        when {
                            fields.isEmpty() -> scope.launch {
                                snackbar.showSnackbar("Add at least one field.")
                            }
                            cleanRecords.isEmpty() -> scope.launch {
                                snackbar.showSnackbar("Fill in at least one record.")
                            }
                            else -> onSave(
                                datasetName.ifBlank { "Manual dataset" },
                                fields.toList(),
                                cleanRecords
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Save dataset", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // ---------- Add field dialog ----------
    if (showAddFieldDialog) {
        val suggestions = listOf(
            "full_name", "email", "username", "password",
            "phone", "address", "city", "country", "note"
        )
        AlertDialog(
            onDismissRequest = { showAddFieldDialog = false },
            title = { Text("Add field") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFieldDraft,
                        onValueChange = { newFieldDraft = it },
                        label = { Text("Field name") },
                        placeholder = { Text("e.g. email, phone, otp") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Quick suggestions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestions
                            .filterNot { fields.contains(it) }
                            .forEach { s ->
                                AssistChip(
                                    onClick = { newFieldDraft = s },
                                    label = {
                                        Text(s, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                )
                            }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        addField(newFieldDraft)
                        showAddFieldDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddFieldDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FieldChip(name: String, onRemove: () -> Unit) {
    InputChip(
        selected = false,
        onClick = { },
        label = {
            Text(
                name.replace('_', ' '),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = "Remove $name",
                    tint = DangerRed,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    )
}

@Composable
private fun ManualRecordCard(
    index: Int,
    fields: List<String>,
    row: SnapshotStateMap<String, String>,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Record ${index + 1}",
                    fontWeight = FontWeight.Bold,
                    color = BrandOrange,
                    modifier = Modifier.weight(1f)
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            painterResource(R.drawable.ic_delete),
                            contentDescription = "Remove record",
                            tint = DangerRed
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            fields.forEach { f ->
                OutlinedTextField(
                    value = row[f].orEmpty(),
                    onValueChange = { row[f] = it },
                    label = { Text(f.replace('_', ' ')) },
                    singleLine = f != "message" && f != "note" && f != "address_line_1",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}