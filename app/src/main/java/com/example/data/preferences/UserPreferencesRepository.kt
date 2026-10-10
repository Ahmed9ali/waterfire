package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "waterfox_settings")

enum class AddressBarPosition {
    TOP,
    BOTTOM
}

data class BrowserSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val addressBarPosition: AddressBarPosition = AddressBarPosition.TOP,
    val trackerBlockingEnabled: Boolean = true,
    val httpsUpgradeEnabled: Boolean = true,
    val sendGpcHeader: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val clearDataOnExit: Boolean = false,
    val autoClearTimeoutMinutes: Int = 0, // 0 = never, 5, 15, 30, 60, 1440
    val javascriptEnabled: Boolean = true,
    val desktopSiteDefault: Boolean = false,
    val openLinksInBackground: Boolean = false,
    val blockPopups: Boolean = true,
    val textSizePercent: Int = 100,
    val trackerBannerDismissed: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ADDRESS_BAR_POSITION = stringPreferencesKey("address_bar_position")
        val TRACKER_BLOCKING_ENABLED = booleanPreferencesKey("tracker_blocking_enabled")
        val HTTPS_UPGRADE_ENABLED = booleanPreferencesKey("https_upgrade_enabled")
        val SEND_GPC_HEADER = booleanPreferencesKey("send_gpc_header")
        val BLOCK_THIRD_PARTY_COOKIES = booleanPreferencesKey("block_third_party_cookies")
        val CLEAR_DATA_ON_EXIT = booleanPreferencesKey("clear_data_on_exit")
        val AUTO_CLEAR_TIMEOUT_MINUTES = intPreferencesKey("auto_clear_timeout_minutes")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val DESKTOP_SITE_DEFAULT = booleanPreferencesKey("desktop_site_default")
        val OPEN_LINKS_IN_BACKGROUND = booleanPreferencesKey("open_links_in_background")
        val BLOCK_POPUPS = booleanPreferencesKey("block_popups")
        val TEXT_SIZE_PERCENT = intPreferencesKey("text_size_percent")
        val TRACKER_BANNER_DISMISSED = booleanPreferencesKey("tracker_banner_dismissed")
    }

    val settingsFlow: Flow<BrowserSettings> = context.dataStore.data.map { preferences ->
        val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.DARK.name
        val addressBarPosStr = preferences[PreferencesKeys.ADDRESS_BAR_POSITION] ?: AddressBarPosition.TOP.name

        BrowserSettings(
            themeMode = try { ThemeMode.valueOf(themeModeStr) } catch (e: Exception) { ThemeMode.DARK },
            addressBarPosition = try { AddressBarPosition.valueOf(addressBarPosStr) } catch (e: Exception) { AddressBarPosition.TOP },
            trackerBlockingEnabled = preferences[PreferencesKeys.TRACKER_BLOCKING_ENABLED] ?: true,
            httpsUpgradeEnabled = preferences[PreferencesKeys.HTTPS_UPGRADE_ENABLED] ?: true,
            sendGpcHeader = preferences[PreferencesKeys.SEND_GPC_HEADER] ?: true,
            blockThirdPartyCookies = preferences[PreferencesKeys.BLOCK_THIRD_PARTY_COOKIES] ?: true,
            clearDataOnExit = preferences[PreferencesKeys.CLEAR_DATA_ON_EXIT] ?: false,
            autoClearTimeoutMinutes = preferences[PreferencesKeys.AUTO_CLEAR_TIMEOUT_MINUTES] ?: 0,
            javascriptEnabled = preferences[PreferencesKeys.JAVASCRIPT_ENABLED] ?: true,
            desktopSiteDefault = preferences[PreferencesKeys.DESKTOP_SITE_DEFAULT] ?: false,
            openLinksInBackground = preferences[PreferencesKeys.OPEN_LINKS_IN_BACKGROUND] ?: false,
            blockPopups = preferences[PreferencesKeys.BLOCK_POPUPS] ?: true,
            textSizePercent = preferences[PreferencesKeys.TEXT_SIZE_PERCENT] ?: 100,
            trackerBannerDismissed = preferences[PreferencesKeys.TRACKER_BANNER_DISMISSED] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setAddressBarPosition(position: AddressBarPosition) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ADDRESS_BAR_POSITION] = position.name
        }
    }

    suspend fun setTrackerBlockingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRACKER_BLOCKING_ENABLED] = enabled
        }
    }

    suspend fun setHttpsUpgradeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HTTPS_UPGRADE_ENABLED] = enabled
        }
    }

    suspend fun setSendGpcHeader(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEND_GPC_HEADER] = enabled
        }
    }

    suspend fun setBlockThirdPartyCookies(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BLOCK_THIRD_PARTY_COOKIES] = enabled
        }
    }

    suspend fun setClearDataOnExit(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLEAR_DATA_ON_EXIT] = enabled
        }
    }

    suspend fun setAutoClearTimeoutMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CLEAR_TIMEOUT_MINUTES] = minutes
        }
    }

    suspend fun setJavascriptEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.JAVASCRIPT_ENABLED] = enabled
        }
    }

    suspend fun setDesktopSiteDefault(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DESKTOP_SITE_DEFAULT] = enabled
        }
    }

    suspend fun setOpenLinksInBackground(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OPEN_LINKS_IN_BACKGROUND] = enabled
        }
    }

    suspend fun setBlockPopups(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BLOCK_POPUPS] = enabled
        }
    }

    suspend fun setTextSizePercent(percent: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_SIZE_PERCENT] = percent
        }
    }

    suspend fun setTrackerBannerDismissed(dismissed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRACKER_BANNER_DISMISSED] = dismissed
        }
    }
}
