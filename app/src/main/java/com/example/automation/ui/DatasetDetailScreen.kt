package com.example.automation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.automation.Dataset
import com.example.automation.DatasetRecord
import com.example.automation.UsageMode
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen

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
    var searchQuery by remember { mutableStateOf("") }
    var showAddRecordDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<DatasetRecord?>(null) }
    var showUsageModeDropdown by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(dataset.name) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showConfigToggles by remember { mutableStateOf(false) }

    val filteredRecords = remember(records, searchQuery) {
        if (searchQuery.isBlank()) records
        else records.filter { r ->
            r.fields.values.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        renameInput = dataset.name
                        showRenameDialog = true
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dataset.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "Rename dataset",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "${records.count { !it.isUsed }} of ${records.size} unused",
                    fontSize = 12.sp,
                    color = BrandOrange
                )
            }

            IconButton(onClick = { showAddRecordDialog = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Add record",
                    tint = BrandOrange
                )
            }

            IconButton(onClick = { showDeleteConfirmDialog = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = "Delete dataset",
                    tint = DangerRed
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search records...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            singleLine = true
        )

        // Config & Mode Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Button(
                    onClick = { showUsageModeDropdown = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "Mode: ${dataset.usageMode.displayName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showUsageModeDropdown,
                    onDismissRequest = { showUsageModeDropdown = false }
                ) {
                    UsageMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.displayName) },
                            onClick = {
                                onUpdateDataset(dataset.copy(usageMode = mode))
                                showUsageModeDropdown = false
                            }
                        )
                    }
                }
            }

            Row {
                TextButton(onClick = onMarkAllUnused) {
                    Text("Mark all unused", fontSize = 12.sp, color = BrandOrange)
                }
                TextButton(onClick = onResetPointer) {
                    Text("Reset pointer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Toggles Accordion
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showConfigToggles = !showConfigToggles }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (showConfigToggles) "Hide dataset toggles" else "Configure toggles & loop options",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = BrandOrange
            )
        }

        if (showConfigToggles) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Skip already used records", fontSize = 13.sp)
                        Switch(
                            checked = dataset.skipAlreadyUsed,
                            onCheckedChange = { onUpdateDataset(dataset.copy(skipAlreadyUsed = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrandOrange)
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Delete record after use", fontSize = 13.sp)
                        Switch(
                            checked = dataset.deleteAfterUse,
                            onCheckedChange = { onUpdateDataset(dataset.copy(deleteAfterUse = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrandOrange)
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Loop when finished", fontSize = 13.sp)
                        Switch(
                            checked = dataset.loopBackToStart,
                            onCheckedChange = { onUpdateDataset(dataset.copy(loopBackToStart = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrandOrange)
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mark used on submit", fontSize = 13.sp)
                            Text("Only mark after form submitted (otherwise marks on fill)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = dataset.markAsUsedOnSubmit,
                            onCheckedChange = { onUpdateDataset(dataset.copy(markAsUsedOnSubmit = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrandOrange)
                        )
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Records List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            items(filteredRecords, key = { it.id }) { record ->
                RecordItemCard(
                    record = record,
                    onEdit = { editingRecord = record },
                    onDelete = { onDeleteRecord(record.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Rename Dataset Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Dataset") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Dataset Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onUpdateDataset(dataset.copy(name = renameInput.trim()))
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete Dataset Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Dataset?") },
            text = { Text("Are you sure you want to delete '${dataset.name}' and all its ${records.size} records? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteDataset()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add / Edit Record Dialog
    if (showAddRecordDialog || editingRecord != null) {
        val target = editingRecord
        var email by remember { mutableStateOf(target?.fields?.get("email") ?: "") }
        var password by remember { mutableStateOf(target?.fields?.get("password") ?: "") }
        var fullName by remember { mutableStateOf(target?.fields?.get("full_name") ?: "") }
        var username by remember { mutableStateOf(target?.fields?.get("username") ?: "") }
        var phone by remember { mutableStateOf(target?.fields?.get("phone") ?: "") }
        var securityQuestion by remember { mutableStateOf(target?.fields?.get("security_question") ?: "") }
        var securityAnswer by remember { mutableStateOf(target?.fields?.get("security_answer") ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddRecordDialog = false
                editingRecord = null
            },
            title = { Text(if (target != null) "Edit Record" else "Add Record") },
            text = {
                Column {
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = securityQuestion, onValueChange = { securityQuestion = it }, label = { Text("Security Question") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = securityAnswer, onValueChange = { securityAnswer = it }, label = { Text("Security Answer") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fields = (target?.fields?.toMutableMap() ?: mutableMapOf()).apply {
                            if (fullName.isNotBlank()) put("full_name", fullName)
                            if (email.isNotBlank()) put("email", email)
                            if (username.isNotBlank()) put("username", username)
                            if (password.isNotBlank()) put("password", password)
                            if (phone.isNotBlank()) put("phone", phone)
                            if (securityQuestion.isNotBlank()) put("security_question", securityQuestion)
                            if (securityAnswer.isNotBlank()) put("security_answer", securityAnswer)
                        }

                        if (target != null) {
                            onUpdateRecord(target.copy(fields = fields))
                        } else {
                            onAddRecord(fields)
                        }
                        showAddRecordDialog = false
                        editingRecord = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddRecordDialog = false
                    editingRecord = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RecordItemCard(
    record: DatasetRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var revealPassword by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.fields["full_name"] ?: record.fields["username"] ?: "Record #${record.orderIndex + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (record.isUsed) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Gray.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Used", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Show primary fields
            record.fields.forEach { (k, v) ->
                if (k == "password" || k == "confirm_password") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$k: ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (revealPassword) v else "••••••••",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { revealPassword = !revealPassword }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                painter = painterResource(id = if (revealPassword) R.drawable.ic_lock_open else R.drawable.ic_lock),
                                contentDescription = "Reveal",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "$k: $v",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Text("Edit", fontSize = 13.sp, color = BrandOrange)
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", fontSize = 13.sp, color = DangerRed)
                }
            }
        }
    }
}
