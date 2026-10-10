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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.automation.AutomationFileAnalyzer
import com.example.automation.DetectedColumn
import com.example.automation.FileAnalysisResult
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.SuccessGreen

@Composable
fun FileAnalysisReviewScreen(
    fileName: String,
    analysisResult: FileAnalysisResult,
    onCancel: () -> Unit,
    onConfirmSave: (name: String, mappedRecords: List<Map<String, String>>) -> Unit
) {
    var datasetName by remember { mutableStateOf(fileName.substringBeforeLast('.')) }
    val columnMappings = remember {
        mutableStateMapOf<String, String>().apply {
            analysisResult.detectedColumns.forEach { col ->
                put(col.rawKey, col.normalizedType)
            }
        }
    }
    val ignoredColumns = remember { mutableStateMapOf<String, Boolean>() }

    val commonTypes = listOf(
        "full_name", "first_name", "last_name", "username", "email",
        "password", "phone", "website", "address_line_1", "city",
        "state", "zip_code", "country", "date_of_birth", "message", "subject", "custom"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Review & Map Fields",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dataset Name Input
        OutlinedTextField(
            value = datasetName,
            onValueChange = { datasetName = it },
            label = { Text("Dataset Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Detected ${analysisResult.validRecordCount} records (${analysisResult.skippedLinesCount} skipped lines)",
            fontSize = 13.sp,
            color = SuccessGreen,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Columns List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(analysisResult.detectedColumns) { col ->
                var showTypeMenu by remember { mutableStateOf(false) }
                val currentType = columnMappings[col.rawKey] ?: col.normalizedType
                val isIgnored = ignoredColumns[col.rawKey] == true

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isIgnored) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = col.rawKey,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isIgnored) Color.Gray else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sample: ${col.sampleValues.firstOrNull() ?: "(empty)"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Type dropdown
                        Box {
                            Button(
                                onClick = { showTypeMenu = true },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isIgnored) Color.Gray else BrandOrange)
                            ) {
                                Text(if (isIgnored) "Ignored" else currentType, fontSize = 12.sp)
                            }
                            DropdownMenu(
                                expanded = showTypeMenu,
                                onDismissRequest = { showTypeMenu = false }
                            ) {
                                commonTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            columnMappings[col.rawKey] = type
                                            ignoredColumns[col.rawKey] = false
                                            showTypeMenu = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Ignore Column", color = Color.Red) },
                                    onClick = {
                                        ignoredColumns[col.rawKey] = true
                                        showTypeMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val finalMapped = analysisResult.records.map { rawMap ->
                        val mapped = mutableMapOf<String, String>()
                        rawMap.forEach { (k, v) ->
                            if (ignoredColumns[k] != true) {
                                val targetType = columnMappings[k] ?: k
                                mapped[targetType] = v
                            }
                        }
                        AutomationFileAnalyzer.deriveMissingFields(mapped)
                        mapped
                    }
                    onConfirmSave(if (datasetName.isNotBlank()) datasetName else "Dataset", finalMapped)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) {
                Text("Save Dataset")
            }
        }
    }
}
