package com.example.browser

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import java.io.File

data class DownloadItem(
    val id: Long,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val filePath: String,
    val totalBytes: Long,
    val bytesDownloaded: Long,
    val status: Int, // DownloadManager.STATUS_*
    val timestamp: Long = System.currentTimeMillis()
)

class DownloadHandler(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun startDownload(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ): Long {
        val guessedFileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val uri = Uri.parse(url)

        val request = DownloadManager.Request(uri).apply {
            setTitle(guessedFileName)
            setDescription("Downloading with WaterFox")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            addRequestHeader("User-Agent", userAgent)
            if (!mimeType.isNullOrBlank()) {
                setMimeType(mimeType)
            }
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, guessedFileName)
        }

        return downloadManager.enqueue(request)
    }

    fun getAllDownloads(): List<DownloadItem> {
        val query = DownloadManager.Query()
        val cursor = downloadManager.query(query) ?: return emptyList()
        val list = mutableListOf<DownloadItem>()

        cursor.use {
            val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
            val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI)
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val totalSizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val downloadedBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val mediaTypeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)

            while (cursor.moveToNext()) {
                val id = if (idIndex != -1) cursor.getLong(idIndex) else 0L
                val title = if (titleIndex != -1) cursor.getString(titleIndex) ?: "file" else "file"
                val url = if (uriIndex != -1) cursor.getString(uriIndex) ?: "" else ""
                val status = if (statusIndex != -1) cursor.getInt(statusIndex) else DownloadManager.STATUS_FAILED
                val totalSize = if (totalSizeIndex != -1) cursor.getLong(totalSizeIndex) else 0L
                val downloaded = if (downloadedBytesIndex != -1) cursor.getLong(downloadedBytesIndex) else 0L
                val localUri = if (localUriIndex != -1) cursor.getString(localUriIndex) ?: "" else ""
                val mime = if (mediaTypeIndex != -1) cursor.getString(mediaTypeIndex) ?: "application/octet-stream" else "application/octet-stream"

                list.add(
                    DownloadItem(
                        id = id,
                        fileName = title,
                        url = url,
                        mimeType = mime,
                        filePath = localUri,
                        totalBytes = totalSize,
                        bytesDownloaded = downloaded,
                        status = status
                    )
                )
            }
        }
        return list.reversed()
    }

    fun cancelOrRemoveDownload(id: Long) {
        downloadManager.remove(id)
    }
}
