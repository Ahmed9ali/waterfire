package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.data.database.AppDatabase
import com.example.data.database.BlockedTrackerEntity
import com.example.data.database.HistoryEntity
import com.example.data.database.TabEntity
import com.example.data.preferences.BrowserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID

class TabManager(
    private val context: Context,
    private val database: AppDatabase,
    private val trackerBlocker: TrackerBlocker,
    private val scope: CoroutineScope
) {

    private val _normalTabs = MutableStateFlow<List<Tab>>(emptyList())
    val normalTabs: StateFlow<List<Tab>> = _normalTabs.asStateFlow()

    private val _privateTabs = MutableStateFlow<List<Tab>>(emptyList())
    val privateTabs: StateFlow<List<Tab>> = _privateTabs.asStateFlow()

    private val _currentTabId = MutableStateFlow<String>("")
    val currentTabId: StateFlow<String> = _currentTabId.asStateFlow()

    private val _isPrivateMode = MutableStateFlow(false)
    val isPrivateMode: StateFlow<Boolean> = _isPrivateMode.asStateFlow()

    // Bumped every time a tab's fields change in place (showHome, isLoading, progress,
    // url, title...). The UI observes this so it recomposes when a tab object is mutated.
    private val _tabsVersion = MutableStateFlow(0)
    val tabsVersion: StateFlow<Int> = _tabsVersion.asStateFlow()

    // Last progress value that was pushed to the UI for each tab (throttling)
    private val lastProgressNotify = HashMap<String, Int>()

    // Tabs that started loading while the home screen was showing. The home screen stays
    // visible until the new page actually paints, so the previous page never flashes.
    private val pendingHomeExit = HashSet<String>()

    // Recently closed tabs (up to 10)
    private val _recentlyClosedTabs = MutableStateFlow<List<ClosedTabInfo>>(emptyList())
    val recentlyClosedTabs: StateFlow<List<ClosedTabInfo>> = _recentlyClosedTabs.asStateFlow()

    // Dialog & UI Callbacks
    val jsDialogState = MutableStateFlow<JsDialogState?>(null)
    val sslErrorState = MutableStateFlow<SslErrorState?>(null)
    val webPermissionState = MutableStateFlow<WebPermissionState?>(null)
    val geolocationState = MutableStateFlow<GeolocationPermissionState?>(null)
    val fileChooserState = MutableStateFlow<FileChooserState?>(null)
    val linkContextMenuState = MutableStateFlow<LinkContextMenuState?>(null)
    val customFullScreenView = MutableStateFlow<View?>(null)

    // Activity context reference for proper window-backed rendering
    var activityContext: Context? = null

    // Automation Callbacks
    var onFormDetected: ((url: String, count: Int) -> Unit)? = null
    var onFormSubmitted: (() -> Unit)? = null

    // Performance timing tracker per tab (startTime)
    private val tabNavigationStartTimes = HashMap<String, Long>()
    private val tabReportedFirstProgress = HashSet<String>()

    // Pre-warmed background WebView
    private var prewarmedWebView: WebView? = null

    private var cachedAutomationScript: String? = null

    private fun getAutomationScript(): String {
        if (cachedAutomationScript != null) return cachedAutomationScript!!
        return try {
            val script = context.assets.open("automation.js").bufferedReader().use { it.readText() }
            cachedAutomationScript = script
            script
        } catch (e: Exception) {
            ""
        }
    }

    // Current app settings
    var currentSettings: BrowserSettings = BrowserSettings()
        set(value) {
            field = value
            applySettingsToAllWebViews()
        }

    companion object {
        const val TAG = "WFPerf"
        const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 WaterFox/1.0"
        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 WaterFox/1.0"

        val WEBGL_SAFE_SCRIPT = """
            (function() {
                try {
                    var g = typeof globalThis !== 'undefined' ? globalThis : (typeof window !== 'undefined' ? window : (typeof self !== 'undefined' ? self : this));
                    if (!g || g.__webgl_guarded__) return;
                    g.__webgl_guarded__ = true;

                    function isGlType(t) {
                        if (!t) return false;
                        var s = String(t).toLowerCase();
                        return s.indexOf('webgl') !== -1 || s.indexOf('webgpu') !== -1 || s === 'gpu';
                    }

                    if (typeof HTMLCanvasElement !== 'undefined' && HTMLCanvasElement.prototype) {
                        var origGetCtx = HTMLCanvasElement.prototype.getContext;
                        HTMLCanvasElement.prototype.getContext = function(type, attrs) {
                            if (isGlType(type)) return null;
                            return origGetCtx.apply(this, arguments);
                        };
                    }

                    if (typeof OffscreenCanvas !== 'undefined' && OffscreenCanvas.prototype && OffscreenCanvas.prototype.getContext) {
                        var origOffscreenGetCtx = OffscreenCanvas.prototype.getContext;
                        OffscreenCanvas.prototype.getContext = function(type, attrs) {
                            if (isGlType(type)) return null;
                            return origOffscreenGetCtx.apply(this, arguments);
                        };
                    }

                    try {
                        if (g.navigator && ('gpu' in g.navigator)) {
                            Object.defineProperty(g.navigator, 'gpu', {
                                get: function() { return undefined; },
                                configurable: true
                            });
                        }
                    } catch (e) {}

                    try {
                        Object.defineProperty(g, 'WebGLRenderingContext', {
                            value: undefined,
                            configurable: true,
                            writable: true
                        });
                        Object.defineProperty(g, 'WebGL2RenderingContext', {
                            value: undefined,
                            configurable: true,
                            writable: true
                        });
                    } catch (e) {
                        g.WebGLRenderingContext = undefined;
                        g.WebGL2RenderingContext = undefined;
                    }
                } catch (e) {}
            })();
        """.trimIndent()
    }

    init {
        // Load saved tabs from Room
        scope.launch(Dispatchers.IO) {
            database.tabDao().getAllTabs().collect { entities ->
                if (_normalTabs.value.isEmpty() && entities.isNotEmpty()) {
                    val tabs = entities.map { entity ->
                        Tab(
                            id = entity.id,
                            url = entity.url,
                            title = entity.title,
                            isPrivate = false
                        ).also { tab ->
                            if (tab.url.isNotBlank()) {
                                tab.isHttps = tab.url.startsWith("https://")
                            }
                        }
                    }
                    _normalTabs.value = tabs
                    val selected = entities.firstOrNull { it.isCurrent }?.id ?: tabs.first().id
                    _currentTabId.value = selected
                } else if (_normalTabs.value.isEmpty() && entities.isEmpty()) {
                    createNewTab(isPrivate = false, initialUrl = "")
                }
            }
        }
    }

    /**
     * Pre-warm one WebView in background during splash so first navigation is instant
     */
    fun prewarmWebView(targetContext: Context) {
        if (prewarmedWebView != null) return
        try {
            val wv = WebView(targetContext.applicationContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            configureWebSettings(wv.settings, false)
            prewarmedWebView = wv
            Log.d(TAG, "Pre-warmed background WebView initialized")
        } catch (e: Exception) {
            Log.w(TAG, "Pre-warming WebView failed", e)
        }
    }

    fun getCurrentTab(): Tab? {
        val currentId = _currentTabId.value
        val list = if (_isPrivateMode.value) _privateTabs.value else _normalTabs.value
        return list.firstOrNull { it.id == currentId } ?: list.firstOrNull()
    }

    fun setPrivateMode(privateMode: Boolean) {
        _isPrivateMode.value = privateMode
        val list = if (privateMode) _privateTabs.value else _normalTabs.value
        if (list.isEmpty()) {
            createNewTab(isPrivate = privateMode, initialUrl = "")
        } else {
            _currentTabId.value = list.first().id
        }
        releasePendingHomeExcept(_currentTabId.value)
    }

    fun selectTab(id: String) {
        val isPrivate = _privateTabs.value.any { it.id == id }
        _isPrivateMode.value = isPrivate
        _currentTabId.value = id
        releasePendingHomeExcept(id)
        updateCurrentTabInRoom(id)
    }

    fun createNewTab(isPrivate: Boolean, initialUrl: String = "", openInBackground: Boolean = false): Tab {
        val newTab = Tab(
            id = UUID.randomUUID().toString(),
            url = initialUrl,
            title = if (initialUrl.isNotBlank()) "Loading…" else "New Tab",
            isPrivate = isPrivate,
            isDesktopMode = currentSettings.desktopSiteDefault,
            showHome = initialUrl.isBlank()
        )

        if (isPrivate) {
            _privateTabs.value = _privateTabs.value + newTab
            if (!openInBackground) {
                _isPrivateMode.value = true
                _currentTabId.value = newTab.id
            }
        } else {
            _normalTabs.value = _normalTabs.value + newTab
            if (!openInBackground) {
                _isPrivateMode.value = false
                _currentTabId.value = newTab.id
                updateCurrentTabInRoom(newTab.id)
            }
            // Persist to Room only if has URL
            if (initialUrl.isNotBlank()) {
                scope.launch(Dispatchers.IO) {
                    database.tabDao().insertTab(
                        TabEntity(
                            id = newTab.id,
                            title = newTab.title,
                            url = newTab.url,
                            isPrivate = false,
                            createdAt = System.currentTimeMillis(),
                            lastAccessedAt = System.currentTimeMillis(),
                            isCurrent = !openInBackground
                        )
                    )
                }
            }
        }

        // Lazy creation: only instantiate WebView if there's an actual URL to load.
        // autoLoad = false because loadUrlInTab below loads the processed URL; letting
        // getOrCreateWebView also load the raw text made every new tab load twice.
        if (initialUrl.isNotBlank()) {
            getOrCreateWebView(newTab, autoLoad = false)
            loadUrlInTab(newTab, initialUrl)
        }

        if (!openInBackground) releasePendingHomeExcept(newTab.id)

        return newTab
    }

    fun closeTab(id: String) {
        val isPrivate = _privateTabs.value.any { it.id == id }
        if (isPrivate) {
            val tab = _privateTabs.value.firstOrNull { it.id == id }
            tab?.let {
                if (it.url.isNotBlank() && !it.url.startsWith("about:")) {
                    recordRecentlyClosed(it.title, it.url, isPrivate = true)
                }
                destroyTabWebView(it)
            }
            val remaining = _privateTabs.value.filter { it.id != id }
            _privateTabs.value = remaining
            lastProgressNotify.remove(id)
            if (_currentTabId.value == id) {
                if (remaining.isNotEmpty()) {
                    _currentTabId.value = remaining.last().id
                } else {
                    if (_normalTabs.value.isNotEmpty()) {
                        _isPrivateMode.value = false
                        _currentTabId.value = _normalTabs.value.last().id
                    } else {
                        createNewTab(isPrivate = false)
                    }
                }
            }
        } else {
            val tab = _normalTabs.value.firstOrNull { it.id == id }
            tab?.let {
                if (it.url.isNotBlank() && !it.url.startsWith("about:")) {
                    recordRecentlyClosed(it.title, it.url, isPrivate = false)
                }
                destroyTabWebView(it)
            }
            val remaining = _normalTabs.value.filter { it.id != id }
            _normalTabs.value = remaining
            lastProgressNotify.remove(id)

            scope.launch(Dispatchers.IO) {
                database.tabDao().deleteTabById(id)
            }

            if (_currentTabId.value == id) {
                if (remaining.isNotEmpty()) {
                    val nextId = remaining.last().id
                    _currentTabId.value = nextId
                    updateCurrentTabInRoom(nextId)
                } else {
                    createNewTab(isPrivate = false)
                }
            }
        }
    }

    private fun recordRecentlyClosed(title: String, url: String, isPrivate: Boolean) {
        val item = ClosedTabInfo(title = if (title.isNotBlank()) title else url, url = url, isPrivate = isPrivate)
        _recentlyClosedTabs.value = (listOf(item) + _recentlyClosedTabs.value).take(10)
    }

    fun restoreRecentlyClosedTab(): Boolean {
        val list = _recentlyClosedTabs.value
        if (list.isEmpty()) return false
        val toRestore = list.first()
        _recentlyClosedTabs.value = list.drop(1)
        createNewTab(isPrivate = toRestore.isPrivate, initialUrl = toRestore.url)
        return true
    }

    fun closeAllTabs(isPrivateOnly: Boolean = false) {
        if (isPrivateOnly) {
            _privateTabs.value.forEach { destroyTabWebView(it) }
            _privateTabs.value = emptyList()
            if (_isPrivateMode.value) {
                _isPrivateMode.value = false
                if (_normalTabs.value.isEmpty()) {
                    createNewTab(isPrivate = false)
                } else {
                    _currentTabId.value = _normalTabs.value.first().id
                }
            }
        } else {
            _privateTabs.value.forEach { destroyTabWebView(it) }
            _normalTabs.value.forEach { destroyTabWebView(it) }
            _privateTabs.value = emptyList()
            _normalTabs.value = emptyList()
            lastProgressNotify.clear()

            scope.launch(Dispatchers.IO) {
                database.tabDao().deleteAllTabs()
            }

            _isPrivateMode.value = false
            createNewTab(isPrivate = false)
        }
    }

    fun burnSiteDataAndTabs(onlyCurrentTab: Boolean = false) {
        if (onlyCurrentTab) {
            val current = getCurrentTab() ?: return
            destroyTabWebView(current)
            closeTab(current.id)
        } else {
            _privateTabs.value.forEach { destroyTabWebView(it) }
            _normalTabs.value.forEach { destroyTabWebView(it) }
            _privateTabs.value = emptyList()
            _normalTabs.value = emptyList()
            lastProgressNotify.clear()

            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            com.example.WaterFoxApp.ensureWebViewCacheDirectories(context)

            trackerBlocker.clearAllCache()

            scope.launch(Dispatchers.IO) {
                database.tabDao().deleteAllTabs()
                database.historyDao().clearAllHistory()
            }

            _isPrivateMode.value = false
            createNewTab(isPrivate = false)
        }
    }

    private fun updateCurrentTabInRoom(currentId: String) {
        scope.launch(Dispatchers.IO) {
            _normalTabs.value.forEach { tab ->
                if (tab.url.isNotBlank()) {
                    database.tabDao().insertTab(
                        TabEntity(
                            id = tab.id,
                            title = tab.title,
                            url = tab.url,
                            isPrivate = false,
                            createdAt = System.currentTimeMillis(),
                            lastAccessedAt = if (tab.id == currentId) System.currentTimeMillis() else 0L,
                            isCurrent = tab.id == currentId
                        )
                    )
                }
            }
        }
    }

    fun loadUrl(url: String) {
        val tab = getCurrentTab() ?: return
        loadUrlInTab(tab, url)
    }

    fun loadUrlInTab(tab: Tab, targetUrl: String) {
        var processedUrl = targetUrl.trim()
        if (processedUrl.isBlank()) return

        // Smart input: URL vs Search detection
        val lower = processedUrl.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://") ||
            lower.startsWith("file://") || lower.startsWith("about:")
        ) {
            // direct URL
        } else if (lower.startsWith("localhost") || lower.matches(Regex("^(\\d{1,3}\\.){3}\\d{1,3}(:\\d+)?(/.*)?\$"))) {
            processedUrl = "http://$processedUrl"
        } else if (processedUrl.contains(".") && !processedUrl.contains(" ") && !processedUrl.contains("\n")) {
            processedUrl = "https://$processedUrl"
        } else {
            processedUrl = DuckDuckGoApi.buildSearchUrl(processedUrl)
        }

        // HTTPS upgrade check
        if (currentSettings.httpsUpgradeEnabled && processedUrl.startsWith("http://") && !processedUrl.contains("localhost") && !processedUrl.contains("127.0.0.1")) {
            processedUrl = "https://" + processedUrl.substring(7)
        }

        tab.url = processedUrl
        tab.title = "Loading…"
        if (tab.showHome) {
            // Coming from the home screen: the WebView may still hold the previous page,
            // so keep home visible until the new page paints (see onPageCommitVisible).
            pendingHomeExit.add(tab.id)
        } else {
            tab.showHome = false
        }
        tab.isHttps = processedUrl.startsWith("https://")
        tab.hasSslError = false
        tab.hasCrashError = false
        tab.crashRecoveryAttempts = 0

        // Show the loading bar immediately instead of waiting for onPageStarted
        tab.isLoading = true
        tab.progress = 5
        lastProgressNotify[tab.id] = 0

        // Timing log (WFPerf)
        tabNavigationStartTimes[tab.id] = System.currentTimeMillis()
        tabReportedFirstProgress.remove(tab.id)
        Log.d(TAG, "Navigation start: $processedUrl")

        notifyTabUpdated(tab)

        val webView = getOrCreateWebView(tab, autoLoad = false)
        webView.loadUrl(processedUrl)
    }

    fun reloadCurrentTab() {
        val tab = getCurrentTab() ?: return
        tab.hasCrashError = false
        tab.crashRecoveryAttempts = 0
        val currentUrl = tab.url
        val wv = tab.webView
        if (wv != null && !wv.url.isNullOrBlank()) {
            tabNavigationStartTimes[tab.id] = System.currentTimeMillis()
            tabReportedFirstProgress.remove(tab.id)
            Log.d(TAG, "Navigation reload: $currentUrl")
            try {
                wv.reload()
            } catch (e: Exception) {
                val newWv = getOrCreateWebView(tab, autoLoad = false)
                newWv.loadUrl(currentUrl)
            }
        } else if (currentUrl.isNotBlank()) {
            tabNavigationStartTimes[tab.id] = System.currentTimeMillis()
            tabReportedFirstProgress.remove(tab.id)
            val newWv = getOrCreateWebView(tab, autoLoad = false)
            newWv.loadUrl(currentUrl)
        }
        notifyTabUpdated(tab)
    }

    fun stopCurrentTab() {
        val tab = getCurrentTab() ?: return
        tab.webView?.stopLoading()
        tab.isLoading = false
        notifyTabUpdated(tab)
    }

    fun goBackCurrentTab(): Boolean {
        val tab = getCurrentTab() ?: return false
        if (tab.showHome && tab.url.isNotBlank()) {
            tab.showHome = false
            notifyTabUpdated(tab)
            return true
        }
        val webView = tab.webView
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return false
    }

    fun goForwardCurrentTab(): Boolean {
        val webView = getCurrentTab()?.webView
        if (webView != null && webView.canGoForward()) {
            webView.goForward()
            return true
        }
        return false
    }

    fun toggleDesktopModeForCurrentTab() {
        val tab = getCurrentTab() ?: return
        tab.isDesktopMode = !tab.isDesktopMode
        val webView = getOrCreateWebView(tab, autoLoad = false)
        webView.settings.userAgentString = if (tab.isDesktopMode) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
        webView.reload()
        notifyTabUpdated(tab)
    }

    fun showHomeInCurrentTab() {
        val tab = getCurrentTab() ?: return
        pendingHomeExit.remove(tab.id)
        if (!tab.showHome) {
            tab.showHome = true
            notifyTabUpdated(tab)
        }
    }

    fun leaveHomeOverlay() {
        val tab = getCurrentTab() ?: return
        if (tab.showHome && tab.url.isNotBlank()) {
            tab.showHome = false
            notifyTabUpdated(tab)
        }
    }

    /**
     * Save web archive for offline viewing into downloads
     */
    fun saveWebArchive(tab: Tab, targetPath: String, onComplete: (Boolean) -> Unit) {
        val wv = tab.webView
        if (wv == null) {
            onComplete(false)
            return
        }
        try {
            wv.saveWebArchive(targetPath, false) { resultPath ->
                onComplete(resultPath != null)
            }
        } catch (e: Exception) {
            onComplete(false)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(tab: Tab, customContext: Context? = null, autoLoad: Boolean = true): WebView {
        if (tab.webView != null) {
            return tab.webView!!
        }

        val targetContext = customContext ?: activityContext ?: context
        com.example.WaterFoxApp.ensureWebViewCacheDirectories(targetContext)

        // Reuse pre-warmed WebView if available for first navigation
        val webView: WebView = if (prewarmedWebView != null) {
            val pw = prewarmedWebView!!
            prewarmedWebView = null
            (pw.parent as? ViewGroup)?.removeView(pw)
            pw
        } else {
            WebView(targetContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isFocusable = true
                isFocusableInTouchMode = true
            }
        }

        configureWebSettings(webView.settings, tab.isDesktopMode)

        // Add Automation Javascript Interface
        webView.addJavascriptInterface(
            com.example.automation.AutomationJsInterface(
                scope = scope,
                onFormDetectedCallback = { url, count -> onFormDetected?.invoke(url, count) },
                onFormSubmittedCallback = { onFormSubmitted?.invoke() }
            ),
            "WaterFoxAutomation"
        )

        // Inject WebGL safe script before any page script executes
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try {
                WebViewCompat.addDocumentStartJavaScript(webView, WEBGL_SAFE_SCRIPT, setOf("*"))
            } catch (e: Exception) {}
            try {
                val autoScript = getAutomationScript()
                if (autoScript.isNotBlank()) {
                    WebViewCompat.addDocumentStartJavaScript(webView, autoScript, setOf("*"))
                }
            } catch (e: Exception) {}
        }

        // Cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (currentSettings.blockThirdPartyCookies || tab.isPrivate) {
            cookieManager.setAcceptThirdPartyCookies(webView, false)
        } else {
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }

        // WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val urlString = uri.toString()

                val scheme = uri.scheme?.lowercase() ?: ""
                if (scheme in listOf("mailto", "tel", "sms", "geo", "market", "intent")) {
                    handleExternalScheme(uri)
                    return true
                }

                if (scheme == "http" || scheme == "https") {
                    if (currentSettings.httpsUpgradeEnabled && scheme == "http") {
                        val upgraded = "https://" + urlString.substring(7)
                        view?.loadUrl(upgraded)
                        return true
                    }
                    return false
                }

                return false
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val requestUrl = request?.url?.toString() ?: return null
                val mainUrl = tab.url

                // Check Tracker blocking - O(1) in-memory HashSet lookup
                if (currentSettings.trackerBlockingEnabled) {
                    val trackerHost = trackerBlocker.isTracker(requestUrl, mainUrl)
                    if (trackerHost != null) {
                        synchronized(tab) {
                            tab.blockedTrackersCount++
                            val list = tab.blockedTrackersList
                            if (!list.contains(trackerHost)) {
                                tab.blockedTrackersList = list + trackerHost
                            }
                        }
                        // Return empty response immediately without disk/proxy overhead
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                val currentUrl = url ?: ""
                tab.url = currentUrl
                tab.isLoading = true
                tab.progress = 10
                tab.hasCrashError = false
                tab.isHttps = currentUrl.startsWith("https://")
                tab.canGoBack = view?.canGoBack() == true
                tab.canGoForward = view?.canGoForward() == true
                lastProgressNotify[tab.id] = 0
                notifyTabUpdated(tab)

                // document_start injection already covers this on modern WebViews;
                // only fall back to evaluateJavascript when it is not supported.
                if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    try {
                        view?.evaluateJavascript(WEBGL_SAFE_SCRIPT, null)
                    } catch (e: Exception) {}
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val currentUrl = url ?: ""
                tab.url = currentUrl
                tab.title = view?.title ?: if (currentUrl.isNotBlank()) currentUrl else "New Tab"
                tab.isLoading = false
                tab.progress = 100
                tab.canGoBack = view?.canGoBack() == true
                tab.canGoForward = view?.canGoForward() == true
                tab.isHttps = currentUrl.startsWith("https://")
                tab.hasCrashError = false
                tab.crashRecoveryAttempts = 0
                lastProgressNotify[tab.id] = 0

                // WFPerf Timing log
                val start = tabNavigationStartTimes[tab.id] ?: 0L
                val elapsed = if (start > 0) System.currentTimeMillis() - start else 0L
                Log.d(TAG, "onPageFinished: $currentUrl in ${elapsed}ms")

                // Fallback automation script injection only if document_start wasn't supported
                if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    try {
                        val script = getAutomationScript()
                        if (script.isNotBlank()) {
                            view?.evaluateJavascript(script, null)
                        }
                    } catch (e: Exception) {}
                }

                // Capture the thumbnail a little later so drawing the WebView
                // does not compete with the page's first render
                view?.postDelayed({ captureThumbnail(view, tab) }, 1500)

                leaveHomeIfPending(tab)
                notifyTabUpdated(tab)

                // Persist to Room History and TabEntity throttled on page completion
                if (!tab.isPrivate && currentUrl.isNotBlank() && !currentUrl.startsWith("about:")) {
                    scope.launch(Dispatchers.IO) {
                        database.historyDao().insertHistory(
                            HistoryEntity(
                                title = tab.title,
                                url = currentUrl,
                                visitedAt = System.currentTimeMillis()
                            )
                        )
                        database.tabDao().insertTab(
                            TabEntity(
                                id = tab.id,
                                title = tab.title,
                                url = tab.url,
                                isPrivate = false,
                                createdAt = System.currentTimeMillis(),
                                lastAccessedAt = System.currentTimeMillis(),
                                isCurrent = tab.id == _currentTabId.value
                            )
                        )
                        // Batch insert blocked trackers for this page
                        tab.blockedTrackersList.forEach { trackerDomain ->
                            database.blockedTrackerDao().insertBlockedTracker(
                                BlockedTrackerEntity(
                                    domain = trackerDomain,
                                    pageUrl = currentUrl
                                )
                            )
                        }
                    }
                }
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                leaveHomeIfPending(tab)
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                tab.hasSslError = true
                notifyTabUpdated(tab)
                if (handler != null) {
                    sslErrorState.value = SslErrorState(
                        url = error?.url ?: tab.url,
                        handler = handler
                    )
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    tab.isLoading = false
                    leaveHomeIfPending(tab)
                    notifyTabUpdated(tab)
                }
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                val didCrash = detail?.didCrash() ?: true
                Log.e(TAG, "onRenderProcessGone detected (crashed=$didCrash, attempts=${tab.crashRecoveryAttempts})")

                view?.let { crashedView ->
                    (crashedView.parent as? ViewGroup)?.removeView(crashedView)
                    try {
                        crashedView.destroy()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error destroying crashed WebView", e)
                    }
                }

                if (tab.webView == view) {
                    tab.webView = null
                }

                tab.isLoading = false

                val currentUrl = tab.url
                if (tab.crashRecoveryAttempts < 1 && currentUrl.isNotBlank() && !currentUrl.startsWith("about:")) {
                    tab.crashRecoveryAttempts++
                    tab.hasCrashError = false
                    notifyTabUpdated(tab)
                    scope.launch(Dispatchers.Main) {
                        val restoredWebView = getOrCreateWebView(tab, activityContext, autoLoad = false)
                        restoredWebView.loadUrl(currentUrl)
                    }
                } else {
                    tab.hasCrashError = true
                    notifyTabUpdated(tab)
                }

                return true
            }
        }

        // WebChromeClient
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                tab.progress = newProgress
                if (newProgress >= 100) {
                    tab.isLoading = false
                }

                // WFPerf Timing log for first progress > 10%
                if (newProgress > 10 && !tabReportedFirstProgress.contains(tab.id)) {
                    tabReportedFirstProgress.add(tab.id)
                    val start = tabNavigationStartTimes[tab.id] ?: 0L
                    val elapsed = if (start > 0) System.currentTimeMillis() - start else 0L
                    Log.d(TAG, "First progress above 10% ($newProgress%) for: ${tab.url} in ${elapsed}ms")
                }

                // Only push to the UI every 10% (and on reset / completion). The progress bar
                // animates between steps, and this avoids recomposing the whole screen on
                // every progress tick while the page is loading.
                val last = lastProgressNotify[tab.id] ?: 0
                if (newProgress >= 100 || newProgress < last || newProgress - last >= 10) {
                    lastProgressNotify[tab.id] = newProgress
                    notifyTabUpdated(tab)
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrBlank()) {
                    tab.title = title
                    notifyTabUpdated(tab)
                }
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                val newTab = createNewTab(isPrivate = tab.isPrivate, openInBackground = currentSettings.openLinksInBackground)
                val newWebView = getOrCreateWebView(newTab, autoLoad = false)
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = newWebView
                resultMsg?.sendToTarget()
                return true
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                if (result != null && message != null) {
                    jsDialogState.value = JsDialogState.Alert(message, result)
                    return true
                }
                return false
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                if (result != null && message != null) {
                    jsDialogState.value = JsDialogState.Confirm(message, result)
                    return true
                }
                return false
            }

            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
                if (result != null && message != null) {
                    jsDialogState.value = JsDialogState.Prompt(message, defaultValue ?: "", result)
                    return true
                }
                return false
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request != null) {
                    webPermissionState.value = WebPermissionState(
                        origin = request.origin.toString(),
                        resources = request.resources,
                        request = request
                    )
                }
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                if (origin != null && callback != null) {
                    geolocationState.value = GeolocationPermissionState(origin, callback)
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (filePathCallback != null) {
                    fileChooserState.value = FileChooserState(
                        filePathCallback = filePathCallback,
                        mimeTypes = fileChooserParams?.acceptTypes ?: arrayOf("*/*"),
                        isMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                    )
                    return true
                }
                return false
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customFullScreenView.value = view
            }

            override fun onHideCustomView() {
                customFullScreenView.value = null
            }
        }

        // Long click context menu for links/images
        webView.setOnLongClickListener {
            val result = webView.hitTestResult
            val type = result.type
            val extra = result.extra
            if (extra != null && (type == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                        type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE ||
                        type == WebView.HitTestResult.IMAGE_TYPE)
            ) {
                linkContextMenuState.value = LinkContextMenuState(
                    url = extra,
                    title = extra,
                    isImage = type == WebView.HitTestResult.IMAGE_TYPE,
                    imageUrl = if (type == WebView.HitTestResult.IMAGE_TYPE) extra else null
                )
                true
            } else {
                false
            }
        }

        tab.webView = webView

        // Auto-load restored tabs if URL present (skipped when the caller loads a URL itself)
        if (autoLoad && tab.url.isNotBlank() && !tab.url.startsWith("about:") && webView.url == null) {
            tabNavigationStartTimes[tab.id] = System.currentTimeMillis()
            webView.loadUrl(tab.url)
        }

        return webView
    }

    private fun configureWebSettings(settings: WebSettings, isDesktop: Boolean) {
        settings.apply {
            javaScriptEnabled = currentSettings.javascriptEnabled
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            mediaPlaybackRequiresUserGesture = true
            textZoom = currentSettings.textSizePercent
            userAgentString = if (isDesktop) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
            allowFileAccess = false
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
        }
    }

    private fun applySettingsToAllWebViews() {
        val allTabs = _normalTabs.value + _privateTabs.value
        allTabs.forEach { tab ->
            tab.webView?.settings?.let { settings ->
                configureWebSettings(settings, tab.isDesktopMode)
            }
        }
    }

    /**
     * Captures 1/4 resolution thumbnail using RGB_565 to minimize memory usage
     */
    private fun captureThumbnail(view: WebView?, tab: Tab) {
        if (view == null || view.width <= 0 || view.height <= 0) return
        try {
            val width = (view.width / 4).coerceAtLeast(160)
            val height = (view.height / 4).coerceAtLeast(240)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            val scaleX = width.toFloat() / view.width.toFloat()
            val scaleY = height.toFloat() / view.height.toFloat()
            canvas.scale(scaleX, scaleY)
            view.draw(canvas)
            tab.thumbnail = bitmap
        } catch (e: Exception) {}
    }

    fun pauseAllWebViews() {
        val allTabs = _normalTabs.value + _privateTabs.value
        allTabs.forEach { tab ->
            try {
                tab.webView?.onPause()
                tab.webView?.pauseTimers()
            } catch (e: Exception) {}
        }
    }

    fun resumeAllWebViews() {
        val allTabs = _normalTabs.value + _privateTabs.value
        allTabs.forEach { tab ->
            try {
                tab.webView?.onResume()
                tab.webView?.resumeTimers()
            } catch (e: Exception) {}
        }
    }

    fun destroyAllWebViews() {
        val allTabs = _normalTabs.value + _privateTabs.value
        allTabs.forEach { tab ->
            destroyTabWebView(tab)
        }
    }

    private fun handleExternalScheme(uri: Uri) {
        try {
            val urlString = uri.toString()
            val intent = if (uri.scheme?.lowercase() == "intent") {
                Intent.parseUri(urlString, Intent.URI_INTENT_SCHEME).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackUrl = uri.getQueryParameter("browser_fallback_url")
                if (!fallbackUrl.isNullOrBlank()) {
                    loadUrl(fallbackUrl)
                }
            } catch (e2: Exception) {}
        }
    }

    private fun destroyTabWebView(tab: Tab) {
        tab.webView?.let { wv ->
            wv.stopLoading()
            wv.clearHistory()
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.destroy()
        }
        tab.webView = null
    }

    /**
     * Tabs are mutated in place (WebView callbacks hold the same object the list holds),
     * so there is no tab.copy() any more. The old copy approach let a stale copy overwrite
     * newer state, which is why Home flipped back and the loader never moved.
     * Bumping the version makes the UI re-read the tab fields.
     */
    private fun notifyTabUpdated(@Suppress("UNUSED_PARAMETER") tab: Tab) {
        _tabsVersion.update { it + 1 }
    }

    /**
     * If this tab started loading from the home screen, hide home now that the new page
     * has painted (or finished / failed), so the old page is never shown in between.
     */
    private fun leaveHomeIfPending(tab: Tab) {
        if (pendingHomeExit.remove(tab.id)) {
            tab.showHome = false
            notifyTabUpdated(tab)
        }
    }

    /**
     * When the user switches tabs, a tab that was still waiting to leave the home screen
     * (its page loaded in the background, where paint callbacks may never fire) must not
     * stay stuck on home. Release every pending tab except the current one, and also the
     * current one if its page has already fully loaded.
     */
    private fun releasePendingHomeExcept(currentId: String) {
        if (pendingHomeExit.isEmpty()) return
        val all = _normalTabs.value + _privateTabs.value
        var changed = false
        for (id in pendingHomeExit.toList()) {
            val tab = all.firstOrNull { it.id == id }
            val loaded = (tab?.webView?.progress ?: 0) >= 100
            if (id == currentId && !loaded) continue
            pendingHomeExit.remove(id)
            if (tab != null) {
                tab.showHome = false
                changed = true
            }
        }
        if (changed) _tabsVersion.update { it + 1 }
    }
}
