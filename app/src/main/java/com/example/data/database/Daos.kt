package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs ORDER BY lastAccessedAt DESC")
    fun getAllTabs(): Flow<List<TabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntity)

    @Update
    suspend fun updateTab(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteTabById(id: String)

    @Query("DELETE FROM tabs")
    suspend fun deleteAllTabs()

    @Query("DELETE FROM tabs WHERE isPrivate = 1")
    suspend fun deleteAllPrivateTabs()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE (folderId IS NULL AND :folderId IS NULL) OR folderId = :folderId ORDER BY createdAt DESC")
    fun getBookmarksInFolder(folderId: Long?): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%'")
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("SELECT * FROM bookmark_folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<BookmarkFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: BookmarkFolderEntity): Long

    @Update
    suspend fun updateFolder(folder: BookmarkFolderEntity)

    @Delete
    suspend fun deleteFolder(folder: BookmarkFolderEntity)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY orderIndex ASC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity): Long

    @Update
    suspend fun updateFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAt DESC")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryEntity)

    @Delete
    suspend fun deleteHistoryItem(item: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history WHERE id IN (:ids)")
    suspend fun deleteHistoryByIds(ids: List<Long>)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
}

@Dao
interface BlockedTrackerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedTracker(item: BlockedTrackerEntity)

    @Query("SELECT * FROM blocked_trackers WHERE pageUrl = :pageUrl ORDER BY blockedAt DESC")
    fun getBlockedTrackersForPage(pageUrl: String): Flow<List<BlockedTrackerEntity>>

    @Query("SELECT COUNT(*) FROM blocked_trackers WHERE blockedAt >= :sinceTimestamp")
    fun getBlockedCountSince(sinceTimestamp: Long): Flow<Int>

    @Query("DELETE FROM blocked_trackers")
    suspend fun clearAllTrackers()
}

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY siteDomain ASC, title ASC")
    fun getAllPasswords(): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE siteDomain LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%'")
    fun searchPasswords(query: String): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE siteDomain = :domain")
    fun getPasswordsForDomain(domain: String): Flow<List<PasswordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordEntity): Long

    @Update
    suspend fun updatePassword(password: PasswordEntity)

    @Delete
    suspend fun deletePassword(password: PasswordEntity)
}

@Dao
interface BrokenSiteReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: BrokenSiteReportEntity)

    @Query("SELECT * FROM broken_site_reports ORDER BY reportedAt DESC")
    fun getAllReports(): Flow<List<BrokenSiteReportEntity>>
}

@Dao
interface SiteProtectionDao {
    @Query("SELECT EXISTS(SELECT 1 FROM site_protection_allowlist WHERE domain = :domain AND isProtectionDisabled = 1)")
    suspend fun isProtectionDisabled(domain: String): Boolean

    @Query("SELECT * FROM site_protection_allowlist")
    fun getAllowlist(): Flow<List<SiteProtectionAllowlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setProtectionDisabled(item: SiteProtectionAllowlistEntity)

    @Query("DELETE FROM site_protection_allowlist WHERE domain = :domain")
    suspend fun removeProtectionOverride(domain: String)
}

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automation_datasets ORDER BY createdAt DESC")
    fun getAllDatasets(): Flow<List<AutomationDatasetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataset(dataset: AutomationDatasetEntity)

    @Update
    suspend fun updateDataset(dataset: AutomationDatasetEntity)

    @Query("DELETE FROM automation_datasets WHERE id = :id")
    suspend fun deleteDataset(id: String)

    @Query("UPDATE automation_datasets SET isActive = 0")
    suspend fun clearActiveDatasets()

    @Query("UPDATE automation_datasets SET isActive = (id = :datasetId)")
    suspend fun setActiveDataset(datasetId: String)

    @Query("UPDATE automation_datasets SET currentIndex = 0 WHERE id = :datasetId")
    suspend fun resetDatasetPointer(datasetId: String)

    @Query("UPDATE automation_datasets SET currentIndex = :newIndex WHERE id = :datasetId")
    suspend fun updateDatasetPointer(datasetId: String, newIndex: Int)

    @Query("SELECT * FROM automation_records ORDER BY orderIndex ASC")
    fun getAllRecords(): Flow<List<AutomationRecordEntity>>

    @Query("SELECT * FROM automation_records WHERE datasetId = :datasetId ORDER BY orderIndex ASC")
    fun getRecordsForDataset(datasetId: String): Flow<List<AutomationRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AutomationRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AutomationRecordEntity)

    @Update
    suspend fun updateRecord(record: AutomationRecordEntity)

    @Query("DELETE FROM automation_records WHERE id = :id")
    suspend fun deleteRecord(id: String)

    @Query("DELETE FROM automation_records WHERE datasetId = :datasetId")
    suspend fun deleteRecordsForDataset(datasetId: String)

    @Query("UPDATE automation_records SET isUsed = 0 WHERE datasetId = :datasetId")
    suspend fun markAllRecordsUnused(datasetId: String)

    @Transaction
    suspend fun saveDatasetWithRecords(dataset: AutomationDatasetEntity, records: List<AutomationRecordEntity>) {
        if (dataset.isActive) {
            clearActiveDatasets()
        }
        insertDataset(dataset)
        deleteRecordsForDataset(dataset.id)
        insertRecords(records)
    }

    @Transaction
    suspend fun clearAllAutomationData() {
        deleteAllRecords()
        deleteAllDatasets()
    }

    @Query("DELETE FROM automation_records")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM automation_datasets")
    suspend fun deleteAllDatasets()
}
