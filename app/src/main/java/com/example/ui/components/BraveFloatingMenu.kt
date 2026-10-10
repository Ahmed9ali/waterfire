package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BrandOrange

@Composable
fun BraveFloatingMenu(
    visible: Boolean,
    isDarkTheme: Boolean,
    isHomePage: Boolean,
    isLoading: Boolean,
    canGoForward: Boolean,
    isDesktopMode: Boolean,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onBookmarks: () -> Unit,
    onRecentTabs: () -> Unit,
    onAutomation: () -> Unit,
    onPasswords: () -> Unit,
    onFindInPage: () -> Unit,
    onToggleDesktopSite: () -> Unit,
    onSettings: () -> Unit,
    onForward: () -> Unit,
    onShare: () -> Unit,
    onDownloadPage: () -> Unit,
    onReloadOrStop: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val menuBg = if (isDarkTheme) Color(0xFF3A3A3E) else Color.White
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1E1E22)
    val dividerColor = if (isDarkTheme) Color(0xFF4A4A4F) else Color(0xFFEEEEEE)
    val circularBtnBg = if (isDarkTheme) Color(0x33FFFFFF) else Color(0x1F000000)

    // Full-screen overlay with 30% dark scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.30f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                animationSpec = tween(150),
                transformOrigin = TransformOrigin(0.95f, 0.95f)
            ) + fadeIn(animationSpec = tween(150)),
            exit = scaleOut(
                animationSpec = tween(150),
                transformOrigin = TransformOrigin(0.95f, 0.95f)
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier.padding(end = 8.dp, bottom = 64.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = menuBg,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .fillMaxHeight(0.82f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume click inside
                    )
                    .testTag("brave_floating_menu_card")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Scrollable list items
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp)
                        ) {
                            // Group 1: Tabs
                            MenuItemRow(
                                title = "New tab",
                                iconRes = R.drawable.ic_add,
                                textColor = textPrimary,
                                onClick = { onDismiss(); onNewTab() }
                            )
                            MenuItemRow(
                                title = "New Private tab",
                                iconRes = R.drawable.ic_shield,
                                textColor = textPrimary,
                                iconTint = BrandOrange,
                                onClick = { onDismiss(); onNewPrivateTab() }
                            )

                            MenuDivider(dividerColor)

                            // Group 2: Library & Recent
                            MenuItemRow(
                                title = "History",
                                iconRes = R.drawable.ic_history,
                                textColor = textPrimary,
                                onClick = { onDismiss(); onHistory() }
                            )
                            MenuItemRow(
                                title = "Downloads",
                                iconRes = R.drawable.ic_download,
                                textColor = textPrimary,
                                onClick = { onDismiss(); onDownloads() }
                            )
                            MenuItemRow(
                                title = "Bookmarks",
                                iconRes = R.drawable.ic_bookmark,
                                textColor = textPrimary,
                                onClick = { onDismiss(); onBookmarks() }
                            )
                            MenuItemRow(
                                title = "Recent tabs",
                                iconRes = R.drawable.ic_refresh,
                                textColor = textPrimary,
                                onClick = { onDismiss(); onRecentTabs() }
                            )

                            MenuDivider(dividerColor)

                            // Group 3: Features & Page Actions
                            MenuItemRow(
                                title = "Automation",
                                iconRes = R.drawable.ic_automation,
                                textColor = textPrimary,
                                iconTint = BrandOrange,
                                onClick = { onDismiss(); onAutomation() }
                            )
                            MenuItemRow(
                                title = "Passwords",
                                iconRes = R.drawable.ic_key,
                                textColor = textPrimary,
                                onClick = { onDismiss(); onPasswords() }
                            )
                            MenuItemRow(
                                title = "Find in page",
                                iconRes = R.drawable.ic_search,
                                textColor = textPrimary,
                                onClick = { onDismiss(); onFindInPage() }
                            )
                            // Desktop site with checkbox
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable {
                                        onDismiss()
                                        onToggleDesktopSite()
                                    }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_desktop),
                                    contentDescription = null,
                                    tint = textPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Desktop site",
                                    fontSize = 17.sp,
                                    color = textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Checkbox(
                                    checked = isDesktopMode,
                                    onCheckedChange = {
                                        onDismiss()
                                        onToggleDesktopSite()
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = BrandOrange,
                                        checkmarkColor = Color.White
                                    )
                                )
                            }

                            MenuDivider(dividerColor)

                            // Group 4: Settings
                            MenuItemRow(
                                title = "Settings",
                                iconRes = R.drawable.ic_settings,
                                textColor = textPrimary,
                                onClick = { onDismiss(); onSettings() }
                            )

                            // Space at bottom so list scrolls freely above pinned buttons
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Subtle bottom fade shadow
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, menuBg)
                                    )
                                )
                        )
                    }

                    // Pinned bottom row of four circular 56dp buttons
                    Divider(color = dividerColor, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Forward
                        val forwardEnabled = canGoForward && !isHomePage
                        CircularMenuButton(
                            iconRes = R.drawable.ic_arrow_forward,
                            enabled = forwardEnabled,
                            bgColor = circularBtnBg,
                            iconTint = textPrimary,
                            onClick = { onDismiss(); onForward() }
                        )

                        // 2. Share
                        val shareEnabled = !isHomePage
                        CircularMenuButton(
                            iconRes = R.drawable.ic_share,
                            enabled = shareEnabled,
                            bgColor = circularBtnBg,
                            iconTint = textPrimary,
                            onClick = { onDismiss(); onShare() }
                        )

                        // 3. Download (Offline web archive)
                        val downloadEnabled = !isHomePage
                        CircularMenuButton(
                            iconRes = R.drawable.ic_download,
                            enabled = downloadEnabled,
                            bgColor = circularBtnBg,
                            iconTint = textPrimary,
                            onClick = { onDismiss(); onDownloadPage() }
                        )

                        // 4. Reload / Stop
                        val reloadEnabled = !isHomePage
                        CircularMenuButton(
                            iconRes = if (isLoading) R.drawable.ic_close else R.drawable.ic_refresh,
                            enabled = reloadEnabled,
                            bgColor = circularBtnBg,
                            iconTint = textPrimary,
                            onClick = { onDismiss(); onReloadOrStop() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemRow(
    title: String,
    iconRes: Int,
    textColor: Color,
    iconTint: Color = textColor,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            color = textColor,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun MenuDivider(color: Color) {
    Divider(
        color = color,
        thickness = 0.5.dp,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)
    )
}

@Composable
private fun CircularMenuButton(
    iconRes: Int,
    enabled: Boolean,
    bgColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.40f
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bgColor.copy(alpha = if (enabled) bgColor.alpha else bgColor.alpha * 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconTint.copy(alpha = alpha),
            modifier = Modifier.size(20.dp)
        )
    }
}
