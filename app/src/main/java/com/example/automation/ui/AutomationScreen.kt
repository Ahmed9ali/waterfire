package com.example.automation.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.automation.AutomationFileAnalyzer
import com.example.automation.ContentFormat
import com.example.automation.Dataset
import com.example.automation.DatasetRecord
import com.example.automation.SmartAnalysisResult
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ImportMode(val label: String) {
    Upload("Upload"),
    Paste("Paste"),
    Manual("Manual")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(viewModel: BrowserViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val automationManager = viewModel.automationManager
    val datasets by automationManager.datasets.collectAsState()
    val allRecords by automationManager.records.collectAsState()
    val settings by automationManager.settings.collectAsState()

    var importMode by remember { mutableStateOf(ImportMode.Upload) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var pendingResult by remember { mutableStateOf<Pair<String, SmartAnalysisResult>?>(null) }
    var showManualSheet by remember { mutableStateOf(false) }
    var manualInitialName by remember { mutableStateOf("") }
    var selectedDataset by remember { mutableStateOf<Dataset?>(null) }
    var showWipe by remember { mutableStateOf(false) }
    var showAddSite by remember { mutableStateOf(false) }
    var newSite by remember { mutableStateOf("") }

    BackHandler {
        when {
            selectedDataset != null -> selectedDataset = null
            showManualSheet -> showManualSheet = false
            pendingResult != null -> pendingResult = null
            else -> viewModel.navigateTo(AppScreen.BROWSER)
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            isBusy = true
            try {
                val (name, content, error) = withContext(Dispatchers.IO) {
                    val cr = context.contentResolver
                    val nm = uri.lastPathSegment ?: "uploaded_data.txt"
                    val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                    if (bytes.size > 5 * 1024 * 1024) return@withContext Triple(nm, "", "File exceeds 5 MB")
                    if (AutomationFileAnalyzer.isBinaryFile(bytes)) return@withContext Triple(nm, "", "Binary files are not supported")
                    Triple(nm, AutomationFileAnalyzer.detectEncodingAndRead(bytes), null)
                }
                if (error != null) snackbar.showSnackbar(error)
                else if (content.isBlank()) snackbar.showSnackbar("File is empty or unrecognized.")
                else {
                    val smart = withContext(Dispatchers.Default) { AutomationFileAnalyzer.analyzeSmart(content) }
                    if (smart.validRecordCount == 0 && smart.format == ContentFormat.UNKNOWN) {
                        manualInitialName = name.substringBeforeLast('.')
                        snackbar.showSnackbar("Couldn't read this file — opening manual entry.")
                        showManualSheet = true
                    } else {
                        pendingResult = name to smart
                    }
                }
            } finally { isBusy = false }
        }
    }

    // ---------------- Manual sheet overlay ----------------
    if (showManualSheet) {
        ManualFieldSheet(
            initialName = manualInitialName,
            onCancel = { showManualSheet = false },
            onSave = { name, _, records ->
                scope.launch {
                    val ds = Dataset(
                        name = name,
                        sourceFileName = "manual_entry",
                        isActive = datasets.isEmpty()
                    )
                    val recs = records.mapIndexed { i, f ->
                        DatasetRecord(datasetId = ds.id, fields = f, orderIndex = i)
                    }
                    automationManager.repository.saveDatasetWithRecords(ds, recs)
                    showManualSheet = false
                    snackbar.showSnackbar("Saved ${recs.size} records to $name")
                }
            }
        )
        return
    }

    // ---------------- Review overlay ----------------
    pendingResult?.let { (name, smart) ->
        FileAnalysisReviewScreen(
            fileName = name,
            analysisResult = smart,
            onCancel = { pendingResult = null },
            onSwitchToManual = {
                manualInitialName = name.substringBeforeLast('.')
                pendingResult = null
                showManualSheet = true
            },
            onConfirmSave = { datasetName, records, _ ->
                scope.launch {
                    val ds = Dataset(
                        name = datasetName,
                        sourceFileName = name,
                        isActive = datasets.isEmpty()
                    )
                    val recs = records.mapIndexed { i, f ->
                        DatasetRecord(datasetId = ds.id, fields = f, orderIndex = i)
                    }
                    automationManager.repository.saveDatasetWithRecords(ds, recs)
                    pendingResult = null
                    snackbar.showSnackbar("Saved ${recs.size} records to $datasetName")
                }
            }
        )
        return
    }

    // ---------------- Detail overlay ----------------
    selectedDataset?.let { ds0 ->
        val ds = datasets.firstOrNull { it.id == ds0.id } ?: ds0
        val recs = allRecords.filter { it.datasetId == ds.id }.sortedBy { it.orderIndex }
        DatasetDetailScreen(
            dataset = ds,
            records = recs,
            onBack = { selectedDataset = null },
            onUpdateDataset = { scope.launch { automationManager.repository.updateDataset(it) } },
            onDeleteDataset = {
                scope.launch {
                    automationManager.repository.deleteDataset(ds.id)
                    selectedDataset = null
                }
            },
            onMarkAllUnused = { scope.launch { automationManager.repository.markAllRecordsUnused(ds.id) } },
            onResetPointer = { scope.launch { automationManager.repository.resetPointer(ds.id) } },
            onAddRecord = { f ->
                scope.launch {
                    automationManager.repository.insertRecord(
                        DatasetRecord(datasetId = ds.id, fields = f, orderIndex = recs.size)
                    )
                }
            },
            onUpdateRecord = { scope.launch { automationManager.repository.updateRecord(it) } },
            onDeleteRecord = { scope.launch { automationManager.repository.deleteRecord(it) } }
        )
        return
    }

    // ---------------- Main screen ----------------
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.BROWSER) }) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Automation", fontWeight = FontWeight.Bold)
                        Text(
                            "${datasets.size} dataset${if (datasets.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.tabManager.loadUrl("file:///android_asset/automation_test.html")
                            viewModel.navigateTo(AppScreen.BROWSER)
                        }
                    ) { Text("Test Page", color = BrandOrange, fontSize = 12.sp) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    when (importMode) {
                        ImportMode.Upload -> filePicker.launch(arrayOf("*/*", "text/*", "application/json"))
                        ImportMode.Paste -> { pasteText = ""; showPasteDialog = true }
                        ImportMode.Manual -> { manualInitialName = ""; showManualSheet = true }
                    }
                },
                containerColor = BrandOrange,
                contentColor = Color.White,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(if (isBusy) "Reading…" else "Import") },
                expanded = !isBusy
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
            // ---------- Import mode segmented ----------
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ImportMode.entries.forEachIndexed { index, m ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, ImportMode.entries.size),
                            selected = importMode == m,
                            onClick = { importMode = m },
                            icon = {
                                Icon(
                                    painterResource(
                                        when (m) {
                                            ImportMode.Upload -> R.drawable.ic_download
                                            ImportMode.Paste -> R.drawable.ic_edit
                                            ImportMode.Manual -> R.drawable.ic_add
                                        }
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) { Text(m.label, fontSize = 12.sp) }
                    }
                }
            }

            // ---------- Primary action ----------
            item {
                when (importMode) {
                    ImportMode.Upload -> Button(
                        onClick = { filePicker.launch(arrayOf("*/*", "text/*", "application/json")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        if (isBusy) CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        ) else {
                            Icon(painterResource(R.drawable.ic_download), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Choose file to analyse", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    ImportMode.Paste -> Button(
                        onClick = { pasteText = ""; showPasteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Paste text to analyse", fontWeight = FontWeight.SemiBold)
                    }

                    ImportMode.Manual -> Button(
                        onClick = { manualInitialName = ""; showManualSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enter records manually", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ---------- Datasets ----------
            item {
                Text(
                    "Datasets",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            if (datasets.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_automation),
                                contentDescription = null,
                                tint = BrandOrange,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("No datasets yet", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Upload a .txt, .csv, .json, .xml, .html file (max 5 MB), paste any text, or enter records manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(datasets, key = { it.id }) { ds ->
                    val dsRecords = allRecords.filter { it.datasetId == ds.id }
                    val unused = dsRecords.count { !it.isUsed }
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ds.isActive) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (ds.isActive) BorderStroke(1.5.dp, BrandOrange) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDataset = ds }
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        ds.name,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (ds.isActive) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "ACTIVE",
                                            color = SuccessGreen,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            },
                            supportingContent = {
                                Column {
                                    Text(
                                        "${ds.sourceFileName.ifBlank { "no file" }} • ${ds.usageMode.displayName}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "Record ${ds.currentIndex + 1} of ${dsRecords.size} • $unused unused",
                                        color = BrandOrange,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            trailingContent = {
                                if (!ds.isActive) {
                                    TextButton(onClick = {
                                        scope.launch { automationManager.repository.setActiveDataset(ds.id) }
                                    }) {
                                        Text("Set active", color = BrandOrange, fontSize = 12.sp)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // ---------- Settings group ----------
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SettingSwitch(
                            title = "Form detection",
                            subtitle = "Find forms and inputs on web pages",
                            checked = settings.detectionEnabled,
                            onCheckedChange = {
                                scope.launch { automationManager.repository.updateSettings(settings.copy(detectionEnabled = it)) }
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        SettingSwitch(
                            title = "Show fill icon on forms",
                            subtitle = "Display the fill button when forms are detected",
                            checked = settings.showOmnibarIcon,
                            onCheckedChange = {
                                scope.launch { automationManager.repository.updateSettings(settings.copy(showOmnibarIcon = it)) }
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        SettingSwitch(
                            title = "Highlight filled fields",
                            subtitle = "Glow outline when a field is filled",
                            checked = settings.highlightFilledFields,
                            onCheckedChange = {
                                scope.launch { automationManager.repository.updateSettings(settings.copy(highlightFilledFields = it)) }
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        SettingSwitch(
                            title = "Auto-submit after filling",
                            subtitle = "Submit the form immediately after injection",
                            checked = settings.autoSubmitAfterFilling,
                            onCheckedChange = {
                                scope.launch { automationManager.repository.updateSettings(settings.copy(autoSubmitAfterFilling = it)) }
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        SettingSwitch(
                            title = "Biometric lock",
                            subtitle = "Ask for biometrics before revealing passwords",
                            checked = settings.biometricLockEnabled,
                            onCheckedChange = {
                                scope.launch { automationManager.repository.updateSettings(settings.copy(biometricLockEnabled = it)) }
                            }
                        )
                    }
                }
            }

            // ---------- Disabled sites ----------
            item {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Disabled sites", fontWeight = FontWeight.SemiBold) },
                            supportingContent = {
                                Text("${settings.disabledSites.size} site${if (settings.disabledSites.size == 1) "" else "s"} excluded")
                            },
                            trailingContent = {
                                TextButton(onClick = { newSite = ""; showAddSite = true }) {
                                    Text("+ Add", color = BrandOrange)
                                }
                            }
                        )
                        if (settings.disabledSites.isEmpty()) {
                            Text(
                                "Automation is enabled everywhere.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            )
                        } else {
                            settings.disabledSites.forEach { site ->
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                ListItem(
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    headlineContent = {
                                        Text(site, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    },
                                    trailingContent = {
                                        IconButton(onClick = { viewModel.removeDisabledSite(site) }) {
                                            Icon(
                                                painterResource(R.drawable.ic_close),
                                                contentDescription = "Remove",
                                                tint = DangerRed
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ---------- Danger zone ----------
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showWipe = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Wipe all automation data", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // ---------------- Paste dialog ----------------
    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Paste text") },
            text = {
                Column {
                    Text(
                        "We'll detect the format (CSV, JSON, key-value, HTML, free text…) automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        placeholder = { Text("Paste anything…") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = pasteText
                        showPasteDialog = false
                        if (text.isNotBlank()) {
                            scope.launch {
                                val smart = withContext(Dispatchers.Default) {
                                    AutomationFileAnalyzer.analyzeSmart(text)
                                }
                                if (smart.validRecordCount == 0 && smart.format == ContentFormat.UNKNOWN) {
                                    manualInitialName = "pasted_text"
                                    snackbar.showSnackbar("Couldn't read the pasted text — opening manual entry.")
                                    showManualSheet = true
                                } else {
                                    pendingResult = "pasted_text.txt" to smart
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) { Text("Analyse") }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ---------------- Add disabled site dialog ----------------
    if (showAddSite) {
        AlertDialog(
            onDismissRequest = { showAddSite = false },
            title = { Text("Disable on site") },
            text = {
                OutlinedTextField(
                    value = newSite,
                    onValueChange = { newSite = it },
                    label = { Text("Domain (example.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val raw = newSite.trim()
                        if (raw.isNotBlank()) {
                            val d = raw.removePrefix("https://").removePrefix("http://").substringBefore('/')
                            viewModel.disableAutomationOnSite(d)
                        }
                        showAddSite = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) { Text("Disable") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSite = false }) { Text("Cancel") }
            }
        )
    }

    // ---------------- Wipe confirm dialog ----------------
    if (showWipe) {
        AlertDialog(
            onDismissRequest = { showWipe = false },
            title = { Text("Wipe all data?") },
            text = { Text("This permanently deletes every dataset and record. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            automationManager.repository.wipeAllAutomationData()
                            automationManager.clearAllPageRecordCaches()
                            showWipe = false
                            snackbar.showSnackbar("All automation data wiped")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("Wipe all") }
            },
            dismissButton = {
                TextButton(onClick = { showWipe = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(subtitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}