package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.WaterFoxApp
import com.example.browser.DownloadHandler
import com.example.browser.DownloadItem
import com.example.browser.DuckDuckGoApi
import com.example.browser.Tab
import com.example.browser.TabManager
import com.example.browser.TrackerBlocker
import com.example.data.database.AppDatabase
import com.example.data.database.BookmarkEntity
import com.example.data.database.BookmarkFolderEntity
import com.example.data.database.BrokenSiteReportEntity
import com.example.data.database.FavoriteEntity
import com.example.data.database.HistoryEntity
import com.example.data.database.PasswordEntity
import com.example.data.preferences.AddressBarPosition
import com.example.data.preferences.BrowserSettings
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.WallpaperMode
import com.example.data.security.CryptoManager
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

enum class AppScreen {
    BROWSER,
    TAB_SWITCHER,
    BOOKMARKS,
    HISTORY,
    PASSWORDS,
    DOWNLOADS,
    SETTINGS,
    AUTOMATION
}

data class SearchSuggestion(
    val title: String,
    val url: String? = null,
    val isBookmarkOrHistory: Boolean = false,
    val isRecent: Boolean = false
)

data class SnackbarActionMessage(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    val database = AppDatabase.getDatabase(context)
    val preferencesRepository = UserPreferencesRepository(context)
    val trackerBlocker = TrackerBlocker(context)
    val cryptoManager = CryptoManager()
    val downloadHandler = DownloadHandler(context)
    val automationRepository = com.example.automation.AutomationRepository(context, database, cryptoManager)
    val automationManager = com.example.automation.AutomationManager(context, automationRepository, viewModelScope)

    val tabManager = TabManager(
        context = context,
        database = database,
        trackerBlocker = trackerBlocker,
        scope = viewModelScope
    )

    // Current Screen
    private val _currentScreen = MutableStateFlow(AppScreen.BROWSER)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Settings
    val settings: StateFlow<BrowserSettings> = preferencesRepository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        BrowserSettings()
    )

    // Omnibar State
    val omnibarInput = MutableStateFlow("")
    val isOmnibarFocused = MutableStateFlow(false)
    val suggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    private var suggestionJob: Job? = null

    // Trigger focus and select all text from Bottom Nav Search button
    val searchTriggerEvent = MutableStateFlow(0L)

    // UI Sheets and Overlays
    val showHamburgerMenu = MutableStateFlow(false) // Floating menu in Brave design
    val showFireDialog = MutableStateFlow(false)
    val showShieldSheet = MutableStateFlow(false)
    val showFindInPage = MutableStateFlow(false)
    val findQuery = MutableStateFlow("")
    val findResultText = MutableStateFlow("")
    val showBrokenSiteDialog = MutableStateFlow(false)
    val showAddFavoriteDialog = MutableStateFlow(false)
    val showFireBurningAnimation = MutableStateFlow(false)
    val snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarActionMessage = MutableStateFlow<SnackbarActionMessage?>(null)

    // Long-Press Popups for Bottom Nav
    val showBookmarkLongPressPopup = MutableStateFlow(false)
    val showTabsLongPressPopup = MutableStateFlow(false)
    val showRecentTabsDialog = MutableStateFlow(false)

    // Automation UI Sheets & Dialogs
    val showAutomationQuickFillSheet = MutableStateFlow(false)
    val pendingHttpPasswordWarningRecord = MutableStateFlow<com.example.automation.DatasetRecord?>(null)

    // Password auth
    val isPasswordUnlocked = MutableStateFlow(false)

    // History and Bookmarks
    val historyList = database.historyDao().getAllHistory().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val bookmarksList = database.bookmarkDao().getAllBookmarks().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val bookmarkFolders = database.bookmarkDao().getAllFolders().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val favoritesList = database.favoriteDao().getAllFavorites().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val passwordsList = database.passwordDao().getAllPasswords().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // 7 days blocked count
    val blockedTrackers7DaysCount = database.blockedTrackerDao()
        .getBlockedCountSince(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Downloads
    val downloadsList = MutableStateFlow<List<DownloadItem>>(emptyList())

    init {
        // Wire Automation callbacks from TabManager
        tabManager.onFormDetected = { url, count ->
            val settingsVal = automationManager.settings.value
            val uriHost = try { android.net.Uri.parse(url).host?.lowercase() } catch (e: Exception) { null }
            val isSiteDisabled = uriHost != null && settingsVal.disabledSites.contains(uriHost)
            if (settingsVal.detectionEnabled && !isSiteDisabled && count > 0) {
                automationManager.currentFormDetection.value = com.example.automation.FormDetectionInfo(url, count)
            } else {
                automationManager.currentFormDetection.value = null
            }
        }
        tabManager.onFormSubmitted = {
            val pending = automationManager.pendingUsedRecord
            val activeDs = automationManager.getActiveDataset()
            if (pending != null && activeDs != null && activeDs.markAsUsedOnSubmit) {
                automationManager.recordUsed(activeDs, pending)
                automationManager.pendingUsedRecord = null
            }
        }

        // Sync settings to TabManager
        viewModelScope.launch {
            settings.collect { newSettings ->
                tabManager.currentSettings = newSettings
            }
        }

        // Ensure default favorites exist
        viewModelScope.launch(Dispatchers.IO) {
            val existing = database.favoriteDao().getAllFavorites().first()
            if (existing.isEmpty()) {
                val defaults = listOf(
                    FavoriteEntity(title = "DuckDuckGo", url = "https://duckduckgo.com", orderIndex = 0),
                    FavoriteEntity(title = "Wikipedia", url = "https://wikipedia.org", orderIndex = 1),
                    FavoriteEntity(title = "Hacker News", url = "https://news.ycombinator.com", orderIndex = 2),
                    FavoriteEntity(title = "GitHub", url = "https://github.com", orderIndex = 3),
                    FavoriteEntity(title = "Reddit", url = "https://reddit.com", orderIndex = 4),
                    FavoriteEntity(title = "EFF", url = "https://eff.org", orderIndex = 5)
                )
                defaults.forEach { database.favoriteDao().insertFavorite(it) }
            }
        }

        refreshDownloads()
    }

    fun executeAutomationFill(
        explicitRecord: com.example.automation.DatasetRecord? = null,
        forceNext: Boolean = false,
        skipHttpCheck: Boolean = false
    ) {
        val activeDataset = automationManager.getActiveDataset()
        if (activeDataset == null) {
            snackbarMessage.value = "No active Automation dataset. Please upload or activate one."
            navigateTo(AppScreen.AUTOMATION)
            return
        }

        val currentTab = tabManager.getCurrentTab()
        val currentUrl = currentTab?.url ?: ""
        val (record, errorMsg) = if (explicitRecord != null) {
            Pair(explicitRecord, null)
        } else {
            automationManager.getNextRecordToFill(activeDataset, currentUrl, forceNext)
        }

        if (record == null) {
            snackbarMessage.value = errorMsg ?: "No available records to fill."
            return
        }

        val webView = currentTab?.webView
        if (webView == null) {
            snackbarMessage.value = "No active webpage to fill."
            return
        }

        // Check HTTP security warning
        if (!skipHttpCheck && automationManager.shouldWarnHttpPassword(currentUrl, record)) {
            pendingHttpPasswordWarningRecord.value = record
            return
        }

        val autoSettings = automationManager.settings.value
        val script = automationManager.buildFillScript(
            fields = record.fields,
            highlight = autoSettings.highlightFilledFields,
            autoSubmit = autoSettings.autoSubmitAfterFilling
        )

        webView.evaluateJavascript(script) { resultJson ->
            var filledCount = 0
            var totalEligible = 0
            try {
                val cleanJson = if (resultJson.startsWith("\"") && resultJson.endsWith("\"")) {
                    JSONObject(JSONObject(resultJson).toString())
                } else {
                    JSONObject(resultJson)
                }
                filledCount = cleanJson.optInt("filledCount", 0)
                totalEligible = cleanJson.optInt("totalEligible", 0)
            } catch (e: Exception) {
                filledCount = record.fields.size
                totalEligible = record.fields.size
            }

            if (!activeDataset.markAsUsedOnSubmit) {
                automationManager.recordUsed(activeDataset, record)
            } else {
                automationManager.pendingUsedRecord = record
            }

            val msg = "Filled $filledCount of $totalEligible fields"
            snackbarActionMessage.value = SnackbarActionMessage(
                message = msg,
                actionLabel = "UNDO",
                onAction = {
                    webView.evaluateJavascript(automationManager.buildUndoScript(), null)
                    snackbarMessage.value = "Form fill undone"
                }
            )
        }
    }

    fun disableAutomationOnSite(domain: String) {
        viewModelScope.launch {
            val currentDisabled = automationManager.settings.value.disabledSites.toMutableSet()
            currentDisabled.add(domain.lowercase())
            automationManager.repository.updateSettings(
                automationManager.settings.value.copy(disabledSites = currentDisabled)
            )
            automationManager.currentFormDetection.value = null
            snackbarMessage.value = "Automation disabled on $domain"
        }
    }

    fun removeDisabledSite(domain: String) {
        viewModelScope.launch {
            val currentDisabled = automationManager.settings.value.disabledSites.toMutableSet()
            currentDisabled.remove(domain.lowercase())
            automationManager.repository.updateSettings(
                automationManager.settings.value.copy(disabledSites = currentDisabled)
            )
            snackbarMessage.value = "Removed $domain from disabled list"
        }
    }

    fun updateActiveDatasetMode(mode: com.example.automation.UsageMode) {
        val active = automationManager.getActiveDataset() ?: return
        viewModelScope.launch {
            automationManager.repository.updateDataset(active.copy(usageMode = mode))
        }
    }

    fun advanceAutomationNextRecord() {
        val active = automationManager.getActiveDataset() ?: return
        val currentTab = tabManager.getCurrentTab()
        val currentUrl = currentTab?.url ?: ""
        val (nextRecord, msg) = automationManager.getNextRecordToFill(active, currentUrl, forceNext = true)
        if (nextRecord != null) {
            snackbarMessage.value = "Selected next record: ${nextRecord.fields["full_name"] ?: nextRecord.fields["username"] ?: "Record #${nextRecord.orderIndex + 1}"}"
        } else {
            snackbarMessage.value = msg ?: "No more records available"
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        showHamburgerMenu.value = false
        showBookmarkLongPressPopup.value = false
        showTabsLongPressPopup.value = false
        showRecentTabsDialog.value = false
    }

    fun handleBackPress(): Boolean {
        if (showBookmarkLongPressPopup.value) {
            showBookmarkLongPressPopup.value = false
            return true
        }
        if (showTabsLongPressPopup.value) {
            showTabsLongPressPopup.value = false
            return true
        }
        if (showRecentTabsDialog.value) {
            showRecentTabsDialog.value = false
            return true
        }
        if (showAutomationQuickFillSheet.value) {
            showAutomationQuickFillSheet.value = false
            return true
        }
        if (pendingHttpPasswordWarningRecord.value != null) {
            pendingHttpPasswordWarningRecord.value = null
            return true
        }
        if (showFindInPage.value) {
            showFindInPage.value = false
            return true
        }
        if (showHamburgerMenu.value) {
            showHamburgerMenu.value = false
            return true
        }
        if (showFireDialog.value) {
            showFireDialog.value = false
            return true
        }
        if (showShieldSheet.value) {
            showShieldSheet.value = false
            return true
        }
        if (isOmnibarFocused.value) {
            isOmnibarFocused.value = false
            return true
        }
        if (_currentScreen.value != AppScreen.BROWSER) {
            _currentScreen.value = AppScreen.BROWSER
            return true
        }
        // In browser screen:
        val currentTab = tabManager.getCurrentTab()
        // If home overlay is showing over a loaded tab, leave home overlay
        if (currentTab != null && currentTab.showHome && currentTab.url.isNotBlank()) {
            tabManager.leaveHomeOverlay()
            return true
        }
        // Then check webview history
        if (tabManager.goBackCurrentTab()) {
            return true
        }
        // Then check tab count
        val tabs = if (tabManager.isPrivateMode.value) tabManager.privateTabs.value else tabManager.normalTabs.value
        if (tabs.size > 1) {
            tabManager.closeTab(tabManager.currentTabId.value)
            return true
        }
        return false // exit app
    }

    // Omnibar & Autocomplete
    fun onOmnibarInputChanged(text: String) {
        omnibarInput.value = text
        suggestionJob?.cancel()

       if (text.trim().isEmpty()) {
            suggestionJob = viewModelScope.launch(Dispatchers.IO) {
                suggestions.value = loadRecentSuggestions(database)
            }
            return
        }

        suggestionJob = viewModelScope.launch(Dispatchers.IO) {
            delay(150) // debounce
            val query = text.trim()

            // 1. Matching bookmarks
            val matchedBookmarks = database.bookmarkDao().searchBookmarks(query).first()
                .take(3)
                .map { SearchSuggestion(title = it.title, url = it.url, isBookmarkOrHistory = true) }

            // 2. Matching history
            val matchedHistory = database.historyDao().searchHistory(query).first()
                .take(3)
                .map { SearchSuggestion(title = it.title, url = it.url, isBookmarkOrHistory = true) }

            // 3. DuckDuckGo Autocomplete
            val ddgSuggestions = DuckDuckGoApi.getAutocompleteSuggestions(query)
                .take(6)
                .map { SearchSuggestion(title = it) }

            val combined = (matchedBookmarks + matchedHistory + ddgSuggestions).distinctBy { it.title }
            suggestions.value = combined
        }
    }

    fun submitUrl(input: String) {
        isOmnibarFocused.value = false
        suggestions.value = emptyList()
        tabManager.loadUrl(input)
    }

    fun triggerSearchFromBottomBar() {
        val currentTab = tabManager.getCurrentTab()
        val initialText = if (currentTab == null || currentTab.showHome || currentTab.url.isBlank() || currentTab.url.startsWith("about:")) "" else currentTab.url
        omnibarInput.value = initialText
        isOmnibarFocused.value = true
        searchTriggerEvent.value = System.currentTimeMillis()
        onOmnibarInputChanged(initialText)
    }

    // Fire Action
    fun executeFireAction(onlyThisTab: Boolean) {
        showFireDialog.value = false
        showFireBurningAnimation.value = true

        viewModelScope.launch {
            delay(1200) // animated burning effect
            tabManager.burnSiteDataAndTabs(onlyThisTab)
            showFireBurningAnimation.value = false
            _currentScreen.value = AppScreen.BROWSER
            snackbarMessage.value = if (onlyThisTab) "Tab and site data deleted" else "All tabs and site data cleared"
        }
    }

    // Find in Page
    fun startFindInPage() {
        showFindInPage.value = true
        showHamburgerMenu.value = false
        findQuery.value = ""
        findResultText.value = ""
    }

    fun updateFindQuery(query: String) {
        findQuery.value = query
        val webView = tabManager.getCurrentTab()?.webView ?: return
        if (query.isNotBlank()) {
            webView.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                findResultText.value = if (numberOfMatches > 0) "${activeMatchOrdinal + 1}/$numberOfMatches" else "0/0"
            }
            webView.findAllAsync(query)
        } else {
            webView.clearMatches()
            findResultText.value = ""
        }
    }

    fun findNext(forward: Boolean) {
        tabManager.getCurrentTab()?.webView?.findNext(forward)
    }

    fun closeFindInPage() {
        tabManager.getCurrentTab()?.webView?.clearMatches()
        showFindInPage.value = false
        findQuery.value = ""
        findResultText.value = ""
    }

    // Bookmarks Operations
    fun isPageBookmarked(url: String): Boolean {
        if (url.isBlank() || url.startsWith("about:")) return false
        return bookmarksList.value.any { it.url == url }
    }

    fun getBookmarkForUrl(url: String): BookmarkEntity? {
        if (url.isBlank() || url.startsWith("about:")) return false.let { null }
        return bookmarksList.value.firstOrNull { it.url == url }
    }

    fun addBookmark(title: String, url: String, folderId: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = BookmarkEntity(title = title, url = url, folderId = folderId)
            database.bookmarkDao().insertBookmark(entity)
            snackbarActionMessage.value = SnackbarActionMessage(
                message = "Bookmark added",
                actionLabel = "UNDO",
                onAction = {
                    viewModelScope.launch(Dispatchers.IO) {
                        database.bookmarkDao().deleteBookmark(entity)
                    }
                }
            )
        }
    }

    fun editBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.bookmarkDao().updateBookmark(bookmark)
            snackbarMessage.value = "Bookmark updated"
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.bookmarkDao().deleteBookmark(bookmark)
            snackbarMessage.value = "Bookmark deleted"
        }
    }

    fun createBookmarkFolder(name: String, parentId: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            database.bookmarkDao().insertFolder(BookmarkFolderEntity(name = name, parentFolderId = parentId))
        }
    }

    fun exportBookmarksHtml(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = database.bookmarkDao().getAllBookmarks().first()
            val sb = StringBuilder()
            sb.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n<TITLE>Bookmarks</TITLE>\n<H1>Bookmarks</H1>\n<DL><p>\n")
            list.forEach { bm ->
                sb.append("    <DT><A HREF=\"${bm.url}\">${bm.title}</A>\n")
            }
            sb.append("</DL><p>\n")
            outputStream.use { it.write(sb.toString().toByteArray()) }
            snackbarMessage.value = "Bookmarks exported successfully"
        }
    }

    fun importBookmarksHtml(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = inputStream.bufferedReader().use { it.readText() }
            val regex = Regex("<A HREF=\"([^\"]+)\"[^>]*>([^<]+)</A>", RegexOption.IGNORE_CASE)
            var count = 0
            regex.findAll(content).forEach { match ->
                val url = match.groupValues[1]
                val title = match.groupValues[2]
                database.bookmarkDao().insertBookmark(BookmarkEntity(title = title, url = url))
                count++
            }
            snackbarMessage.value = "Imported $count bookmarks"
        }
    }

    // Favorites Operations
    fun addFavorite(title: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = database.favoriteDao().getAllFavorites().first().size
            database.favoriteDao().insertFavorite(
                FavoriteEntity(title = title, url = url, orderIndex = count)
            )
            snackbarMessage.value = "Added to favorites"
        }
    }

    fun editFavorite(favorite: FavoriteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.favoriteDao().updateFavorite(favorite)
            snackbarMessage.value = "Favorite updated"
        }
    }

    fun deleteFavorite(favorite: FavoriteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.favoriteDao().deleteFavorite(favorite)
            snackbarMessage.value = "Removed from favorites"
        }
    }

    // History Operations
    fun deleteHistoryItem(item: HistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.historyDao().deleteHistoryItem(item)
        }
    }

    fun deleteHistoryItems(ids: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            database.historyDao().deleteHistoryByIds(ids)
            snackbarMessage.value = "Deleted selected history items"
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            database.historyDao().clearAllHistory()
            snackbarMessage.value = "History cleared"
        }
    }

    // Passwords Operations
    fun addOrUpdatePassword(title: String, siteDomain: String, username: String, plainPassword: String, existingId: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val (encrypted, iv) = cryptoManager.encrypt(plainPassword)
            val entity = PasswordEntity(
                id = existingId ?: 0,
                title = title,
                siteDomain = siteDomain,
                username = username,
                encryptedPassword = encrypted,
                ivBase64 = iv
            )
            if (existingId != null && existingId > 0) {
                database.passwordDao().updatePassword(entity)
                snackbarMessage.value = "Password updated"
            } else {
                database.passwordDao().insertPassword(entity)
                snackbarMessage.value = "Password saved"
            }
        }
    }

    fun deletePassword(password: PasswordEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.passwordDao().deletePassword(password)
            snackbarMessage.value = "Password deleted"
        }
    }

    fun getDecryptedPassword(password: PasswordEntity): String {
        return cryptoManager.decrypt(password.encryptedPassword, password.ivBase64)
    }

    // Broken Site Report
    fun submitBrokenSiteReport(url: String, reason: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.brokenSiteReportDao().insertReport(
                BrokenSiteReportEntity(url = url, reason = reason, description = description)
            )
            showBrokenSiteDialog.value = false
            snackbarMessage.value = "Report saved locally. Thank you!"
        }
    }

    // Save page offline web archive
    fun saveCurrentPageOffline() {
        val tab = tabManager.getCurrentTab()
        if (tab == null || tab.url.isBlank() || tab.showHome) {
            snackbarMessage.value = "Cannot download home page"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val cleanHost = try { Uri.parse(tab.url).host ?: "page" } catch (e: Exception) { "page" }
            val fileName = "${cleanHost}_${System.currentTimeMillis()}.mht"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val destFile = File(downloadsDir, fileName)

            withContext(Dispatchers.Main) {
                tabManager.saveWebArchive(tab, destFile.absolutePath) { success ->
                    if (success) {
                        refreshDownloads()
                        snackbarMessage.value = "Page saved for offline viewing: $fileName"
                    } else {
                        snackbarMessage.value = "Failed to save offline page"
                    }
                }
            }
        }
    }

    // Downloads
    fun refreshDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            downloadsList.value = downloadHandler.getAllDownloads()
        }
    }

    fun removeDownload(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadHandler.cancelOrRemoveDownload(id)
            refreshDownloads()
        }
    }

    // Settings Updates
    fun updateTheme(mode: ThemeMode) = viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    fun updateAddressBarPosition(pos: AddressBarPosition) = viewModelScope.launch { preferencesRepository.setAddressBarPosition(pos) }
    fun updateTrackerBlocking(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setTrackerBlockingEnabled(enabled) }
    fun updateHttpsUpgrade(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setHttpsUpgradeEnabled(enabled) }
    fun updateBlockThirdPartyCookies(block: Boolean) = viewModelScope.launch { preferencesRepository.setBlockThirdPartyCookies(block) }
    fun updateClearOnExit(clear: Boolean) = viewModelScope.launch { preferencesRepository.setClearDataOnExit(clear) }
    fun updateAutoClearTimeout(minutes: Int) = viewModelScope.launch { preferencesRepository.setAutoClearTimeoutMinutes(minutes) }
    fun updateJavascriptEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setJavascriptEnabled(enabled) }
    fun updateDesktopSiteDefault(defaultDesktop: Boolean) = viewModelScope.launch { preferencesRepository.setDesktopSiteDefault(defaultDesktop) }
    fun updateOpenLinksInBackground(inBackground: Boolean) = viewModelScope.launch { preferencesRepository.setOpenLinksInBackground(inBackground) }
    fun updateTextSizePercent(percent: Int) = viewModelScope.launch { preferencesRepository.setTextSizePercent(percent) }
    fun dismissTrackerBanner() = viewModelScope.launch { preferencesRepository.setTrackerBannerDismissed(true) }

    fun updateWallpaperMode(mode: WallpaperMode) = viewModelScope.launch { preferencesRepository.setWallpaperMode(mode) }
    fun updateWallpaperSolidColor(colorHex: String) = viewModelScope.launch { preferencesRepository.setWallpaperSolidColor(colorHex) }

    fun setCustomWallpaperFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wallpapersDir = File(context.filesDir, "wallpapers").apply { mkdirs() }
                val targetFile = File(wallpapersDir, "custom_wallpaper_${System.currentTimeMillis()}.jpg")

                // Decode downsampled to screen size off main thread
                val inputStream = context.contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                val screenWidth = context.resources.displayMetrics.widthPixels.coerceAtLeast(720)
                val screenHeight = context.resources.displayMetrics.heightPixels.coerceAtLeast(1280)
                var sampleSize = 1
                while (options.outWidth / (sampleSize * 2) >= screenWidth &&
                    options.outHeight / (sampleSize * 2) >= screenHeight
                ) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val freshStream = context.contentResolver.openInputStream(uri)
                val sampledBitmap = BitmapFactory.decodeStream(freshStream, null, decodeOptions)
                freshStream?.close()

                if (sampledBitmap != null) {
                    FileOutputStream(targetFile).use { out ->
                        sampledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    preferencesRepository.setCustomWallpaperPath(targetFile.absolutePath)
                    preferencesRepository.setWallpaperMode(WallpaperMode.GALLERY_IMAGE)
                    snackbarMessage.value = "Wallpaper updated"
                }
            } catch (e: Exception) {
                snackbarMessage.value = "Failed to load image from gallery"
            }
        }
    }

    fun clearBrowsingData(clearHistory: Boolean, clearCookies: Boolean, clearCache: Boolean, clearTabs: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (clearHistory) database.historyDao().clearAllHistory()
            if (clearCookies) {
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                android.webkit.CookieManager.getInstance().flush()
            }
            if (clearCache) {
                android.webkit.WebStorage.getInstance().deleteAllData()
                WaterFoxApp.ensureWebViewCacheDirectories(context)
            }
            if (clearTabs) {
                tabManager.closeAllTabs(isPrivateOnly = false)
            }
            snackbarMessage.value = "Browsing data cleared"
        }
    }
}
