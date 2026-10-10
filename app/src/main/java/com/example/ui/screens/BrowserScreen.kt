package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.input.pointer.pointerInput
import com.example.browser.TabManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.R
import com.example.browser.Tab
import com.example.data.database.FavoriteEntity
import com.example.data.preferences.AddressBarPosition
import com.example.ui.components.AutomationQuickFillSheet
import com.example.ui.components.FireDialog
import com.example.ui.components.HamburgerMenuSheet
import com.example.ui.components.LinkContextMenuSheet
import com.example.ui.components.Omnibar
import com.example.ui.components.PrivacyShieldSheet
import com.example.ui.components.SearchSuggestionsList
import com.example.ui.components.TabCounterBadge
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.BrandOrangeBright
import com.example.ui.theme.PrivateAccent
import com.example.ui.theme.PrivateBackground
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BrowserViewModel

@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val context = LocalContext.current

    BackHandler {
        viewModel.handleBackPress()
    }

    val normalTabs by viewModel.tabManager.normalTabs.collectAsState()
    val privateTabs by viewModel.tabManager.privateTabs.collectAsState()
    val currentTabId by viewModel.tabManager.currentTabId.collectAsState()
    val isPrivateMode by viewModel.tabManager.isPrivateMode.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val currentTab = viewModel.tabManager.getCurrentTab()

    val totalTabsCount = if (isPrivateMode) privateTabs.size else normalTabs.size
    val omnibarText by viewModel.omnibarInput.collectAsState()
    val isOmnibarFocused by viewModel.isOmnibarFocused.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    val showHamburgerMenu by viewModel.showHamburgerMenu.collectAsState()
    val showFireDialog by viewModel.showFireDialog.collectAsState()
    val showShieldSheet by viewModel.showShieldSheet.collectAsState()
    val showFindInPage by viewModel.showFindInPage.collectAsState()
    val findQuery by viewModel.findQuery.collectAsState()
    val findResultText by viewModel.findResultText.collectAsState()
    val showAddFavoriteDialog by viewModel.showAddFavoriteDialog.collectAsState()

    val linkContextMenuState by viewModel.tabManager.linkContextMenuState.collectAsState()

    // Favorite to edit or remove dialog state
    var selectedFavoriteForDialog by remember { mutableStateOf<FavoriteEntity?>(null) }

    val isAddressBarAtBottom = settings.addressBarPosition == AddressBarPosition.BOTTOM

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isPrivateMode) PrivateBackground else MaterialTheme.colorScheme.background)
    ) {
        // Top Bar & Omnibar if Position is TOP
        if (!isAddressBarAtBottom) {
            BrowserControlBar(
                viewModel = viewModel,
                currentTab = currentTab,
                totalTabsCount = totalTabsCount,
                omnibarText = omnibarText,
                isOmnibarFocused = isOmnibarFocused,
                suggestions = suggestions
            )
        }

        // Web Content / New Tab Page
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val isBlankPage = currentTab == null || currentTab.url.isBlank() || currentTab.url.startsWith("about:")

            if (isBlankPage) {
                // New Tab Home Page with DuckDuckGo vector artwork and Favorites Grid
                NewTabPage(
                    viewModel = viewModel,
                    isPrivateMode = isPrivateMode,
                    onFavoriteLongClick = { selectedFavoriteForDialog = it },
                    onAddFavoriteClick = { viewModel.showAddFavoriteDialog.value = true }
                )
            } else {
                // WebView with SwipeRefreshLayout or Crash Fallback
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
                                        setColorSchemeColors(android.graphics.Color.parseColor("#DE5833"))
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

            // Find in Page Bar at Bottom
            if (showFindInPage) {
                FindInPageBar(
                    query = findQuery,
                    resultText = findResultText,
                    onQueryChange = { viewModel.updateFindQuery(it) },
                    onPrevClick = { viewModel.findNext(false) },
                    onNextClick = { viewModel.findNext(true) },
                    onCloseClick = { viewModel.closeFindInPage() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Bottom Bar & Omnibar if Position is BOTTOM
        if (isAddressBarAtBottom) {
            BrowserControlBar(
                viewModel = viewModel,
                currentTab = currentTab,
                totalTabsCount = totalTabsCount,
                omnibarText = omnibarText,
                isOmnibarFocused = isOmnibarFocused,
                suggestions = suggestions,
                isBottomBar = true
            )
        }
    }

    // Sheets & Dialogs
    HamburgerMenuSheet(
        visible = showHamburgerMenu,
        canGoBack = currentTab?.canGoBack == true,
        canGoForward = currentTab?.canGoForward == true,
        isDesktopMode = currentTab?.isDesktopMode == true,
        onBackClick = { viewModel.tabManager.goBackCurrentTab() },
        onForwardClick = { viewModel.tabManager.goForwardCurrentTab() },
        onNewTabClick = {
            viewModel.tabManager.createNewTab(isPrivate = isPrivateMode)
        },
        onSettingsClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
        onBookmarksClick = { viewModel.navigateTo(AppScreen.BOOKMARKS) },
        onPasswordsClick = { viewModel.navigateTo(AppScreen.PASSWORDS) },
        onDownloadsClick = {
            viewModel.refreshDownloads()
            viewModel.navigateTo(AppScreen.DOWNLOADS)
        },
        onHistoryClick = { viewModel.navigateTo(AppScreen.HISTORY) },
        onAutomationClick = { viewModel.navigateTo(AppScreen.AUTOMATION) },
        onAddBookmarkClick = {
            currentTab?.let { tab ->
                viewModel.addBookmark(
                    title = if (tab.title.isNotBlank()) tab.title else tab.url,
                    url = tab.url
                )
            }
        },
        onAddToFavoritesClick = {
            currentTab?.let { tab ->
                viewModel.addFavorite(
                    title = if (tab.title.isNotBlank()) tab.title else tab.url,
                    url = tab.url
                )
            }
        },
        onShareClick = {
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
        onFindInPageClick = { viewModel.startFindInPage() },
        onRequestDesktopSiteClick = { viewModel.tabManager.toggleDesktopModeForCurrentTab() },
        onPrintPageClick = {
            currentTab?.webView?.let { wv ->
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
                val printAdapter = wv.createPrintDocumentAdapter("WaterFox Document")
                printManager?.print("WaterFox Page", printAdapter, null)
            }
        },
        onAddToHomeScreenClick = {
            currentTab?.let { tab ->
                val shortcutIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tab.url))
                val pinShortcutInfo = androidx.core.content.pm.ShortcutInfoCompat.Builder(context, tab.url)
                    .setShortLabel(if (tab.title.isNotBlank()) tab.title else "Web Page")
                    .setIntent(shortcutIntent)
                    .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(context, R.drawable.ic_duckduckgo))
                    .build()
                androidx.core.content.pm.ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
                viewModel.snackbarMessage.value = "Shortcut added to home screen"
            }
        },
        onCopyLinkClick = {
            currentTab?.let { tab ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("URL", tab.url))
                viewModel.snackbarMessage.value = "Link copied to clipboard"
            }
        },
        onRefreshClick = { viewModel.tabManager.reloadCurrentTab() },
        onReportBrokenSiteClick = { viewModel.showBrokenSiteDialog.value = true },
        onDismiss = { viewModel.showHamburgerMenu.value = false }
    )

    FireDialog(
        visible = showFireDialog,
        onDeleteThisTab = { viewModel.executeFireAction(onlyThisTab = true) },
        onDeleteAll = { viewModel.executeFireAction(onlyThisTab = false) },
        onDismiss = { viewModel.showFireDialog.value = false }
    )

    PrivacyShieldSheet(
        visible = showShieldSheet,
        pageUrl = currentTab?.url ?: "",
        blockedTrackers = currentTab?.blockedTrackersList ?: emptyList(),
        isProtectionEnabled = settings.trackerBlockingEnabled,
        onToggleProtection = { enabled ->
            viewModel.updateTrackerBlocking(enabled)
        },
        onDismiss = { viewModel.showShieldSheet.value = false }
    )

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
                userAgent = TabManager.MOBILE_USER_AGENT,
                contentDisposition = null,
                mimeType = "image/*",
                contentLength = 0L
            )
            viewModel.snackbarMessage.value = "Image download started"
        },
        onDismiss = { viewModel.tabManager.linkContextMenuState.value = null }
    )

    // Automation Quick Fill Sheet (opened via Long-Press on omnibar icon)
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
        onChangeMode = { mode ->
            viewModel.updateActiveDatasetMode(mode)
        },
        onDisableOnSite = { host ->
            viewModel.disableAutomationOnSite(host)
        },
        onOpenAutomationScreen = {
            viewModel.navigateTo(AppScreen.AUTOMATION)
        },
        onDismiss = {
            viewModel.showAutomationQuickFillSheet.value = false
        }
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

    // Favorite Options Dialog (Edit / Delete)
    selectedFavoriteForDialog?.let { favorite ->
        AlertDialog(
            onDismissRequest = { selectedFavoriteForDialog = null },
            title = { Text(favorite.title) },
            text = { Text(favorite.url) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFavorite(favorite)
                        selectedFavoriteForDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Favorite")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFavoriteForDialog = null }) {
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
                        label = { Text("URL (e.g. https://example.com)") },
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
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
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
}

@Composable
private fun BrowserControlBar(
    viewModel: BrowserViewModel,
    currentTab: Tab?,
    totalTabsCount: Int,
    omnibarText: String,
    isOmnibarFocused: Boolean,
    suggestions: List<com.example.viewmodel.SearchSuggestion>,
    isBottomBar: Boolean = false
) {
    val currentDetection by viewModel.automationManager.currentFormDetection.collectAsState()
    val autoSettings by viewModel.automationManager.settings.collectAsState()
    val hasFormFields = autoSettings.showOmnibarIcon && currentDetection != null && (currentDetection?.detectedFieldCount ?: 0) > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // When address bar is at the bottom, show suggestions above the bar
        if (isBottomBar && isOmnibarFocused && suggestions.isNotEmpty()) {
            SearchSuggestionsList(
                suggestions = suggestions,
                onSubmit = { viewModel.submitUrl(it) },
                onCopy = { viewModel.onOmnibarInputChanged(it) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Arrow Button
            IconButton(
                onClick = { viewModel.tabManager.goBackCurrentTab() },
                enabled = currentTab?.canGoBack == true,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("browser_back_button")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = if (currentTab?.canGoBack == true) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Omnibar (takes the center)
            Box(modifier = Modifier.weight(1f)) {
                Omnibar(
                    url = currentTab?.url ?: "",
                    isLoading = currentTab?.isLoading == true,
                    progress = currentTab?.progress ?: 0,
                    isHttps = currentTab?.isHttps == true,
                    hasSslError = currentTab?.hasSslError == true,
                    blockedTrackersCount = currentTab?.blockedTrackersCount ?: 0,
                    inputText = omnibarText,
                    isFocused = isOmnibarFocused,
                    onInputChanged = { viewModel.onOmnibarInputChanged(it) },
                    onFocusChanged = { viewModel.isOmnibarFocused.value = it },
                    onSubmit = { viewModel.submitUrl(it) },
                    onShieldClick = { viewModel.showShieldSheet.value = true },
                    hasAutomationFields = hasFormFields,
                    automationFieldCount = currentDetection?.detectedFieldCount ?: 0,
                    onAutomationClick = { viewModel.executeAutomationFill() },
                    onAutomationLongClick = { viewModel.showAutomationQuickFillSheet.value = true }
                )
            }

            // Fire Button
            IconButton(
                onClick = { viewModel.showFireDialog.value = true },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("browser_fire_button")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_fire),
                    contentDescription = "Fire Button",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tab Count Badge (rounded square)
            TabCounterBadge(
                count = totalTabsCount,
                onClick = { viewModel.navigateTo(AppScreen.TAB_SWITCHER) },
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Hamburger Menu
            IconButton(
                onClick = { viewModel.showHamburgerMenu.value = true },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("browser_menu_button")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_more_vert),
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // When address bar is at the top, show suggestions below the bar
        if (!isBottomBar && isOmnibarFocused && suggestions.isNotEmpty()) {
            SearchSuggestionsList(
                suggestions = suggestions,
                onSubmit = { viewModel.submitUrl(it) },
                onCopy = { viewModel.onOmnibarInputChanged(it) }
            )
        }
    }
}

@Composable
private fun NewTabPage(
    viewModel: BrowserViewModel,
    isPrivateMode: Boolean,
    onFavoriteLongClick: (FavoriteEntity) -> Unit,
    onAddFavoriteClick: () -> Unit
) {
    val favorites by viewModel.favoritesList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // DuckDuckGo Logo vector artwork
        Icon(
            painter = painterResource(id = R.drawable.ic_duckduckgo),
            contentDescription = "DuckDuckGo Logo",
            tint = Color.Unspecified,
            modifier = Modifier.size(68.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isPrivateMode) "Private Browsing" else "WaterFox",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPrivateMode) PrivateAccent else MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = if (isPrivateMode) "History and cookies are wiped when private tabs close." else "Privacy, simplified. No tracking, real speed.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Prominent Search Bar on New Tab Page
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable {
                    viewModel.isOmnibarFocused.value = true
                    viewModel.onOmnibarInputChanged("")
                }
                .testTag("home_search_bar"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 3.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "Search",
                    tint = BrandOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search DuckDuckGo or enter URL",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Favorites Grid
        Text(
            text = "FAVORITES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(favorites, key = { it.id }) { favorite ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(favorite.id) {
                            detectTapGestures(
                                onTap = { viewModel.tabManager.loadUrl(favorite.url) },
                                onLongPress = { onFavoriteLongClick(favorite) }
                            )
                        }
                        .padding(4.dp)
                        .testTag("favorite_item_${favorite.title}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = favorite.title.take(1).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = favorite.title,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // "+" Button to add favorite
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAddFavoriteClick)
                        .padding(4.dp)
                        .testTag("add_favorite_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = "Add favorite",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
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
            .testTag("find_in_page_bar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                cursorBrush = SolidColor(BrandOrange),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text("Find in page…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            )

            if (resultText.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = resultText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(onClick = onPrevClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Previous match",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(onClick = onNextClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_forward),
                    contentDescription = "Next match",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(onClick = onCloseClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Close find",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
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
            painter = painterResource(id = R.drawable.ic_shield),
            contentDescription = "Crash icon",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "This page encountered a problem",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The graphics renderer closed unexpectedly. You can reload the page or return to the new tab page.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onGoHome,
                modifier = Modifier.testTag("crash_go_home_button")
            ) {
                Text("New Tab")
            }
            Button(
                onClick = onReload,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("crash_reload_button")
            ) {
                Text("Reload")
            }
        }
    }
}
