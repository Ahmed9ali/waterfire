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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.example.automation.Dataset
import com.example.automation.DatasetRecord
import com.example.automation.UsageMode
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen

private enum class RecordFilter(val label: String) {
    All("All"),
    Unused("Unused"),
    Used("Used")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetDetailScreen(
    dataset: Dataset,
    records: List<DatasetRecord>,
    onBack: () -> Unit,
    onUpdateDataset: (Dataset) -> Unit,
    onDeleteDataset: () -> Unit,
    onMarkAllUnused: () -> Unit,
    onResetPointer: () -> Unit,
    onAddRecord: (Map<String, String>) -> Unit,
    onUpdateRecord: (DatasetRecord) -> Unit,
    onDeleteRecord: (String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(RecordFilter.All) }
    var showUsageMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(dataset.name) }
    var showDeleteDs by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<DatasetRecord?>(null) }
    var addingRecord by remember { mutableStateOf(false) }
    var showToggles by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    val filtered = remember(records, search, filter) {
        records.filter { r ->
            val matchesFilter = when (filter) {
                RecordFilter.All -> true
                RecordFilter.Unused -> !r.isUsed
                RecordFilter.Used -> r.isUsed
            }
            val matchesSearch = search.isBlank() ||
                    r.fields.values.any { it.contains(search, ignoreCase = true) }
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            dataset.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${records.count { !it.isUsed }} of ${records.size} unused • Record ${dataset.currentIndex + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandOrange
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { renameInput = dataset.name; showRenameDialog = true }) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = "Rename")
                    }
                    IconButton(onClick = { showDeleteDs = true }) {
                        Icon(painterResource(R.drawable.ic_delete), contentDescription = "Delete", tint = DangerRed)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addingRecord = true },
                containerColor = BrandOrange,
                contentColor = Color.White,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text("Add record") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------- Usage mode card ----------
            item {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Usage mode", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(dataset.usageMode.displayName) },
                            trailingContent = {
                                TextButton(onClick = { showUsageMenu = !showUsageMenu }) {
                                    Text("Change", color = BrandOrange)
                                }
                            }
                        )
                        if (showUsageMenu) {
                            UsageMode.entries.forEach { m ->
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                ListItem(
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    headlineContent = { Text(m.displayName) },
                                    trailingContent = {
                                        if (m == dataset.usageMode) Icon(
                                            painterResource(R.drawable.ic_check),
                                            contentDescription = null,
                                            tint = SuccessGreen
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onUpdateDataset(dataset.copy(usageMode = m))
                                            showUsageMenu = false
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // ---------- Reset card ----------
            item {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text("Reset progress", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Mark every record unused & reset pointer") },
                        trailingContent = {
                            Row {
                                TextButton(onClick = onMarkAllUnused) {
                                    Text("Unused", color = BrandOrange)
                                }
                                TextButton(onClick = onResetPointer) {
                                    Text("Pointer", color = BrandOrange)
                                }
                            }
                        }
                    )
                }
            }

            // ---------- Toggles card ----------
            item {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Advanced toggles", fontWeight = FontWeight.SemiBold) },
                            trailingContent = {
                                TextButton(onClick = { showToggles = !showToggles }) {
                                    Text(if (showToggles) "Hide" else "Show", color = BrandOrange)
                                }
                            }
                        )
                        if (showToggles) {
                            DetailToggle("Skip already used", dataset.skipAlreadyUsed) {
                                onUpdateDataset(dataset.copy(skipAlreadyUsed = it))
                            }
                            DetailToggle("Delete after use", dataset.deleteAfterUse) {
                                onUpdateDataset(dataset.copy(deleteAfterUse = it))
                            }
                            DetailToggle("Loop when finished", dataset.loopBackToStart) {
                                onUpdateDataset(dataset.copy(loopBackToStart = it))
                            }
                            DetailToggle("Mark used only on submit", dataset.markAsUsedOnSubmit) {
                                onUpdateDataset(dataset.copy(markAsUsedOnSubmit = it))
                            }
                        }
                    }
                }
            }

            // ---------- Search + filters ----------
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search records…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RecordFilter.entries.forEach { f ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = { Text(f.label) }
                        )
                    }
                }
            }

            // ---------- Records ----------
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "No records match.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(filtered, key = { it.id }) { r ->
                    RecordCard(r, onEdit = { editingRecord = r }, onDelete = { onDeleteRecord(r.id) })
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // ---------------- Rename dialog ----------------
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename dataset") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) onUpdateDataset(dataset.copy(name = renameInput.trim()))
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ---------------- Delete dataset dialog ----------------
    if (showDeleteDs) {
        AlertDialog(
            onDismissRequest = { showDeleteDs = false },
            title = { Text("Delete dataset?") },
            text = { Text("Deletes \"${dataset.name}\" and all ${records.size} records. Cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDs = false
                        onDeleteDataset()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDs = false }) { Text("Cancel") }
            }
        )
    }

    // ---------------- Edit / add record dialog ----------------
    if (addingRecord || editingRecord != null) {
        val target = editingRecord
        EditRecordDialog(
            title = if (target == null) "Add record" else "Edit record",
            initial = target?.fields ?: emptyMap(),
            onDismiss = {
                addingRecord = false
                editingRecord = null
            },
            onSave = { fields ->
                if (target == null) onAddRecord(fields) else onUpdateRecord(target.copy(fields = fields))
                addingRecord = false
                editingRecord = null
            }
        )
    }
}

@Composable
private fun DetailToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) }
    )
}

@Composable
private fun RecordCard(
    record: DatasetRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var revealPw by remember { mutableStateOf(false) }

    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    record.fields["full_name"]
                        ?: record.fields["username"]
                        ?: "Record #${record.orderIndex + 1}",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (record.isUsed) {
                    Text(
                        "USED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            record.fields.forEach { (k, v) ->
                if (k == "password" || k == "confirm_password") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$k: ", style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (revealPw) v else "••••••••",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { revealPw = !revealPw }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                painterResource(if (revealPw) R.drawable.ic_lock_open else R.drawable.ic_lock),
                                contentDescription = "Reveal",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        "$k: $v",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) { Text("Edit", color = BrandOrange) }
                TextButton(onClick = onDelete) { Text("Delete", color = DangerRed) }
            }
        }
    }
}

@Composable
private fun EditRecordDialog(
    title: String,
    initial: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    val mutableFields = remember {
        mutableStateMapOf<String, String>().apply { putAll(initial) }
    }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                mutableFields.keys.toList().forEach { key ->
                    OutlinedTextField(
                        value = mutableFields[key] ?: "",
                        onValueChange = { mutableFields[key] = it },
                        label = { Text(key) },
                        singleLine = key != "message",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    "Add field",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Key") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Value") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        val k = newKey.trim()
                        if (k.isNotBlank()) {
                            mutableFields[k] = newValue
                            newKey = ""
                            newValue = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add field") }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val out = mutableFields.filterValues { it.isNotBlank() }
                    if (out.isNotEmpty()) onSave(out)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}