package com.example.ui.screens

import android.content.ClipData
import androidx.compose.material3.MaterialTheme
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.R
import com.example.browser.Tab
import com.example.data.database.FavoriteEntity
import com.example.data.preferences.AddressBarPosition
import com.example.ui.components.AutomationQuickFillSheet
import com.example.ui.components.BraveBottomNavBar
import com.example.ui.components.BraveFloatingMenu
import com.example.ui.components.FireDialog
import com.example.ui.components.LinkContextMenuSheet
import com.example.ui.components.Omnibar
import com.example.ui.components.PrivacyShieldSheet
import com.example.ui.components.SearchSuggestionsList
import com.example.ui.components.WallpaperBackground
import com.example.ui.components.rememberFavicon
import com.example.ui.theme.MaterialTheme.colorScheme.primary
import com.example.ui.theme.PrivateAccent
import com.example.ui.theme.PrivateBackground
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BrowserViewModel

@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val context = LocalContext.current
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()

    // Height of the always-visible bottom bar. Used so overlays (suggestions, find bar)
    // only lift by the part of the keyboard that is NOT already covered by the bar.
    val bottomBarInsets = WindowInsets.navigationBars.add(WindowInsets(bottom = 57.dp))

    BackHandler {
        viewModel.handleBackPress()
    }

    val normalTabs by viewModel.tabManager.normalTabs.collectAsState()
    val privateTabs by viewModel.tabManager.privateTabs.collectAsState()
    val currentTabId by viewModel.tabManager.currentTabId.collectAsState()
    val isPrivateMode by viewModel.tabManager.isPrivateMode.collectAsState()
    // Observed so the screen recomposes whenever a tab's fields change in place
    // (showHome, isLoading, progress, url, title, ...). Without this the Home button
    // and the loading bar never redraw.
    val tabsVersion by viewModel.tabManager.tabsVersion.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val currentTab = remember(tabsVersion, currentTabId, isPrivateMode, normalTabs, privateTabs) {
        viewModel.tabManager.getCurrentTab()
    }

    val totalTabsCount = if (isPrivateMode) privateTabs.size else normalTabs.size
    val omnibarText by viewModel.omnibarInput.collectAsState()
    val isOmnibarFocused by viewModel.isOmnibarFocused.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val searchTriggerEvent by viewModel.searchTriggerEvent.collectAsState()

    val showFloatingMenu by viewModel.showHamburgerMenu.collectAsState()
    val showFireDialog by viewModel.showFireDialog.collectAsState()
    val showShieldSheet by viewModel.showShieldSheet.collectAsState()
    val showFindInPage by viewModel.showFindInPage.collectAsState()
    val findQuery by viewModel.findQuery.collectAsState()
    val findResultText by viewModel.findResultText.collectAsState()
    val showAddFavoriteDialog by viewModel.showAddFavoriteDialog.collectAsState()
    val showRecentTabsDialog by viewModel.showRecentTabsDialog.collectAsState()
    val recentlyClosedTabs by viewModel.tabManager.recentlyClosedTabs.collectAsState()

    val showBookmarkPopup by viewModel.showBookmarkLongPressPopup.collectAsState()
    val showTabsPopup by viewModel.showTabsLongPressPopup.collectAsState()

    val linkContextMenuState by viewModel.tabManager.linkContextMenuState.collectAsState()

    // Favorite selected for long-press popup
    var favoriteForPopup by remember { mutableStateOf<FavoriteEntity?>(null) }
    var favoriteToEdit by remember { mutableStateOf<FavoriteEntity?>(null) }

    // Resolve the theme the same way WaterFoxTheme does (SYSTEM follows the phone setting).
    // Before, SYSTEM counted as dark here but as light in MaterialTheme, so cards and
    // dialogs came out light on a dark screen.
    val isDarkTheme = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val isBlankOrHome = currentTab == null || currentTab.showHome || currentTab.url.isBlank() || currentTab.url.startsWith("about:")

    // Detect if address bar / keyboard editing is active to hide bottom navigation bar
    val isKeyboardVisible = isOmnibarFocused

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isPrivateMode) PrivateBackground else MaterialTheme.colorScheme.background)
    ) {
        // Wallpaper is drawn once at full-screen size behind the (opaque) bars, so it
        // never moves when the keyboard opens or the page area changes size.
        if (isBlankOrHome) {
            WallpaperBackground(
                mode = settings.wallpaperMode,
                solidColorHex = settings.wallpaperSolidColor,
                customImagePath = settings.customWallpaperPath
            )
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar (Status bar padding + single row Omnibar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .statusBarsPadding()
            ) {
                val currentDetection by viewModel.automationManager.currentFormDetection.collectAsState()
                val autoSettings by viewModel.automationManager.settings.collectAsState()
                val hasFormFields = autoSettings.showOmnibarIcon && currentDetection != null && (currentDetection?.detectedFieldCount ?: 0) > 0

                Omnibar(
                    url = currentTab?.url ?: "",
                    isLoading = currentTab?.isLoading == true,
                    progress = currentTab?.progress ?: 0,
                    isHttps = currentTab?.isHttps == true,
                    hasSslError = currentTab?.hasSslError == true,
                    isDarkTheme = isDarkTheme,
                    isHomePage = isBlankOrHome,
                    inputText = omnibarText,
                    isFocused = isOmnibarFocused,
                    searchTriggerTimestamp = searchTriggerEvent,
                    onInputChanged = { viewModel.onOmnibarInputChanged(it) },
                    onFocusChanged = { viewModel.isOmnibarFocused.value = it },
                    onSubmit = { viewModel.submitUrl(it) },
                    onFireClick = { viewModel.showFireDialog.value = true },
                    hasAutomationFields = hasFormFields,
                    automationFieldCount = currentDetection?.detectedFieldCount ?: 0,
                    onAutomationClick = { viewModel.executeAutomationFill() },
                    onAutomationLongClick = { viewModel.showAutomationQuickFillSheet.value = true }
                )
            }

            // Web Content or Brave-style Home Page
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isBlankOrHome) {
                    // Brave-style Native Compose Home Page (Full-bleed wallpaper, Shortcut tray, Brand block)
                    BraveHomePage(
                        viewModel = viewModel,
                        isPrivateMode = isPrivateMode,
                        isDarkTheme = isDarkTheme,
                        onFavoriteLongPress = { fav -> favoriteForPopup = fav },
                        onAddFavorite = { viewModel.showAddFavoriteDialog.value = true }
                    )
                } else {
                    // Active WebView with SwipeRefreshLayout or Crash Fallback
                    currentTab?.let { tab ->
                        if (tab.hasCrashError) {
                            PageCrashView(
                                tab = tab,
                                onReload = { viewModel.tabManager.reloadCurrentTab() },
                                onGoHome = { viewModel.tabManager.loadUrlInTab(tab, "") }
                            )
                        } else {
                            key(tab.id) {
                                AndroidView(
                                    factory = { ctx ->
                                        val swipeRefreshLayout = SwipeRefreshLayout(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            setColorSchemeColors(primaryArgb)
                                            setOnRefreshListener {
                                                try {
                                                    tab.webView?.reload()
                                                } catch (e: Exception) {
                                                    isRefreshing = false
                                                }
                                            }
                                        }
                                        val webView = viewModel.tabManager.getOrCreateWebView(tab, ctx)
                                        (webView.parent as? ViewGroup)?.removeView(webView)
                                        swipeRefreshLayout.addView(
                                            webView,
                                            ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        )
                                        swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
                                            try {
                                                webView.scrollY > 0
                                            } catch (e: Exception) {
                                                false
                                            }
                                        }
                                        swipeRefreshLayout
                                    },
                                    update = { swipeRefreshLayout ->
                                        val webView = viewModel.tabManager.getOrCreateWebView(tab, swipeRefreshLayout.context)
                                        if (webView.parent != swipeRefreshLayout) {
                                            (webView.parent as? ViewGroup)?.removeView(webView)
                                            for (i in swipeRefreshLayout.childCount - 1 downTo 0) {
                                                val child = swipeRefreshLayout.getChildAt(i)
                                                if (child is WebView && child != webView) {
                                                    swipeRefreshLayout.removeViewAt(i)
                                                }
                                            }
                                            swipeRefreshLayout.addView(
                                                webView,
                                                0,
                                                ViewGroup.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                            )
                                        }
                                        swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
                                            try {
                                                webView.scrollY > 0
                                            } catch (e: Exception) {
                                                false
                                            }
                                        }
                                        if (!tab.isLoading) {
                                            swipeRefreshLayout.isRefreshing = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Suggestions / recents: fill the whole area under the address bar (Brave style)
                if (isOmnibarFocused && suggestions.isNotEmpty()) {
                    SearchSuggestionsList(
                        suggestions = suggestions,
                        isDarkTheme = isDarkTheme,
                        onSubmit = { viewModel.submitUrl(it) },
                        onCopy = { viewModel.onOmnibarInputChanged(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.ime.exclude(bottomBarInsets))
                    )
                }

                // Find in Page Bar at Bottom of page area
                if (showFindInPage) {
                    FindInPageBar(
                        query = findQuery,
                        resultText = findResultText,
                        onQueryChange = { viewModel.updateFindQuery(it) },
                        onPrevClick = { viewModel.findNext(false) },
                        onNextClick = { viewModel.findNext(true) },
                        onCloseClick = { viewModel.closeFindInPage() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.ime.exclude(bottomBarInsets))
                    )
                }
            }

            // Brave Bottom Navigation Bar (always visible; the keyboard simply covers it)
            val isBookmarked = currentTab != null && viewModel.isPageBookmarked(currentTab.url)
            BraveBottomNavBar(
                tabCount = totalTabsCount,
                isDarkTheme = isDarkTheme,
                isHomePage = isBlankOrHome,
                isCurrentPageBookmarked = isBookmarked,
                showBookmarkPopup = showBookmarkPopup,
                showTabsPopup = showTabsPopup,
                onHomeClick = {
                    viewModel.tabManager.showHomeInCurrentTab()
                },
                onBookmarkClick = {
                    viewModel.navigateTo(AppScreen.BOOKMARKS)
                },
                onSearchClick = {
                    viewModel.triggerSearchFromBottomBar()
                },
                onTabsClick = {
                    viewModel.navigateTo(AppScreen.TAB_SWITCHER)
                },
                onMenuClick = {
                    viewModel.showHamburgerMenu.value = true
                },
                onBookmarkLongPress = {
                    viewModel.showBookmarkLongPressPopup.value = true
                },
                onTabsLongPress = {
                    viewModel.showTabsLongPressPopup.value = true
                },
                onDismissBookmarkPopup = {
                    viewModel.showBookmarkLongPressPopup.value = false
                },
                onDismissTabsPopup = {
                    viewModel.showTabsLongPressPopup.value = false
                },
                onAddOrEditBookmark = {
                    val tab = currentTab
                    if (tab != null && tab.url.isNotBlank()) {
                        val existing = viewModel.getBookmarkForUrl(tab.url)
                        if (existing != null) {
                            // Already bookmarked, edit it
                            viewModel.editBookmark(existing.copy(title = if (tab.title.isNotBlank()) tab.title else existing.title))
                        } else {
                            viewModel.addBookmark(title = if (tab.title.isNotBlank()) tab.title else tab.url, url = tab.url)
                        }
                    }
                },
                onViewBookmarks = {
                    viewModel.navigateTo(AppScreen.BOOKMARKS)
                },
                onNewTab = {
                    viewModel.tabManager.createNewTab(isPrivate = isPrivateMode)
                },
                onCloseCurrentTab = {
                    currentTab?.let { viewModel.tabManager.closeTab(it.id) }
                },
                onNewPrivateTab = {
                    viewModel.tabManager.createNewTab(isPrivate = true)
                }
            )
        }

        // Brave Floating Menu (Popup card anchored to bottom-right)
        BraveFloatingMenu(
            visible = showFloatingMenu,
            isDarkTheme = isDarkTheme,
            isHomePage = isBlankOrHome,
            isLoading = currentTab?.isLoading == true,
            canGoForward = currentTab?.canGoForward == true,
            isDesktopMode = currentTab?.isDesktopMode == true,
            onNewTab = { viewModel.tabManager.createNewTab(isPrivate = isPrivateMode) },
            onNewPrivateTab = { viewModel.tabManager.createNewTab(isPrivate = true) },
            onHistory = { viewModel.navigateTo(AppScreen.HISTORY) },
            onDownloads = {
                viewModel.refreshDownloads()
                viewModel.navigateTo(AppScreen.DOWNLOADS)
            },
            onBookmarks = { viewModel.navigateTo(AppScreen.BOOKMARKS) },
            onRecentTabs = {
                if (recentlyClosedTabs.isEmpty()) {
                    viewModel.snackbarMessage.value = "No recently closed tabs"
                } else {
                    viewModel.showRecentTabsDialog.value = true
                }
            },
            onAutomation = { viewModel.navigateTo(AppScreen.AUTOMATION) },
            onPasswords = { viewModel.navigateTo(AppScreen.PASSWORDS) },
            onFindInPage = { viewModel.startFindInPage() },
            onToggleDesktopSite = { viewModel.tabManager.toggleDesktopModeForCurrentTab() },
            onSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
            onForward = { viewModel.tabManager.goForwardCurrentTab() },
            onShare = {
                currentTab?.let { tab ->
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, tab.url)
                        putExtra(Intent.EXTRA_SUBJECT, tab.title)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share URL"))
                }
            },
            onDownloadPage = {
                viewModel.saveCurrentPageOffline()
            },
            onReloadOrStop = {
                if (currentTab?.isLoading == true) {
                    viewModel.tabManager.stopCurrentTab()
                } else {
                    viewModel.tabManager.reloadCurrentTab()
                }
            },
            onDismiss = { viewModel.showHamburgerMenu.value = false }
        )

        // Fire Dialog (Delete This Tab / Delete All)
        FireDialog(
            visible = showFireDialog,
            onDeleteThisTab = { viewModel.executeFireAction(onlyThisTab = true) },
            onDeleteAll = { viewModel.executeFireAction(onlyThisTab = false) },
            onDismiss = { viewModel.showFireDialog.value = false }
        )

        // Privacy Shield Sheet
        PrivacyShieldSheet(
            visible = showShieldSheet,
            pageUrl = currentTab?.url ?: "",
            blockedTrackers = currentTab?.blockedTrackersList ?: emptyList(),
            isProtectionEnabled = settings.trackerBlockingEnabled,
            onToggleProtection = { enabled -> viewModel.updateTrackerBlocking(enabled) },
            onDismiss = { viewModel.showShieldSheet.value = false }
        )

        // Link Long-press Context Menu
        LinkContextMenuSheet(
            state = linkContextMenuState,
            onOpenInNewTab = { url -> viewModel.tabManager.createNewTab(isPrivate = false, initialUrl = url) },
            onOpenInBackgroundTab = { url -> viewModel.tabManager.createNewTab(isPrivate = false, initialUrl = url, openInBackground = true) },
            onOpenInPrivateTab = { url -> viewModel.tabManager.createNewTab(isPrivate = true, initialUrl = url) },
            onCopyLink = { url ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("URL", url))
                viewModel.snackbarMessage.value = "Link copied"
            },
            onShareLink = { url ->
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, url)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Link"))
            },
            onDownloadImage = { imageUrl ->
                viewModel.downloadHandler.startDownload(
                    url = imageUrl,
                    userAgent = com.example.browser.TabManager.MOBILE_USER_AGENT,
                    contentDisposition = null,
                    mimeType = "image/*",
                    contentLength = 0L
                )
                viewModel.snackbarMessage.value = "Image download started"
            },
            onDismiss = { viewModel.tabManager.linkContextMenuState.value = null }
        )

        // Automation Quick Fill Sheet
        val showQuickFillSheet by viewModel.showAutomationQuickFillSheet.collectAsState()
        val pendingHttpWarning by viewModel.pendingHttpPasswordWarningRecord.collectAsState()
        val activeDataset = viewModel.automationManager.getActiveDataset()
        val allActiveRecords = remember(activeDataset) {
            activeDataset?.let { viewModel.automationManager.getRecordsForDataset(it.id) } ?: emptyList()
        }
        val activeRecord = remember(activeDataset, currentTab?.url) {
            val pageUrl = currentTab?.url ?: ""
            viewModel.automationManager.getCachedRecordForUrl(pageUrl)
                ?: (if (activeDataset != null) viewModel.automationManager.getNextRecordToFill(activeDataset, pageUrl).first else null)
        }

        AutomationQuickFillSheet(
            visible = showQuickFillSheet,
            dataset = activeDataset,
            records = allActiveRecords,
            activeRecord = activeRecord,
            pageUrl = currentTab?.url ?: "",
            onFillClick = {
                viewModel.showAutomationQuickFillSheet.value = false
                viewModel.executeAutomationFill(explicitRecord = activeRecord)
            },
            onNextRecordClick = {
                viewModel.advanceAutomationNextRecord()
            },
            onSelectRecord = { rec ->
                viewModel.executeAutomationFill(explicitRecord = rec)
                viewModel.showAutomationQuickFillSheet.value = false
            },
            onChangeMode = { mode -> viewModel.updateActiveDatasetMode(mode) },
            onDisableOnSite = { host -> viewModel.disableAutomationOnSite(host) },
            onOpenAutomationScreen = { viewModel.navigateTo(AppScreen.AUTOMATION) },
            onDismiss = { viewModel.showAutomationQuickFillSheet.value = false }
        )

        // Insecure HTTP Password Warning Dialog
        pendingHttpWarning?.let { rec ->
            AlertDialog(
                onDismissRequest = { viewModel.pendingHttpPasswordWarningRecord.value = null },
                title = { Text("Insecure Connection Warning") },
                text = {
                    Text("This web page is loaded over an unencrypted connection (HTTP). Sending passwords across HTTP can expose them to attackers on your network. Are you sure you want to fill this form?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.pendingHttpPasswordWarningRecord.value = null
                            viewModel.executeAutomationFill(explicitRecord = rec, skipHttpCheck = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Fill Anyway")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.pendingHttpPasswordWarningRecord.value = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Favorite Long-Press Popup (Open in new tab, Open in private tab, Edit, Remove)
        favoriteForPopup?.let { favorite ->
            Dialog(onDismissRequest = { favoriteForPopup = null }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(260.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = cleanFavoriteLabel(favorite.title, favorite.url),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

                        PopupRow("Open in new tab", R.drawable.ic_add, isDarkTheme) {
                            viewModel.tabManager.createNewTab(isPrivate = false, initialUrl = favorite.url)
                            favoriteForPopup = null
                        }
                        PopupRow("Open in private tab", R.drawable.ic_shield, isDarkTheme, tint = MaterialTheme.colorScheme.primary) {
                            viewModel.tabManager.createNewTab(isPrivate = true, initialUrl = favorite.url)
                            favoriteForPopup = null
                        }
                        PopupRow("Edit", R.drawable.ic_edit, isDarkTheme) {
                            favoriteToEdit = favorite
                            favoriteForPopup = null
                        }
                        PopupRow("Remove", R.drawable.ic_delete, isDarkTheme, tint = MaterialTheme.colorScheme.error) {
                            viewModel.deleteFavorite(favorite)
                            favoriteForPopup = null
                        }
                    }
                }
            }
        }

        // Edit Favorite Dialog
        favoriteToEdit?.let { favorite ->
            var editTitle by remember { mutableStateOf(favorite.title) }
            var editUrl by remember { mutableStateOf(favorite.url) }

            AlertDialog(
                onDismissRequest = { favoriteToEdit = null },
                title = { Text("Edit Favorite") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editUrl,
                            onValueChange = { editUrl = it },
                            label = { Text("URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editUrl.isNotBlank()) {
                                viewModel.editFavorite(favorite.copy(title = editTitle, url = editUrl))
                            }
                            favoriteToEdit = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { favoriteToEdit = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Add Favorite Dialog
        if (showAddFavoriteDialog) {
            var titleInput by remember { mutableStateOf("") }
            var urlInput by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { viewModel.showAddFavoriteDialog.value = false },
                title = { Text("Add Favorite") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("URL (e.g. example.com)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (urlInput.isNotBlank()) {
                                val target = if (urlInput.startsWith("http://") || urlInput.startsWith("https://")) urlInput else "https://$urlInput"
                                viewModel.addFavorite(title = if (titleInput.isNotBlank()) titleInput else target, url = target)
                                viewModel.showAddFavoriteDialog.value = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showAddFavoriteDialog.value = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Recent Tabs Dialog
        if (showRecentTabsDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.showRecentTabsDialog.value = false },
                title = { Text("Recently Closed Tabs") },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                        items(recentlyClosedTabs) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.tabManager.createNewTab(isPrivate = item.isPrivate, initialUrl = item.url)
                                        viewModel.showRecentTabsDialog.value = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = if (item.isPrivate) R.drawable.ic_shield else R.drawable.ic_history),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.url,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.showRecentTabsDialog.value = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
private fun PopupRow(
    title: String,
    iconRes: Int,
    isDarkTheme: Boolean,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Brave-style Native Compose Home Page
 * 1. Full-bleed Wallpaper behind content
 * 2. Shortcut tray at the top (translucent card, 64dp circles, clean host/title, + Add tile)
 * 3. Brand block near bottom (Original WaterFox logo, wordmark, "Private. Fast. Yours.")
 */
@Composable
private fun BraveHomePage(
    viewModel: BrowserViewModel,
    isPrivateMode: Boolean,
    isDarkTheme: Boolean,
    onFavoriteLongPress: (FavoriteEntity) -> Unit,
    onAddFavorite: () -> Unit
) {
    val favorites by viewModel.favoritesList.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 2. Shortcut Tray at Top of content (Rounded card 20dp, 40% translucent black, 8dp side margin)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(favorites, key = { it.id }) { favorite ->
                        FavoriteTile(
                            favorite = favorite,
                            isDarkTheme = isDarkTheme,
                            onTap = { viewModel.tabManager.loadUrl(favorite.url) },
                            onLongPress = { onFavoriteLongPress(favorite) }
                        )
                    }

                    // Last item: "+" Add Tile
                    item {
                        AddFavoriteTile(isDarkTheme = isDarkTheme, onClick = onAddFavorite)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 3. Brand Block near the bottom (above bottom bar)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                // Original WaterFox Logo (stylized fox head with water drop)
                Icon(
                    painter = painterResource(id = R.drawable.ic_waterfox_logo),
                    contentDescription = "WaterFox Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(68.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // "WaterFox" wordmark
                Text(
                    text = if (isPrivateMode) "WaterFox Private" else "WaterFox",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Small one-line tagline: "Private. Fast. Yours."
                Text(
                    text = "Private. Fast. Yours.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun FavoriteTile(
    favorite: FavoriteEntity,
    isDarkTheme: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val cleanLabel = remember(favorite.title, favorite.url) {
        cleanFavoriteLabel(favorite.title, favorite.url)
    }

    val host = remember(favorite.url) {
        try { Uri.parse(favorite.url).host ?: "" } catch (e: Exception) { "" }
    }
    val favicon = rememberFavicon(host)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(favorite.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .padding(vertical = 2.dp)
            .testTag("shortcut_tile_${favorite.title}")
    ) {
        // 48dp circle with the site's icon (letter fallback)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            if (favicon != null) {
                Image(
                    bitmap = favicon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                val letter = cleanLabel.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
                Text(
                    text = letter,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = cleanLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun AddFavoriteTile(isDarkTheme: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
            .testTag("shortcut_tile_add")
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "Add favorite",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Add",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Normalizes title so it never shows raw "https://..." or "www."
 */
private fun cleanFavoriteLabel(title: String, url: String): String {
    if (title.isNotBlank() && !title.startsWith("http://") && !title.startsWith("https://")) {
        return title
    }
    return try {
        val host = Uri.parse(url).host ?: url
        host.removePrefix("www.").removePrefix("https://").removePrefix("http://")
    } catch (e: Exception) {
        url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
    }
}

@Composable
private fun FindInPageBar(
    query: String,
    resultText: String,
    onQueryChange: (String) -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Find in page…") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            if (resultText.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = resultText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPrevClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Previous",
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onNextClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_forward),
                    contentDescription = "Next",
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onCloseClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Close",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PageCrashView(
    tab: Tab,
    onReload: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_warning),
            contentDescription = "Crash error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Aw, Snap!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Something went wrong while displaying this webpage.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onReload,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Reload")
            }
            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Go to Home", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
