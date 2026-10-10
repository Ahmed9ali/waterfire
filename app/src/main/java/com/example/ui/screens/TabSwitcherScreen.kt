package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.browser.Tab
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.BrandOrangeDark
import com.example.ui.theme.PrivateAccent
import com.example.ui.theme.PrivateBackground
import com.example.ui.theme.PrivateSurface
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BrowserViewModel

@Composable
fun TabSwitcherScreen(
    viewModel: BrowserViewModel,
    onTabSelected: (String) -> Unit
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.BROWSER)
    }

    val normalTabs by viewModel.tabManager.normalTabs.collectAsState()
    val privateTabs by viewModel.tabManager.privateTabs.collectAsState()
    val currentTabId by viewModel.tabManager.currentTabId.collectAsState()
    val isPrivateMode by viewModel.tabManager.isPrivateMode.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val blockedCount7Days by viewModel.blockedTrackers7DaysCount.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    val activeTabsList = if (isPrivateMode) privateTabs else normalTabs
    val accentColor = if (isPrivateMode) PrivateAccent else BrandOrange

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isPrivateMode) PrivateBackground else MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.BROWSER) },
                modifier = Modifier.testTag("tab_switcher_back")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back to browser",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Normal / Private Toggle Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    // Normal Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (!isPrivateMode) BrandOrange else Color.Transparent)
                            .clickable { viewModel.tabManager.setPrivateMode(false) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("normal_tabs_tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${normalTabs.size}",
                            color = if (!isPrivateMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Private Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isPrivateMode) PrivateAccent else Color.Transparent)
                            .clickable { viewModel.tabManager.setPrivateMode(true) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("private_tabs_tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_shield),
                                contentDescription = null,
                                tint = if (isPrivateMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${privateTabs.size}",
                                color = if (isPrivateMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Fire Button
            IconButton(
                onClick = { viewModel.showFireDialog.value = true },
                modifier = Modifier.testTag("tab_switcher_fire_button")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_fire),
                    contentDescription = "Fire",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }

            // New Tab (+)
            IconButton(
                onClick = {
                    viewModel.tabManager.createNewTab(isPrivate = isPrivateMode, initialUrl = "")
                    viewModel.navigateTo(AppScreen.BROWSER)
                },
                modifier = Modifier.testTag("tab_switcher_new_tab")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "New Tab",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Overflow Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vert),
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Close all tabs") },
                        onClick = {
                            showMenu = false
                            viewModel.tabManager.closeAllTabs(isPrivateOnly = isPrivateMode)
                        }
                    )
                }
            }
        }

        // Tracker Protection Banner (Dismissible)
        if (!settings.trackerBannerDismissed && blockedCount7Days > 0) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = BrandOrangeDark.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandOrange.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shield_protected),
                        contentDescription = null,
                        tint = BrandOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$blockedCount7Days trackers blocked in the last 7 days",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.dismissTrackerBanner() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2-Column Grid of Tab Cards
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(activeTabsList, key = { it.id }) { tab ->
                val isSelected = tab.id == currentTabId

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.tabManager.selectTab(tab.id)
                            viewModel.navigateTo(AppScreen.BROWSER)
                        }
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .testTag("tab_card_${tab.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPrivateMode) PrivateSurface else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Tab Card Header (Title & Close Button)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (tab.isPrivate) R.drawable.ic_shield else R.drawable.ic_search
                                ),
                                contentDescription = null,
                                tint = if (tab.isPrivate) PrivateAccent else BrandOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (tab.title.isNotBlank()) tab.title else "New Tab",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.tabManager.closeTab(tab.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Close tab",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Tab Thumbnail Preview (Captured Bitmap or Sleek Placeholder)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.85f)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (tab.thumbnail != null) {
                                Image(
                                    bitmap = tab.thumbnail!!.asImageBitmap(),
                                    contentDescription = "Tab preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_duckduckgo),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (tab.isPrivate) "Private Tab" else "WaterFox",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
