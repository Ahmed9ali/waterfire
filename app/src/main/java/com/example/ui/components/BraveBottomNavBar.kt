package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.R
import androidx.compose.material3.MaterialTheme
import com.example.ui.theme.MaterialTheme.colorScheme.primary

@Composable
fun BraveBottomNavBar(
    tabCount: Int,
    isDarkTheme: Boolean,
    isHomePage: Boolean,
    isCurrentPageBookmarked: Boolean,
    showBookmarkPopup: Boolean,
    showTabsPopup: Boolean,
    onHomeClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    onBookmarkLongPress: () -> Unit,
    onTabsLongPress: () -> Unit,
    onDismissBookmarkPopup: () -> Unit,
    onDismissTabsPopup: () -> Unit,
    onAddOrEditBookmark: () -> Unit,
    onViewBookmarks: () -> Unit,
    onNewTab: () -> Unit,
    onCloseCurrentTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val barBg = MaterialTheme.colorScheme.surfaceContainer
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val iconTint = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barBg)
            .navigationBarsPadding()
    ) {
        // 1px top divider
        Divider(color = dividerColor, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home (default ripple so taps give visible feedback)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clickable(onClick = onHomeClick)
                    .testTag("brave_nav_home"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Home,
                    contentDescription = "Home",
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 2. Bookmark (Tap & Long-Press)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onBookmarkClick() },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onBookmarkLongPress()
                            }
                        )
                    }
                    .testTag("brave_nav_bookmark"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrentPageBookmarked) AppIcons.BookmarkCheck else AppIcons.Bookmark,
                    contentDescription = "Bookmarks",
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )

                // Bookmark Long-Press Popup
                if (showBookmarkPopup) {
                    BookmarkLongPressPopup(
                        isDarkTheme = isDarkTheme,
                        isHomePage = isHomePage,
                        isBookmarked = isCurrentPageBookmarked,
                        onAddOrEdit = onAddOrEditBookmark,
                        onViewBookmarks = onViewBookmarks,
                        onDismiss = onDismissBookmarkPopup
                    )
                }
            }

            // 3. Search
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clickable(onClick = onSearchClick)
                    .testTag("brave_nav_search"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Search,
                    contentDescription = "Search",
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 4. Tabs Counter (Tap & Long-Press)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onTabsClick() },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTabsLongPress()
                            }
                        )
                    }
                    .testTag("brave_nav_tabs"),
                contentAlignment = Alignment.Center
            ) {
                // Rounded square outline showing tab count
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(1.8.dp, iconTint, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tabCount > 99) "99+" else tabCount.toString(),
                        color = iconTint,
                        fontSize = if (tabCount > 99) 8.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Tabs Long-Press Popup
                if (showTabsPopup) {
                    TabsLongPressPopup(
                        isDarkTheme = isDarkTheme,
                        onNewTab = onNewTab,
                        onCloseTab = onCloseCurrentTab,
                        onNewPrivateTab = onNewPrivateTab,
                        onDismiss = onDismissTabsPopup
                    )
                }
            }

            // 5. Menu
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clickable(onClick = onMenuClick)
                    .testTag("brave_nav_menu"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.MoreVert,
                    contentDescription = "Menu",
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun BookmarkLongPressPopup(
    isDarkTheme: Boolean,
    isHomePage: Boolean,
    isBookmarked: Boolean,
    onAddOrEdit: () -> Unit,
    onViewBookmarks: () -> Unit,
    onDismiss: () -> Unit
) {
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textDisabled = textPrimary.copy(alpha = 0.38f)

    Popup(
        onDismissRequest = onDismiss
    ) {
        // Transparent dismiss overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-68).dp, x = (-48).dp)
                    .width(210.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // Add / Edit Bookmark
                    val addEnabled = !isHomePage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable(enabled = addEnabled) {
                                onAddOrEdit()
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = if (isBookmarked) R.drawable.ic_edit else R.drawable.ic_bookmark),
                            contentDescription = null,
                            tint = if (addEnabled) MaterialTheme.colorScheme.primary else textDisabled,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = if (isBookmarked) "Edit bookmark" else "Add bookmark",
                            color = if (addEnabled) textPrimary else textDisabled,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

                    // View Bookmarks
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable {
                                onViewBookmarks()
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder),
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "View bookmarks",
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabsLongPressPopup(
    isDarkTheme: Boolean,
    onNewTab: () -> Unit,
    onCloseTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onDismiss: () -> Unit
) {
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val textPrimary = MaterialTheme.colorScheme.onSurface

    Popup(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-68).dp, x = 48.dp)
                    .width(210.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // New Tab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable {
                                onNewTab()
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "New tab",
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Close Tab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable {
                                onCloseTab()
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Close tab",
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

                    // New Private Tab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable {
                                onNewPrivateTab()
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shield),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "New Private tab",
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
