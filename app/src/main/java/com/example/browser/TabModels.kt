package com.example.browser

import android.graphics.Bitmap
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebView
import java.util.UUID

data class Tab(
    val id: String = UUID.randomUUID().toString(),
    var url: String = "",
    var title: String = "New Tab",
    val isPrivate: Boolean = false,
    var progress: Int = 0,
    var isLoading: Boolean = false,
    var canGoBack: Boolean = false,
    var canGoForward: Boolean = false,
    var isHttps: Boolean = false,
    var hasSslError: Boolean = false,
    var blockedTrackersCount: Int = 0,
    var blockedTrackersList: List<String> = emptyList(),
    var thumbnail: Bitmap? = null,
    var webView: WebView? = null,
    var isDesktopMode: Boolean = false,
    var hasCrashError: Boolean = false,
    var crashRecoveryAttempts: Int = 0,
    var showHome: Boolean = false
)

data class ClosedTabInfo(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val isPrivate: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class JsDialogState {
    data class Alert(val message: String, val result: JsResult) : JsDialogState()
    data class Confirm(val message: String, val result: JsResult) : JsDialogState()
    data class Prompt(val message: String, val defaultValue: String, val result: JsPromptResult) : JsDialogState()
}

data class SslErrorState(
    val url: String,
    val handler: SslErrorHandler
)

data class WebPermissionState(
    val origin: String,
    val resources: Array<String>,
    val request: PermissionRequest
)

data class GeolocationPermissionState(
    val origin: String,
    val callback: GeolocationPermissions.Callback
)

data class FileChooserState(
    val filePathCallback: ValueCallback<Array<android.net.Uri>>,
    val mimeTypes: Array<String>,
    val isMultiple: Boolean
)

data class LinkContextMenuState(
    val url: String,
    val title: String,
    val isImage: Boolean = false,
    val imageUrl: String? = null
)
