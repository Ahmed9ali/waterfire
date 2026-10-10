package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TabEntity::class,
        BookmarkFolderEntity::class,
        BookmarkEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        BlockedTrackerEntity::class,
        PasswordEntity::class,
        BrokenSiteReportEntity::class,
        SiteProtectionAllowlistEntity::class,
        AutomationDatasetEntity::class,
        AutomationRecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tabDao(): TabDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun blockedTrackerDao(): BlockedTrackerDao
    abstract fun passwordDao(): PasswordDao
    abstract fun brokenSiteReportDao(): BrokenSiteReportDao
    abstract fun siteProtectionDao(): SiteProtectionDao
    abstract fun automationDao(): AutomationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "waterfox_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial favorites
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        val defaultFavorites = listOf(
                            FavoriteEntity(title = "DuckDuckGo", url = "https://duckduckgo.com", orderIndex = 0),
                            FavoriteEntity(title = "Wikipedia", url = "https://wikipedia.org", orderIndex = 1),
                            FavoriteEntity(title = "Hacker News", url = "https://news.ycombinator.com", orderIndex = 2),
                            FavoriteEntity(title = "GitHub", url = "https://github.com", orderIndex = 3),
                            FavoriteEntity(title = "Reddit", url = "https://reddit.com", orderIndex = 4),
                            FavoriteEntity(title = "EFF", url = "https://eff.org", orderIndex = 5)
                        )
                        defaultFavorites.forEach { database.favoriteDao().insertFavorite(it) }

                        // Initial sample bookmark
                        database.bookmarkDao().insertBookmark(
                            BookmarkEntity(
                                title = "DuckDuckGo Privacy Guidelines",
                                url = "https://spreadprivacy.com",
                                folderId = null
                            )
                        )
                    }
                }
            }
        }
    }
}
