package com.example.automation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.database.AppDatabase
import com.example.data.database.AutomationDatasetEntity
import com.example.data.database.AutomationRecordEntity
import com.example.data.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.automationDataStore: DataStore<Preferences> by preferencesDataStore(name = "waterfox_automation")

class AutomationRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val cryptoManager: CryptoManager = CryptoManager()
) {

    private object Keys {
        val DETECTION_ENABLED = booleanPreferencesKey("detection_enabled")
        val SHOW_OMNIBAR_ICON = booleanPreferencesKey("show_omnibar_icon")
        val HIGHLIGHT_FIELDS = booleanPreferencesKey("highlight_fields")
        val AUTO_SUBMIT = booleanPreferencesKey("auto_submit")
        val DISABLED_SITES = stringSetPreferencesKey("disabled_sites")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
    }

    val settingsFlow: Flow<AutomationSettings> = context.automationDataStore.data.map { prefs ->
        AutomationSettings(
            detectionEnabled = prefs[Keys.DETECTION_ENABLED] ?: true,
            showOmnibarIcon = prefs[Keys.SHOW_OMNIBAR_ICON] ?: true,
            highlightFilledFields = prefs[Keys.HIGHLIGHT_FIELDS] ?: true,
            autoSubmitAfterFilling = prefs[Keys.AUTO_SUBMIT] ?: false,
            disabledSites = prefs[Keys.DISABLED_SITES] ?: emptySet(),
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK] ?: false
        )
    }

    val datasetsFlow: Flow<List<Dataset>> = database.automationDao().getAllDatasets().map { entities ->
        entities.map { entity ->
            Dataset(
                id = entity.id,
                name = entity.name,
                sourceFileName = entity.sourceFileName,
                usageMode = try {
                    UsageMode.valueOf(entity.usageMode)
                } catch (e: Exception) {
                    UsageMode.SEQUENTIAL_TOP
                },
                skipAlreadyUsed = entity.skipAlreadyUsed,
                deleteAfterUse = entity.deleteAfterUse,
                loopBackToStart = entity.loopBackToStart,
                markAsUsedOnSubmit = entity.markAsUsedOnSubmit,
                currentIndex = entity.currentIndex,
                isActive = entity.isActive,
                createdAt = entity.createdAt
            )
        }
    }

    val recordsFlow: Flow<List<DatasetRecord>> = database.automationDao().getAllRecords().map { entities ->
        entities.map { entity ->
            val json = cryptoManager.decrypt(entity.encryptedFields, entity.ivBase64)
            val fields = parseFields(json)
            DatasetRecord(
                id = entity.id,
                datasetId = entity.datasetId,
                fields = fields,
                isUsed = entity.isUsed,
                orderIndex = entity.orderIndex
            )
        }
    }

    suspend fun updateSettings(settings: AutomationSettings) {
        context.automationDataStore.edit { prefs ->
            prefs[Keys.DETECTION_ENABLED] = settings.detectionEnabled
            prefs[Keys.SHOW_OMNIBAR_ICON] = settings.showOmnibarIcon
            prefs[Keys.HIGHLIGHT_FIELDS] = settings.highlightFilledFields
            prefs[Keys.AUTO_SUBMIT] = settings.autoSubmitAfterFilling
            prefs[Keys.DISABLED_SITES] = settings.disabledSites
            prefs[Keys.BIOMETRIC_LOCK] = settings.biometricLockEnabled
        }
    }

    suspend fun saveDatasetWithRecords(dataset: Dataset, records: List<DatasetRecord>) {
        val datasetEntity = AutomationDatasetEntity(
            id = dataset.id,
            name = dataset.name,
            sourceFileName = dataset.sourceFileName,
            usageMode = dataset.usageMode.name,
            skipAlreadyUsed = dataset.skipAlreadyUsed,
            deleteAfterUse = dataset.deleteAfterUse,
            loopBackToStart = dataset.loopBackToStart,
            markAsUsedOnSubmit = dataset.markAsUsedOnSubmit,
            currentIndex = dataset.currentIndex,
            isActive = dataset.isActive,
            createdAt = dataset.createdAt
        )

        val recordEntities = records.map { record ->
            val serialized = serializeFields(record.fields)
            val (encrypted, iv) = cryptoManager.encrypt(serialized)
            AutomationRecordEntity(
                id = record.id,
                datasetId = dataset.id,
                encryptedFields = encrypted,
                ivBase64 = iv,
                isUsed = record.isUsed,
                orderIndex = record.orderIndex
            )
        }

        database.automationDao().saveDatasetWithRecords(datasetEntity, recordEntities)
    }

    suspend fun updateDataset(dataset: Dataset) {
        val entity = AutomationDatasetEntity(
            id = dataset.id,
            name = dataset.name,
            sourceFileName = dataset.sourceFileName,
            usageMode = dataset.usageMode.name,
            skipAlreadyUsed = dataset.skipAlreadyUsed,
            deleteAfterUse = dataset.deleteAfterUse,
            loopBackToStart = dataset.loopBackToStart,
            markAsUsedOnSubmit = dataset.markAsUsedOnSubmit,
            currentIndex = dataset.currentIndex,
            isActive = dataset.isActive,
            createdAt = dataset.createdAt
        )
        database.automationDao().updateDataset(entity)
    }

    suspend fun setActiveDataset(datasetId: String) {
        database.automationDao().setActiveDataset(datasetId)
    }

    suspend fun deleteDataset(datasetId: String) {
        database.automationDao().deleteRecordsForDataset(datasetId)
        database.automationDao().deleteDataset(datasetId)
    }

    suspend fun deleteRecord(recordId: String) {
        database.automationDao().deleteRecord(recordId)
    }

    suspend fun updateRecord(record: DatasetRecord) {
        val serialized = serializeFields(record.fields)
        val (encrypted, iv) = cryptoManager.encrypt(serialized)
        val entity = AutomationRecordEntity(
            id = record.id,
            datasetId = record.datasetId,
            encryptedFields = encrypted,
            ivBase64 = iv,
            isUsed = record.isUsed,
            orderIndex = record.orderIndex
        )
        database.automationDao().updateRecord(entity)
    }

    suspend fun insertRecord(record: DatasetRecord) {
        val serialized = serializeFields(record.fields)
        val (encrypted, iv) = cryptoManager.encrypt(serialized)
        val entity = AutomationRecordEntity(
            id = record.id,
            datasetId = record.datasetId,
            encryptedFields = encrypted,
            ivBase64 = iv,
            isUsed = record.isUsed,
            orderIndex = record.orderIndex
        )
        database.automationDao().insertRecord(entity)
    }

    suspend fun markAllRecordsUnused(datasetId: String) {
        database.automationDao().markAllRecordsUnused(datasetId)
        database.automationDao().resetDatasetPointer(datasetId)
    }

    suspend fun resetPointer(datasetId: String) {
        database.automationDao().resetDatasetPointer(datasetId)
    }

    suspend fun updatePointer(datasetId: String, newIndex: Int) {
        database.automationDao().updateDatasetPointer(datasetId, newIndex)
    }

    suspend fun wipeAllAutomationData() {
        database.automationDao().clearAllAutomationData()
    }

    private fun serializeFields(fields: Map<String, String>): String {
        val obj = JSONObject()
        fields.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    private fun parseFields(json: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val obj = JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getString(k)
            }
        } catch (e: Exception) {}
        return map
    }
}
