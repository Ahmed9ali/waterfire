package com.example.automation

import android.webkit.JavascriptInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class AutomationJsInterface(
    private val scope: CoroutineScope,
    private val onFormDetectedCallback: (url: String, fieldCount: Int) -> Unit,
    private val onFormSubmittedCallback: () -> Unit
) {

    @JavascriptInterface
    fun onFormsDetected(jsonPayload: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val obj = JSONObject(jsonPayload)
                val url = obj.optString("url")
                val count = obj.optInt("count", 0)
                onFormDetectedCallback(url, count)
            } catch (e: Exception) {}
        }
    }

    @JavascriptInterface
    fun onFormSubmitted() {
        scope.launch(Dispatchers.Main) {
            onFormSubmittedCallback()
        }
    }
}
