package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.FireBurningAnimation
import com.example.ui.components.GeolocationDialog
import com.example.ui.components.JsDialogRenderer
import com.example.ui.components.SslErrorDialog
import com.example.ui.components.WebPermissionDialog
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.BrokenSiteReportDialog
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PasswordsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TabSwitcherScreen
import com.example.ui.theme.WaterFoxTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WaterFoxApp.initEnvironment()
        WaterFoxApp.ensureWebViewCacheDirectories(this)
        enableEdgeToEdge()

        viewModel.tabManager.activityContext = this
        handleIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val isPrivateMode by viewModel.tabManager.isPrivateMode.collectAsState()

            WaterFoxTheme(
                themeMode = settings.themeMode,
                isPrivateTab = isPrivateMode
            ) {
                MainContent(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.tabManager.resumeAllWebViews()
    }

    override fun onPause() {
        super.onPause()
        viewModel.tabManager.pauseAllWebViews()
    }

    override fun onDestroy() {
        if (isFinishing) {
            viewModel.tabManager.activityContext = null
            viewModel.tabManager.destroyAllWebViews()
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null) {
                    val url = uri.toString()
                    viewModel.tabManager.createNewTab(isPrivate = false, initialUrl = url)
                    viewModel.navigateTo(AppScreen.BROWSER)
                }
            }
            Intent.ACTION_WEB_SEARCH -> {
                val query = intent.getStringExtra("query")
                if (!query.isNullOrBlank()) {
                    viewModel.tabManager.createNewTab(isPrivate = false, initialUrl = query)
                    viewModel.navigateTo(AppScreen.BROWSER)
                }
            }
            Intent.ACTION_PROCESS_TEXT -> {
                val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                if (!text.isNullOrBlank()) {
                    viewModel.tabManager.createNewTab(isPrivate = false, initialUrl = text)
                    viewModel.navigateTo(AppScreen.BROWSER)
                }
            }
        }
    }
}

@Composable
fun MainContent(viewModel: BrowserViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val showBurningAnim by viewModel.showFireBurningAnimation.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()
    val jsDialogState by viewModel.tabManager.jsDialogState.collectAsState()
    val sslErrorState by viewModel.tabManager.sslErrorState.collectAsState()
    val webPermissionState by viewModel.tabManager.webPermissionState.collectAsState()
    val geoPermissionState by viewModel.tabManager.geolocationState.collectAsState()
    val fileChooserState by viewModel.tabManager.fileChooserState.collectAsState()
    val customFullScreenView by viewModel.tabManager.customFullScreenView.collectAsState()
    val showBrokenSiteDialog by viewModel.showBrokenSiteDialog.collectAsState()

    var showSplash by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    // File Chooser Launcher
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        val clipData = result.data?.clipData
        val callback = fileChooserState?.filePathCallback

        if (clipData != null) {
            val list = mutableListOf<Uri>()
            for (i in 0 until clipData.itemCount) {
                list.add(clipData.getItemAt(i).uri)
            }
            callback?.onReceiveValue(list.toTypedArray())
        } else if (uri != null) {
            callback?.onReceiveValue(arrayOf(uri))
        } else {
            callback?.onReceiveValue(null)
        }
        viewModel.tabManager.fileChooserState.value = null
    }

    LaunchedEffect(fileChooserState) {
        fileChooserState?.let { state ->
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = if (state.mimeTypes.isNotEmpty() && state.mimeTypes[0].isNotBlank()) state.mimeTypes[0] else "*/*"
                if (state.isMultiple) {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            }
            fileChooserLauncher.launch(Intent.createChooser(intent, "Choose File"))
        }
    }

    val snackbarActionMsg by viewModel.snackbarActionMessage.collectAsState()

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarMessage.value = null
        }
    }

    LaunchedEffect(snackbarActionMsg) {
        snackbarActionMsg?.let { actMsg ->
            val result = snackbarHostState.showSnackbar(
                message = actMsg.message,
                actionLabel = actMsg.actionLabel,
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                actMsg.onAction?.invoke()
            }
            viewModel.snackbarActionMessage.value = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showSplash) {
                SplashScreen(onSplashFinished = { showSplash = false })
            } else {
                when (currentScreen) {
                    AppScreen.BROWSER -> BrowserScreen(viewModel = viewModel)
                    AppScreen.TAB_SWITCHER -> TabSwitcherScreen(
                        viewModel = viewModel,
                        onTabSelected = { viewModel.navigateTo(AppScreen.BROWSER) }
                    )
                    AppScreen.BOOKMARKS -> BookmarksScreen(viewModel = viewModel)
                    AppScreen.HISTORY -> HistoryScreen(viewModel = viewModel)
                    AppScreen.PASSWORDS -> PasswordsScreen(viewModel = viewModel)
                    AppScreen.DOWNLOADS -> DownloadsScreen(viewModel = viewModel)
                    AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    AppScreen.AUTOMATION -> com.example.automation.ui.AutomationScreen(viewModel = viewModel)
                }

                // Fullscreen HTML5 Video View
                customFullScreenView?.let { customView ->
                    AndroidView(
                        factory = { customView },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }

                // Fire Burning Animation Overlay
                if (showBurningAnim) {
                    FireBurningAnimation()
                }

                // Web Dialogs
                JsDialogRenderer(
                    dialogState = jsDialogState,
                    onDismiss = { viewModel.tabManager.jsDialogState.value = null }
                )

                SslErrorDialog(
                    sslErrorState = sslErrorState,
                    onGoBack = {
                        sslErrorState?.handler?.cancel()
                        viewModel.tabManager.sslErrorState.value = null
                        viewModel.tabManager.goBackCurrentTab()
                    },
                    onProceedAnyway = {
                        sslErrorState?.handler?.proceed()
                        viewModel.tabManager.sslErrorState.value = null
                    }
                )

                WebPermissionDialog(
                    permissionState = webPermissionState,
                    onAllow = {
                        webPermissionState?.request?.grant(webPermissionState?.resources)
                        viewModel.tabManager.webPermissionState.value = null
                    },
                    onDeny = {
                        webPermissionState?.request?.deny()
                        viewModel.tabManager.webPermissionState.value = null
                    }
                )

                GeolocationDialog(
                    geoState = geoPermissionState,
                    onAllow = {
                        geoPermissionState?.callback?.invoke(geoPermissionState?.origin, true, false)
                        viewModel.tabManager.geolocationState.value = null
                    },
                    onDeny = {
                        geoPermissionState?.callback?.invoke(geoPermissionState?.origin, false, false)
                        viewModel.tabManager.geolocationState.value = null
                    }
                )

                BrokenSiteReportDialog(
                    visible = showBrokenSiteDialog,
                    currentUrl = viewModel.tabManager.getCurrentTab()?.url ?: "",
                    onSubmit = { url, reason, desc ->
                        viewModel.submitBrokenSiteReport(url, reason, desc)
                    },
                    onDismiss = { viewModel.showBrokenSiteDialog.value = false }
                )
            }
        }
    }
}
