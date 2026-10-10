package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads site icons for the home screen shortcuts.
 * Memory cache first, then a disk cache in cacheDir, then the network (once per site).
 */
object FaviconLoader {

    private val memory = LruCache<String, Bitmap>(64)

    fun peek(host: String): Bitmap? = memory.get(host)

    suspend fun load(context: Context, host: String): Bitmap? = withContext(Dispatchers.IO) {
        if (host.isBlank()) return@withContext null
        memory.get(host)?.let { return@withContext it }

        val dir = File(context.cacheDir, "favicons").apply { mkdirs() }
        val safeName = host.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val file = File(dir, "$safeName.png")

        if (file.exists() && file.length() > 0) {
            val cached = BitmapFactory.decodeFile(file.absolutePath)
            if (cached != null) {
                memory.put(host, cached)
                return@withContext cached
            }
        }

        try {
            val connection = (URL("https://www.google.com/s2/favicons?domain=$host&sz=128")
                .openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
            }
            val bytes = connection.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                input.copyTo(out)
                out.toByteArray()
            }
            connection.disconnect()

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                memory.put(host, bitmap)
                try {
                    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                } catch (e: Exception) {
                    // cache write failed, the icon still shows
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}

/** Returns the site's icon, or null while loading / if it is not available. */
@Composable
fun rememberFavicon(host: String): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val state: State<ImageBitmap?> = produceState(
        initialValue = FaviconLoader.peek(host)?.asImageBitmap(),
        key1 = host
    ) {
        value = FaviconLoader.load(context, host)?.asImageBitmap()
    }
    return state.value
}
