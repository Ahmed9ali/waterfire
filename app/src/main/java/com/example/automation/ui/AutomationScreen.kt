package com.example.automation.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.automation.AutomationFileAnalyzer
import com.example.automation.Dataset
import com.example.automation.DatasetRecord
import com.example.automation.FileAnalysisResult
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AutomationScreen(
    viewModel: BrowserViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val automationManager = viewModel.automationManager
    val datasets by automationManager.datasets.collectAsState()
    val allRecords by automationManager.records.collectAsState()
    val settings by automationManager.settings.collectAsState()

    var selectedDatasetForDetail by remember { mutableStateOf<Dataset?>(null) }
    var pendingAnalysisResult by remember { mutableStateOf<Pair<String, FileAnalysisResult>?>(null) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteTextContent by remember { mutableStateOf("") }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var showAddDisabledSiteDialog by remember { mutableStateOf(false) }
    var newDisabledSiteInput by remember { mutableStateOf("") }

    BackHandler {
        if (selectedDatasetForDetail != null) {
            selectedDatasetForDetail = null
        } else if (pendingAnalysisResult != null) {
            pendingAnalysisResult = null
        } else {
            viewModel.navigateTo(AppScreen.BROWSER)
        }
    }

    // SAF File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val readResult = withContext(Dispatchers.IO) {
                    val cr = context.contentResolver
                    val name = uri.lastPathSegment ?: "uploaded_data.txt"
                    val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                    if (bytes.size > 5 * 1024 * 1024) {
                        return@withContext Triple(name, "", "File exceeds maximum size of 5 MB")
                    }
                    if (AutomationFileAnalyzer.isBinaryFile(bytes)) {
                        return@withContext Triple(name, "", "Binary files cannot be parsed")
                    }
                    val content = AutomationFileAnalyzer.detectEncodingAndRead(bytes)
                    Triple(name, content, null)
                }

                val (fileName, content, error) = readResult
                if (error != null) {
                    viewModel.snackbarMessage.value = error
                } else if (content.isNotBlank()) {
                    val analysis = withContext(Dispatchers.Default) {
                        AutomationFileAnalyzer.analyzeText(content)
                    }
                    if (analysis.validRecordCount == 0) {
                        viewModel.snackbarMessage.value = "No valid records recognized in file."
                    } else {
                        pendingAnalysisResult = Pair(fileName, analysis)
                    }
                } else {
                    viewModel.snackbarMessage.value = "File is empty or unrecognized."
                }
            }
        }
    }

    // Review Screen overlay
    pendingAnalysisResult?.let { (fileName, analysis) ->
        FileAnalysisReviewScreen(
            fileName = fileName,
            analysisResult = analysis,
            onCancel = { pendingAnalysisResult = null },
            onConfirmSave = { datasetName, mappedRecords ->
                scope.launch {
                    val newDataset = Dataset(
                        name = datasetName,
                        sourceFileName = fileName,
                        isActive = datasets.isEmpty()
                    )
                    val records = mappedRecords.mapIndexed { index, fields ->
                        DatasetRecord(
                            datasetId = newDataset.id,
                            fields = fields,
                            orderIndex = index
                        )
                    }
                    automationManager.repository.saveDatasetWithRecords(newDataset, records)
                    pendingAnalysisResult = null
                    viewModel.snackbarMessage.value = "Saved ${records.size} records to $datasetName"
                }
            }
        )
        return
    }

    // Detail Screen overlay
    selectedDatasetForDetail?.let { targetDataset ->
        val currentDs = datasets.firstOrNull { it.id == targetDataset.id } ?: targetDataset
        val records = allRecords.filter { it.datasetId == currentDs.id }.sortedBy { it.orderIndex }
        DatasetDetailScreen(
            dataset = currentDs,
            records = records,
            onBack = { selectedDatasetForDetail = null },
            onUpdateDataset = { updated ->
                scope.launch { automationManager.repository.updateDataset(updated) }
            },
            onDeleteDataset = {
                scope.launch {
                    automationManager.repository.deleteDataset(currentDs.id)
                    selectedDatasetForDetail = null
                }
            },
            onMarkAllUnused = {
                scope.launch { automationManager.repository.markAllRecordsUnused(currentDs.id) }
            },
            onResetPointer = {
                scope.launch { automationManager.repository.resetPointer(currentDs.id) }
            },
            onAddRecord = { fields ->
                scope.launch {
                    val rec = DatasetRecord(
                        datasetId = currentDs.id,
                        fields = fields,
                        orderIndex = records.size
                    )
                    automationManager.repository.insertRecord(rec)
                }
            },
            onUpdateRecord = { updatedRec ->
                scope.launch { automationManager.repository.updateRecord(updatedRec) }
            },
            onDeleteRecord = { recId ->
                scope.launch { automationManager.repository.deleteRecord(recId) }
            }
        )
        return
    }

    // Main Automation List & Settings
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.BROWSER) },
                modifier = Modifier.testTag("automation_back_button")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Automation",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Test on sample form button
            Button(
                onClick = {
                    viewModel.tabManager.loadUrl("file:///android_asset/automation_test.html")
                    viewModel.navigateTo(AppScreen.BROWSER)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text("Test Page", fontSize = 12.sp, color = BrandOrange)
            }
        }

        // Action Buttons Row: Upload File & Paste Text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("*/*", "text/*", "application/json")) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_download), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Upload File", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showPasteDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_edit), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Paste Text", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
        }

        Divider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = "Datasets",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandOrange,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (datasets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_automation), contentDescription = null, tint = BrandOrange, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Datasets Yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Upload a .txt, .csv, .tsv or .json file (max 5 MB) or paste raw text to parse records and fill forms with one tap.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(datasets, key = { it.id }) { ds ->
                    val dsRecords = allRecords.filter { it.datasetId == ds.id }
                    val unusedCount = dsRecords.count { !it.isUsed }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedDatasetForDetail = ds },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ds.isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        ),
                        border = if (ds.isActive) androidx.compose.foundation.BorderStroke(1.5.dp, BrandOrange) else null
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = ds.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (ds.isActive) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SuccessGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                        }
                                    }
                                }

                                if (!ds.isActive) {
                                    TextButton(onClick = {
                                        scope.launch { automationManager.repository.setActiveDataset(ds.id) }
                                    }) {
                                        Text("Set Active", fontSize = 12.sp, color = BrandOrange)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "File: ${ds.sourceFileName} • Mode: ${ds.usageMode.displayName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Record ${ds.currentIndex + 1} of ${dsRecords.size} ($unusedCount unused)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandOrange
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Text(
                    text = "Global Settings",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandOrange,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Form detection", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("Detect forms and inputs on web pages", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.detectionEnabled,
                        onCheckedChange = { scope.launch { automationManager.repository.updateSettings(settings.copy(detectionEnabled = it)) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandOrange)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show icon on forms", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("Display Automation fill button in address bar when forms are found", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.showOmnibarIcon,
                        onCheckedChange = { scope.launch { automationManager.repository.updateSettings(settings.copy(showOmnibarIcon = it)) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandOrange)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Highlight filled fields", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("Briefly glow orange outline around fields when filled", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.highlightFilledFields,
                        onCheckedChange = { scope.launch { automationManager.repository.updateSettings(settings.copy(highlightFilledFields = it)) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandOrange)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-submit after filling", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("Submit form immediately after data injection (OFF by default)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.autoSubmitAfterFilling,
                        onCheckedChange = { scope.launch { automationManager.repository.updateSettings(settings.copy(autoSubmitAfterFilling = it)) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandOrange)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Biometric Lock", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("Require biometric confirmation before revealing passwords", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.biometricLockEnabled,
                        onCheckedChange = { scope.launch { automationManager.repository.updateSettings(settings.copy(biometricLockEnabled = it)) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandOrange)
                    )
                }
            }

            // Per-Site Disable List
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Disabled Sites", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("Sites where form detection is turned off", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = {
                        newDisabledSiteInput = ""
                        showAddDisabledSiteDialog = true
                    }) {
                        Text("+ Add Site", color = BrandOrange, fontSize = 12.sp)
                    }
                }

                if (settings.disabledSites.isEmpty()) {
                    Text(
                        text = "No disabled sites. Automation is enabled everywhere.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        settings.disabledSites.forEach { site ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(site, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                IconButton(
                                    onClick = { viewModel.removeDisabledSite(site) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_close),
                                        contentDescription = "Remove",
                                        tint = DangerRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showWipeConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Wipe all Automation data", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Add Disabled Site Dialog
    if (showAddDisabledSiteDialog) {
        AlertDialog(
            onDismissRequest = { showAddDisabledSiteDialog = false },
            title = { Text("Disable on Site") },
            text = {
                OutlinedTextField(
                    value = newDisabledSiteInput,
                    onValueChange = { newDisabledSiteInput = it },
                    label = { Text("Domain (e.g. example.com)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDisabledSiteInput.isNotBlank()) {
                            val domain = newDisabledSiteInput.trim().removePrefix("https://").removePrefix("http://").substringBefore('/')
                            viewModel.disableAutomationOnSite(domain)
                        }
                        showAddDisabledSiteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Disable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDisabledSiteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Paste Text Dialog
    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Paste Text Data") },
            text = {
                OutlinedTextField(
                    value = pasteTextContent,
                    onValueChange = { pasteTextContent = it },
                    placeholder = { Text("Paste lines, key-value blocks, CSV or JSON here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = pasteTextContent
                        if (text.isNotBlank()) {
                            val analysis = AutomationFileAnalyzer.analyzeText(text)
                            if (analysis.validRecordCount == 0) {
                                viewModel.snackbarMessage.value = "No records recognized in pasted text."
                            } else {
                                pendingAnalysisResult = Pair("pasted_data.txt", analysis)
                            }
                        }
                        showPasteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Analyze")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Wipe Confirm Dialog
    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            title = { Text("Wipe all Automation data?") },
            text = { Text("This will permanently delete all uploaded datasets and records.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            automationManager.repository.wipeAllAutomationData()
                            automationManager.clearAllPageRecordCaches()
                            showWipeConfirmDialog = false
                            viewModel.snackbarMessage.value = "All Automation data wiped"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Wipe All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}
