package com.example

import android.app.Application
import android.content.Context
import android.system.Os
import android.util.Log
import java.io.File

class WaterFoxApp : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        initEnvironment()
    }

    override fun onCreate() {
        super.onCreate()
        initEnvironment()
        ensureWebViewCacheDirectories(this)
    }

    companion object {
        private const val TAG = "WaterFoxApp"

        fun initEnvironment() {
            try {
                // In cloud and virtualized container environments lacking /dev/dri/renderD128 hardware nodes,
                // configure Mesa and Gallium to use the llvmpipe software driver and redirect Mesa logs to /dev/null.
                // This prevents "E/MESA : Failed to open rendernode: No such file or directory" error messages.
                Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
                Os.setenv("GALLIUM_DRIVER", "llvmpipe", true)
                Os.setenv("MESA_LOADER_DRIVER_OVERRIDE", "llvmpipe", true)
                Os.setenv("MESA_LOG_FILE", "/dev/null", true)
                Os.setenv("GALLIUM_LOG_FILE", "/dev/null", true)
                Os.setenv("MESA_DEBUG", "silent", true)
                Os.setenv("EGL_LOG_LEVEL", "fatal", true)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to configure graphics environment variables", e)
            }
        }

        fun ensureWebViewCacheDirectories(context: Context) {
            try {
                val cacheBase = context.cacheDir ?: return
                val paths = listOf(
                    "WebView",
                    "WebView/Default",
                    "WebView/Default/HTTP Cache",
                    "WebView/Default/HTTP Cache/index-dir",
                    "WebView/Default/HTTP Cache/Code Cache",
                    "WebView/Default/HTTP Cache/Code Cache/wasm",
                    "WebView/Default/HTTP Cache/Code Cache/js",
                    "WebView/Default/Code Cache",
                    "WebView/Default/Code Cache/wasm",
                    "WebView/Default/Code Cache/js",
                    "WebView/HTTP Cache",
                    "WebView/HTTP Cache/index-dir",
                    "WebView/HTTP Cache/Code Cache",
                    "WebView/HTTP Cache/Code Cache/wasm",
                    "WebView/HTTP Cache/Code Cache/js",
                    "WebView/Code Cache",
                    "WebView/Code Cache/wasm",
                    "WebView/Code Cache/js"
                )
                for (sub in paths) {
                    val dir = File(cacheBase, sub)
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to ensure WebView cache directories", e)
            }
        }
    }
}
