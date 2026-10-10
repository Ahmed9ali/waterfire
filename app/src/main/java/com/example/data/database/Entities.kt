package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val isPrivate: Boolean,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val isCurrent: Boolean
)

@Entity(tableName = "bookmark_folders")
data class BookmarkFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentFolderId: Long? = null
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val folderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val orderIndex: Int = 0
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_trackers")
data class BlockedTrackerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val pageUrl: String,
    val blockedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val siteDomain: String,
    val username: String,
    val encryptedPassword: String,
    val ivBase64: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "broken_site_reports")
data class BrokenSiteReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val reason: String,
    val description: String,
    val reportedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "site_protection_allowlist")
data class SiteProtectionAllowlistEntity(
    @PrimaryKey val domain: String,
    val isProtectionDisabled: Boolean = true
)

@Entity(tableName = "automation_datasets")
data class AutomationDatasetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourceFileName: String,
    val usageMode: String,
    val skipAlreadyUsed: Boolean,
    val deleteAfterUse: Boolean,
    val loopBackToStart: Boolean,
    val markAsUsedOnSubmit: Boolean,
    val currentIndex: Int,
    val isActive: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "automation_records",
    indices = [Index(value = ["datasetId"])]
)
data class AutomationRecordEntity(
    @PrimaryKey val id: String,
    val datasetId: String,
    val encryptedFields: String,
    val ivBase64: String,
    val isUsed: Boolean,
    val orderIndex: Int
)
