package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.AddressBarPosition
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BrowserViewModel

@Composable
fun SettingsScreen(viewModel: BrowserViewModel) {
    val context = LocalContext.current

    BackHandler {
        viewModel.navigateTo(AppScreen.BROWSER)
    }

    val settings by viewModel.settings.collectAsState()
    var showClearDataDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.BROWSER) },
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // GENERAL SECTION
            SettingsSectionHeader(title = "General")

            SettingsClickableItem(
                title = "Set as default browser",
                subtitle = "Make WaterFox your default browser for links",
                icon = R.drawable.ic_desktop,
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        viewModel.snackbarMessage.value = "Default browser settings can be set in Android Settings."
                    }
                }
            )

            SettingsClickableItem(
                title = "Automation",
                subtitle = "Smart form filler driven by uploaded datasets",
                icon = R.drawable.ic_automation,
                onClick = {
                    viewModel.navigateTo(AppScreen.AUTOMATION)
                }
            )

            // Theme selector
            var showThemeMenu by remember { mutableStateOf(false) }
            Box {
                SettingsClickableItem(
                    title = "Theme",
                    subtitle = when (settings.themeMode) {
                        ThemeMode.DARK -> "Dark (Default)"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.SYSTEM -> "Follow System"
                    },
                    icon = R.drawable.ic_settings,
                    onClick = { showThemeMenu = true }
                )
                DropdownMenu(
                    expanded = showThemeMenu,
                    onDismissRequest = { showThemeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Dark (Default)") },
                        onClick = {
                            viewModel.updateTheme(ThemeMode.DARK)
                            showThemeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Light") },
                        onClick = {
                            viewModel.updateTheme(ThemeMode.LIGHT)
                            showThemeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Follow System") },
                        onClick = {
                            viewModel.updateTheme(ThemeMode.SYSTEM)
                            showThemeMenu = false
                        }
                    )
                }
            }

            // Address bar position
            var showPositionMenu by remember { mutableStateOf(false) }
            Box {
                SettingsClickableItem(
                    title = "Address bar position",
                    subtitle = if (settings.addressBarPosition == AddressBarPosition.TOP) "Top" else "Bottom",
                    icon = R.drawable.ic_settings,
                    onClick = { showPositionMenu = true }
                )
                DropdownMenu(
                    expanded = showPositionMenu,
                    onDismissRequest = { showPositionMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Top") },
                        onClick = {
                            viewModel.updateAddressBarPosition(AddressBarPosition.TOP)
                            showPositionMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Bottom") },
                        onClick = {
                            viewModel.updateAddressBarPosition(AddressBarPosition.BOTTOM)
                            showPositionMenu = false
                        }
                    )
                }
            }

            // Text size slider
            var sliderValue by remember(settings.textSizePercent) { mutableFloatStateOf(settings.textSizePercent.toFloat()) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Web text size",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${sliderValue.toInt()}%",
                        fontSize = 14.sp,
                        color = BrandOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        viewModel.updateTextSizePercent(sliderValue.toInt())
                    },
                    valueRange = 50f..200f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = BrandOrange,
                        activeTrackColor = BrandOrange
                    )
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // PRIVACY SECTION
            SettingsSectionHeader(title = "Privacy & Security")

            SettingsSwitchItem(
                title = "Tracker blocking",
                subtitle = "Block third-party advertising and analytics trackers",
                checked = settings.trackerBlockingEnabled,
                onCheckedChange = { viewModel.updateTrackerBlocking(it) }
            )

            SettingsSwitchItem(
                title = "Automatic HTTPS upgrade",
                subtitle = "Automatically load secure HTTPS versions of websites",
                checked = settings.httpsUpgradeEnabled,
                onCheckedChange = { viewModel.updateHttpsUpgrade(it) }
            )

            SettingsSwitchItem(
                title = "Block third-party cookies",
                subtitle = "Prevent cross-site cookie tracking",
                checked = settings.blockThirdPartyCookies,
                onCheckedChange = { viewModel.updateBlockThirdPartyCookies(it) }
            )

            SettingsSwitchItem(
                title = "Clear data on exit",
                subtitle = "Clear tabs, cookies, and cache when app closes",
                checked = settings.clearDataOnExit,
                onCheckedChange = { viewModel.updateClearOnExit(it) }
            )

            // Auto-clear timeout dropdown
            var showTimeoutMenu by remember { mutableStateOf(false) }
            val timeoutLabel = when (settings.autoClearTimeoutMinutes) {
                0 -> "Never"
                5 -> "After 5 minutes"
                15 -> "After 15 minutes"
                30 -> "After 30 minutes"
                60 -> "After 1 hour"
                1440 -> "After 1 day"
                else -> "Never"
            }
            Box {
                SettingsClickableItem(
                    title = "Automatically clear in background",
                    subtitle = timeoutLabel,
                    icon = R.drawable.ic_fire,
                    onClick = { showTimeoutMenu = true }
                )
                DropdownMenu(
                    expanded = showTimeoutMenu,
                    onDismissRequest = { showTimeoutMenu = false }
                ) {
                    val options = listOf(0 to "Never", 5 to "After 5 minutes", 15 to "After 15 minutes", 30 to "After 30 minutes", 60 to "After 1 hour", 1440 to "After 1 day")
                    options.forEach { (mins, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.updateAutoClearTimeout(mins)
                                showTimeoutMenu = false
                            }
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // BROWSING SECTION
            SettingsSectionHeader(title = "Browsing")

            SettingsSwitchItem(
                title = "JavaScript",
                subtitle = "Enable JavaScript execution on websites",
                checked = settings.javascriptEnabled,
                onCheckedChange = { viewModel.updateJavascriptEnabled(it) }
            )

            SettingsSwitchItem(
                title = "Request desktop site by default",
                subtitle = "Always request full desktop version of web pages",
                checked = settings.desktopSiteDefault,
                onCheckedChange = { viewModel.updateDesktopSiteDefault(it) }
            )

            SettingsSwitchItem(
                title = "Open links in background",
                subtitle = "When a link opens a new tab, stay on current tab",
                checked = settings.openLinksInBackground,
                onCheckedChange = { viewModel.updateOpenLinksInBackground(it) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // DATA MANAGEMENT
            SettingsSectionHeader(title = "Data Management")

            SettingsClickableItem(
                title = "Clear browsing data",
                subtitle = "History, cookies, cached files, open tabs",
                icon = R.drawable.ic_delete,
                onClick = { showClearDataDialog = true }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // ABOUT
            SettingsSectionHeader(title = "About WaterFox")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "WaterFox Browser v1.0.0",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A private, fast Android browser inspired by DuckDuckGo Privacy Browser. Fully local, zero telemetry, tracker protection powered by 300+ domain blocklists.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Clear Browsing Data Dialog
    if (showClearDataDialog) {
        var clearHistory by remember { mutableStateOf(true) }
        var clearCookies by remember { mutableStateOf(true) }
        var clearCache by remember { mutableStateOf(true) }
        var clearTabs by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear browsing data", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearHistory, onCheckedChange = { clearHistory = it }, colors = CheckboxDefaults.colors(checkedColor = BrandOrange))
                        Text("Browsing history", fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearCookies, onCheckedChange = { clearCookies = it }, colors = CheckboxDefaults.colors(checkedColor = BrandOrange))
                        Text("Cookies and site data", fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearCache, onCheckedChange = { clearCache = it }, colors = CheckboxDefaults.colors(checkedColor = BrandOrange))
                        Text("Cached images and files", fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearTabs, onCheckedChange = { clearTabs = it }, colors = CheckboxDefaults.colors(checkedColor = BrandOrange))
                        Text("Close all open tabs", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearBrowsingData(clearHistory, clearCookies, clearCache, clearTabs)
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = BrandOrange,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BrandOrange,
                checkedTrackColor = BrandOrange.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
