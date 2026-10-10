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
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
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
                            if (isGlType(type)) {
                                return null;
                            }
                            return origGetCtx.apply(this, arguments);
                        };
                    }

                    if (typeof OffscreenCanvas !== 'undefined' && OffscreenCanvas.prototype && OffscreenCanvas.prototype.getContext) {
                        var origOffscreenGetCtx = OffscreenCanvas.prototype.getContext;
                        OffscreenCanvas.prototype.getContext = function(type, attrs) {
                            if (isGlType(type)) {
                                return null;
                            }
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
    }

    fun selectTab(id: String) {
        val isPrivate = _privateTabs.value.any { it.id == id }
        _isPrivateMode.value = isPrivate
        _currentTabId.value = id
        updateCurrentTabInRoom(id)
    }

    fun createNewTab(isPrivate: Boolean, initialUrl: String = "", openInBackground: Boolean = false): Tab {
        val newTab = Tab(
            id = UUID.randomUUID().toString(),
            url = initialUrl,
            title = if (initialUrl.isNotBlank()) "Loading…" else "New Tab",
            isPrivate = isPrivate,
            isDesktopMode = currentSettings.desktopSiteDefault
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
            // Persist to Room
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

        if (initialUrl.isNotBlank()) {
            getOrCreateWebView(newTab)
            loadUrlInTab(newTab, initialUrl)
        }

        return newTab
    }

    fun closeTab(id: String) {
        val isPrivate = _privateTabs.value.any { it.id == id }
        if (isPrivate) {
            val tab = _privateTabs.value.firstOrNull { it.id == id }
            tab?.let { destroyTabWebView(it) }
            val remaining = _privateTabs.value.filter { it.id != id }
            _privateTabs.value = remaining
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
            tab?.let { destroyTabWebView(it) }
            val remaining = _normalTabs.value.filter { it.id != id }
            _normalTabs.value = remaining

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
            // Localhost or IP address
            processedUrl = "http://$processedUrl"
        } else if (processedUrl.contains(".") && !processedUrl.contains(" ") && !processedUrl.contains("\n")) {
            // Domain input (e.g. wikipedia.org or example.com:8080)
            processedUrl = "https://$processedUrl"
        } else {
            // DuckDuckGo search with UTF-8 encoding
            processedUrl = DuckDuckGoApi.buildSearchUrl(processedUrl)
        }

        // HTTPS upgrade check
        if (currentSettings.httpsUpgradeEnabled && processedUrl.startsWith("http://") && !processedUrl.contains("localhost") && !processedUrl.contains("127.0.0.1")) {
            processedUrl = "https://" + processedUrl.substring(7)
        }

        tab.url = processedUrl
        tab.title = "Loading…"
        tab.isHttps = processedUrl.startsWith("https://")
        tab.hasSslError = false
        tab.hasCrashError = false
        tab.crashRecoveryAttempts = 0
        notifyTabUpdated(tab)

        val webView = getOrCreateWebView(tab)
        webView.loadUrl(processedUrl)
    }

    fun reloadCurrentTab() {
        val tab = getCurrentTab() ?: return
        tab.hasCrashError = false
        tab.crashRecoveryAttempts = 0
        val currentUrl = tab.url
        val wv = tab.webView
        if (wv != null && !wv.url.isNullOrBlank()) {
            try {
                wv.reload()
            } catch (e: Exception) {
                val newWv = getOrCreateWebView(tab)
                newWv.loadUrl(currentUrl)
            }
        } else if (currentUrl.isNotBlank()) {
            val newWv = getOrCreateWebView(tab)
            newWv.loadUrl(currentUrl)
        }
        notifyTabUpdated(tab)
    }

    fun goBackCurrentTab(): Boolean {
        val webView = getCurrentTab()?.webView
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
        val webView = getOrCreateWebView(tab)
        webView.settings.userAgentString = if (tab.isDesktopMode) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
        webView.reload()
        notifyTabUpdated(tab)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(tab: Tab, customContext: Context? = null): WebView {
        if (tab.webView != null) {
            return tab.webView!!
        }

        val targetContext = customContext ?: activityContext ?: context
        com.example.WaterFoxApp.ensureWebViewCacheDirectories(targetContext)
        val webView = WebView(targetContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFocusable = true
            isFocusableInTouchMode = true
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

        // Inject WebGL safe script before any page script executes to prevent Mesa framebuffer crashes
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try {
                WebViewCompat.addDocumentStartJavaScript(webView, WEBGL_SAFE_SCRIPT, setOf("*"))
            } catch (e: Exception) {
                Log.w("TabManager", "addDocumentStartJavaScript failed", e)
            }
            try {
                val autoScript = getAutomationScript()
                if (autoScript.isNotBlank()) {
                    WebViewCompat.addDocumentStartJavaScript(webView, autoScript, setOf("*"))
                }
            } catch (e: Exception) {
                Log.w("TabManager", "addDocumentStartJavaScript automation failed", e)
            }
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

                // Check external schemes (mailto:, tel:, sms:, geo:, intent:, market:)
                val scheme = uri.scheme?.lowercase() ?: ""
                if (scheme in listOf("mailto", "tel", "sms", "geo", "market", "intent")) {
                    handleExternalScheme(uri)
                    return true
                }

                // If target url starts with http/https
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

                // Check Tracker blocking
                if (currentSettings.trackerBlockingEnabled) {
                    val trackerHost = trackerBlocker.isTracker(requestUrl, mainUrl)
                    if (trackerHost != null) {
                        synchronized(tab) {
                            tab.blockedTrackersCount++
                            val list = tab.blockedTrackersList.toMutableList()
                            if (!list.contains(trackerHost)) {
                                list.add(trackerHost)
                            }
                            tab.blockedTrackersList = list
                        }
                        scope.launch(Dispatchers.Main) {
                            notifyTabUpdated(tab)
                        }

                        // Save to Room for statistics banner
                        scope.launch(Dispatchers.IO) {
                            database.blockedTrackerDao().insertBlockedTracker(
                                BlockedTrackerEntity(
                                    domain = trackerHost,
                                    pageUrl = mainUrl
                                )
                            )
                        }

                        // Block tracker by returning empty response
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
                notifyTabUpdated(tab)

                try {
                    view?.evaluateJavascript(WEBGL_SAFE_SCRIPT, null)
                } catch (e: Exception) {}
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

                // Inject fallback automation script
                try {
                    val script = getAutomationScript()
                    if (script.isNotBlank()) {
                        view?.evaluateJavascript(script, null)
                    }
                } catch (e: Exception) {}

                // Capture thumbnail for tab switcher safely
                captureThumbnail(view, tab)

                notifyTabUpdated(tab)

                // Save to Room History and TabEntity if normal tab
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
                    }
                }
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
                    notifyTabUpdated(tab)
                }
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                val didCrash = detail?.didCrash() ?: true
                Log.e("TabManager", "onRenderProcessGone detected (crashed=$didCrash, attempts=${tab.crashRecoveryAttempts})")

                view?.let { crashedView ->
                    (crashedView.parent as? ViewGroup)?.removeView(crashedView)
                    try {
                        crashedView.destroy()
                    } catch (e: Exception) {
                        Log.e("TabManager", "Error destroying crashed WebView", e)
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
                        val restoredWebView = getOrCreateWebView(tab, activityContext)
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
                notifyTabUpdated(tab)
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
                val newWebView = getOrCreateWebView(newTab)
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

        // Safe auto-load for restored tabs
        if (tab.url.isNotBlank() && !tab.url.startsWith("about:") && webView.url == null) {
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
            mediaPlaybackRequiresUserGesture = false
            textZoom = currentSettings.textSizePercent
            userAgentString = if (isDesktop) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
            allowFileAccess = false
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
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

    private fun captureThumbnail(view: WebView?, tab: Tab) {
        if (view == null || view.width <= 0 || view.height <= 0) return
        try {
            val width = 280
            val height = 400
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
            // If app not installed for intent://, try fallback URL if provided
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

    private fun notifyTabUpdated(tab: Tab) {
        if (tab.isPrivate) {
            _privateTabs.value = _privateTabs.value.map { if (it.id == tab.id) tab.copy() else it }
        } else {
            _normalTabs.value = _normalTabs.value.map { if (it.id == tab.id) tab.copy() else it }
        }
    }
}
