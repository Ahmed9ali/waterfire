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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.R
import com.example.automation.AutomationFileAnalyzer
import com.example.automation.CountryList
import com.example.automation.FieldType
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.DangerRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FieldDef(
    val key: String,
    val type: FieldType,
    val displayName: String
)

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

    val fields = remember { mutableStateListOf<FieldDef>() }
    val rows = remember { mutableStateListOf<SnapshotStateMap<String, String>>() }

    var showAddFieldDialog by remember { mutableStateOf(false) }
    var editingFieldKey by remember { mutableStateOf<String?>(null) }

    fun sanitizeKey(raw: String): String =
        raw.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    fun addField(key: String, type: FieldType, display: String) {
        val k = sanitizeKey(key)
        if (k.isBlank()) return
        if (fields.any { it.key == k }) {
            scope.launch { snackbar.showSnackbar("Field \"$k\" already exists") }
            return
        }
        val finalType = if (type == FieldType.AUTO) FieldType.infer(k) else type
        fields.add(FieldDef(key = k, type = finalType, displayName = display.ifBlank { k }))
        rows.forEach { it[k] = "" }
    }

    fun updateField(oldKey: String, newDef: FieldDef) {
        val idx = fields.indexOfFirst { it.key == oldKey }
        if (idx < 0) return
        val newKey = sanitizeKey(newDef.key)
        if (newKey.isBlank()) return
        if (newKey != oldKey && fields.any { it.key == newKey }) {
            scope.launch { snackbar.showSnackbar("Field \"$newKey\" already exists") }
            return
        }
        fields[idx] = newDef.copy(key = newKey)
        if (newKey != oldKey) {
            rows.forEach { row ->
                val v = row.remove(oldKey)
                if (v != null) row[newKey] = v
            }
        }
    }

    fun removeField(k: String) {
        fields.removeAll { it.key == k }
        rows.forEach { it.remove(k) }
    }

    fun addRow() {
        val m = mutableStateMapOf<String, String>()
        fields.forEach { m[it.key] = "" }
        rows.add(m)
    }

    fun removeRow(index: Int) {
        if (index in rows.indices) rows.removeAt(index)
    }

    fun quickPreset() {
        val preset = listOf(
            "full_name" to FieldType.FULL_NAME,
            "email" to FieldType.EMAIL,
            "username" to FieldType.USERNAME,
            "password" to FieldType.PASSWORD,
            "phone" to FieldType.PHONE,
            "date_of_birth" to FieldType.DATE_OF_BIRTH,
            "country" to FieldType.COUNTRY
        )
        preset.forEach { (k, t) -> if (fields.none { it.key == k }) addField(k, t, k) }
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
        bottomBar = {
            Column {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }

                    Button(
                        onClick = {
                            val cleanRecords = rows.map { row ->
                                val m = mutableMapOf<String, String>()
                                fields.forEach { f ->
                                    val v = row[f.key]?.trim().orEmpty()
                                    if (v.isNotBlank()) m[f.key] = v
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
                                    fields.map { it.key },
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
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                                "Add the field names you want (email, phone, date of birth, country…). " +
                                        "Each field has a type that decides the input UI. Then fill one card per record.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }

            // ---------- Fields ----------
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
                        editingFieldKey = null
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
                                        editingFieldKey = null
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
                            FieldChip(
                                field = f,
                                onEdit = {
                                    editingFieldKey = f.key
                                    showAddFieldDialog = true
                                },
                                onRemove = { removeField(f.key) }
                            )
                        }
                    }
                }
            }

            // ---------- Records ----------
            if (fields.isNotEmpty()) {
                item {
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
                                    "Tap \"Add record\" below to start filling values.",
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

                // Add record button — inline, no FAB overlap
                item {
                    Button(
                        onClick = { addRow() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Add record", fontWeight = FontWeight.SemiBold)
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // ---------- Add / Edit field dialog ----------
    if (showAddFieldDialog) {
        val editing = editingFieldKey?.let { k -> fields.firstOrNull { it.key == k } }
        AddFieldDialog(
            existing = editing,
            onDismiss = { showAddFieldDialog = false; editingFieldKey = null },
            onConfirm = { key, type, display ->
                if (editing == null) {
                    addField(key, type, display)
                } else {
                    val newType = if (type == FieldType.AUTO) FieldType.infer(key) else type
                    updateField(editing.key, FieldDef(sanitizeKey(key), newType, display.ifBlank { key }))
                }
                showAddFieldDialog = false
                editingFieldKey = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFieldDialog(
    existing: FieldDef?,
    onDismiss: () -> Unit,
    onConfirm: (key: String, type: FieldType, display: String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.key ?: "") }
    var displayName by remember { mutableStateOf(existing?.displayName ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: FieldType.AUTO) }
    var typeMenuOpen by remember { mutableStateOf(false) }

    val inferred = FieldType.infer(name)

    val suggestions = listOf(
        "full_name", "email", "phone", "username", "password",
        "date_of_birth", "country", "address_line_1", "city", "state", "zip_code"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add field" else "Edit field") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (displayName.isBlank()) displayName = it
                        if (type == FieldType.AUTO) type = FieldType.infer(it)
                    },
                    label = { Text("Field name (used as key)") },
                    placeholder = { Text("e.g. email, phone, otp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display label (optional)") },
                    placeholder = { Text("e.g. Email address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = typeMenuOpen,
                    onExpandedChange = { typeMenuOpen = !typeMenuOpen }
                ) {
                    OutlinedTextField(
                        value = type.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Field type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenuOpen) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuOpen,
                        onDismissRequest = { typeMenuOpen = false }
                    ) {
                        FieldType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.label) },
                                onClick = {
                                    type = if (t == FieldType.AUTO) inferred else t
                                    typeMenuOpen = false
                                }
                            )
                        }
                    }
                }

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
                    suggestions.forEach { s ->
                        AssistChip(
                            onClick = {
                                name = s
                                displayName = s
                                type = FieldType.infer(s)
                            },
                            label = { Text(s, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, type, displayName) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) { Text(if (existing == null) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun FieldChip(
    field: FieldDef,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    InputChip(
        selected = false,
        onClick = onEdit,
        label = {
            Text(
                "${field.displayName} · ${field.type.label}",
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
                    contentDescription = "Remove ${field.key}",
                    tint = DangerRed,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualRecordCard(
    index: Int,
    fields: List<FieldDef>,
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
                when (f.type) {
                    FieldType.DATE_OF_BIRTH -> DobField(
                        field = f,
                        value = row[f.key].orEmpty(),
                        onChange = { row[f.key] = it }
                    )
                    FieldType.COUNTRY -> CountryDropdown(
                        field = f,
                        value = row[f.key].orEmpty(),
                        onChange = { row[f.key] = it }
                    )
                    FieldType.PASSWORD -> OutlinedTextField(
                        value = row[f.key].orEmpty(),
                        onValueChange = { row[f.key] = it },
                        label = { Text(f.displayName) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    FieldType.EMAIL -> OutlinedTextField(
                        value = row[f.key].orEmpty(),
                        onValueChange = { row[f.key] = it },
                        label = { Text(f.displayName) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    FieldType.PHONE -> OutlinedTextField(
                        value = row[f.key].orEmpty(),
                        onValueChange = { row[f.key] = it },
                        label = { Text(f.displayName) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    else -> OutlinedTextField(
                        value = row[f.key].orEmpty(),
                        onValueChange = { row[f.key] = it },
                        label = { Text(f.displayName) },
                        singleLine = f.key != "message" && f.key != "note" && f.key != "address_line_1",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DobField(
    field: FieldDef,
    value: String,
    onChange: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(field.displayName) },
            placeholder = { Text("yyyy-mm-dd") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        OutlinedButton(
            onClick = { showPicker = true },
            modifier = Modifier.height(56.dp)
        ) {
            Icon(
                painterResource(R.drawable.ic_edit),
                contentDescription = "Pick date",
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val ms = state.selectedDateMillis
                    if (ms != null) {
                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        onChange(fmt.format(Date(ms)))
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryDropdown(
    field: FieldDef,
    value: String,
    onChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(value) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = if (expanded) query else value,
            onValueChange = {
                query = it
                onChange(it)
            },
            label = { Text(field.displayName) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val filtered = if (query.isBlank()) CountryList.ALL
            else CountryList.ALL.filter { it.contains(query, ignoreCase = true) }
            if (filtered.isEmpty()) {
                DropdownMenuItem(text = { Text("No matches") }, onClick = { expanded = false })
            } else {
                filtered.take(80).forEach { country ->
                    DropdownMenuItem(
                        text = { Text(country) },
                        onClick = {
                            onChange(country)
                            query = country
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}