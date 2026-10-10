package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BrandOrange

@Composable
fun HamburgerMenuSheet(
    visible: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isDesktopMode: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onNewTabClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onPasswordsClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onAutomationClick: () -> Unit,
    onAddBookmarkClick: () -> Unit,
    onAddToFavoritesClick: () -> Unit,
    onShareClick: () -> Unit,
    onFindInPageClick: () -> Unit,
    onRequestDesktopSiteClick: () -> Unit,
    onPrintPageClick: () -> Unit,
    onAddToHomeScreenClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onReportBrokenSiteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    CustomBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top quick-action row: Back, Forward, New Tab, Settings
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onDismiss()
                            onBackClick()
                        },
                        enabled = canGoBack,
                        modifier = Modifier.testTag("menu_back_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }

                    IconButton(
                        onClick = {
                            onDismiss()
                            onForwardClick()
                        },
                        enabled = canGoForward,
                        modifier = Modifier.testTag("menu_forward_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_forward),
                            contentDescription = "Forward",
                            tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }

                    IconButton(
                        onClick = {
                            onDismiss()
                            onNewTabClick()
                        },
                        modifier = Modifier.testTag("menu_new_tab_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = "New Tab",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            onDismiss()
                            onSettingsClick()
                        },
                        modifier = Modifier.testTag("menu_settings_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation List: Bookmarks, Passwords, Downloads, History
            MenuItem(icon = R.drawable.ic_bookmark, title = "Bookmarks") {
                onDismiss(); onBookmarksClick()
            }
            MenuItem(icon = R.drawable.ic_key, title = "Passwords") {
                onDismiss(); onPasswordsClick()
            }
            MenuItem(icon = R.drawable.ic_download, title = "Downloads") {
                onDismiss(); onDownloadsClick()
            }
            MenuItem(icon = R.drawable.ic_history, title = "History") {
                onDismiss(); onHistoryClick()
            }
            MenuItem(icon = R.drawable.ic_automation, title = "Automation", tint = BrandOrange) {
                onDismiss(); onAutomationClick()
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Page Actions
            MenuItem(icon = R.drawable.ic_bookmark, title = "Add Bookmark") {
                onDismiss(); onAddBookmarkClick()
            }
            MenuItem(icon = R.drawable.ic_star, title = "Add to Favorites") {
                onDismiss(); onAddToFavoritesClick()
            }
            MenuItem(icon = R.drawable.ic_share, title = "Share") {
                onDismiss(); onShareClick()
            }
            MenuItem(icon = R.drawable.ic_search, title = "Find in Page") {
                onDismiss(); onFindInPageClick()
            }
            MenuItem(
                icon = R.drawable.ic_desktop,
                title = if (isDesktopMode) "Request Mobile Site" else "Request Desktop Site"
            ) {
                onDismiss(); onRequestDesktopSiteClick()
            }
            MenuItem(icon = R.drawable.ic_print, title = "Print Page") {
                onDismiss(); onPrintPageClick()
            }
            MenuItem(icon = R.drawable.ic_add, title = "Add to Home Screen") {
                onDismiss(); onAddToHomeScreenClick()
            }
            MenuItem(icon = R.drawable.ic_edit, title = "Copy Link") {
                onDismiss(); onCopyLinkClick()
            }
            MenuItem(icon = R.drawable.ic_refresh, title = "Refresh") {
                onDismiss(); onRefreshClick()
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            MenuItem(
                icon = R.drawable.ic_warning,
                title = "Report broken site",
                tint = BrandOrange
            ) {
                onDismiss(); onReportBrokenSiteClick()
            }
        }
    }
}

@Composable
private fun MenuItem(
    icon: Int,
    title: String,
    tint: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = tint ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
