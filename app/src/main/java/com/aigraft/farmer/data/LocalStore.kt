package com.aigraft.farmer.data

import android.content.Context
import android.util.Log
import com.aigraft.farmer.i18n.AppLang
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val TAG = "LocalStore"
private const val PREFS_NAME = "ai_graft_store"
private const val KEY_LANG = "app_lang"
private const val KEY_SCANS = "scan_history_v1"
private const val KEY_BATCHES = "graft_batches_v1"
private const val MAX_SCANS = 30

class LocalStore(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val scansDir: File by lazy {
        File(context.filesDir, "scans").apply { mkdirs() }
    }

    fun loadLang(): AppLang = AppLang.fromCode(prefs.getString(KEY_LANG, AppLang.HI.code))

    fun saveLang(lang: AppLang) {
        prefs.edit().putString(KEY_LANG, lang.code).apply()
    }

    fun loadScans(): List<ScanRecord> {
        val raw = prefs.getString(KEY_SCANS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<ScanRecord>>(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode scans", e)
            emptyList()
        }
    }

    fun saveScans(scans: List<ScanRecord>) {
        val trimmed = scans.take(MAX_SCANS)
        val dropped = scans.drop(MAX_SCANS)
        dropped.forEach {
            runCatching { File(it.photoPath).delete() }
            runCatching { File(it.thumbPath).delete() }
        }
        prefs.edit().putString(KEY_SCANS, json.encodeToString(trimmed)).apply()
    }

    fun clearScans() {
        val existing = loadScans()
        existing.forEach {
            runCatching { File(it.photoPath).delete() }
            runCatching { File(it.thumbPath).delete() }
        }
        prefs.edit().remove(KEY_SCANS).apply()
    }

    fun loadBatches(): List<GraftBatch> {
        val raw = prefs.getString(KEY_BATCHES, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<GraftBatch>>(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode batches", e)
            emptyList()
        }
    }

    fun saveBatches(batches: List<GraftBatch>) {
        prefs.edit().putString(KEY_BATCHES, json.encodeToString(batches)).apply()
    }
}
