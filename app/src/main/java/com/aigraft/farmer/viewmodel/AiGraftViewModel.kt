package com.aigraft.farmer.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aigraft.farmer.ai.PlantVisionAnalyzer
import com.aigraft.farmer.audio.SpeechManager
import com.aigraft.farmer.audio.SpeechState
import com.aigraft.farmer.data.GraftBatch
import com.aigraft.farmer.data.LocalStore
import com.aigraft.farmer.data.PendingScan
import com.aigraft.farmer.data.PhotoProcessor
import com.aigraft.farmer.data.ScanMode
import com.aigraft.farmer.data.ScanRecord
import com.aigraft.farmer.data.startOfDayMillis
import com.aigraft.farmer.i18n.AppLang
import com.aigraft.farmer.notifications.ReminderScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private const val TAG = "AiGraftViewModel"

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Loading : AnalysisUiState
    data class Success(val scan: ScanRecord) : AnalysisUiState
    data class Error(val message: String, val canRetry: Boolean) : AnalysisUiState
}

class AiGraftViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val store = LocalStore(appContext)
    private val analyzer = PlantVisionAnalyzer()
    private val speechManager = SpeechManager(appContext)

    private val _lang = MutableStateFlow(store.loadLang())
    val lang: StateFlow<AppLang> = _lang.asStateFlow()

    private val _scans = MutableStateFlow(store.loadScans())
    val scans: StateFlow<List<ScanRecord>> = _scans.asStateFlow()

    private val _batches = MutableStateFlow(store.loadBatches())
    val batches: StateFlow<List<GraftBatch>> = _batches.asStateFlow()

    private val _pendingScan = MutableStateFlow<PendingScan?>(null)
    val pendingScan: StateFlow<PendingScan?> = _pendingScan.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _isPreparingPhoto = MutableStateFlow(false)
    val isPreparingPhoto: StateFlow<Boolean> = _isPreparingPhoto.asStateFlow()

    val speechState: StateFlow<SpeechState> = speechManager.state

    fun setLang(newLang: AppLang) {
        if (_lang.value == newLang) return
        speechManager.stop()
        _lang.value = newLang
        store.saveLang(newLang)
        // Reschedule active reminders in the newly chosen language
        _batches.value.filter { it.reminders }.forEach { batch ->
            ReminderScheduler.scheduleBatch(appContext, batch, newLang)
        }
    }

    fun speak(id: String, text: String, langOverride: AppLang? = null) {
        speechManager.speak(id, text, langOverride ?: _lang.value)
    }

    fun stopSpeech() {
        speechManager.stop()
    }

    suspend fun preparePhotoFromUri(uri: Uri, mode: ScanMode, batchId: String? = null): Boolean {
        _isPreparingPhoto.value = true
        return try {
            val prepared = PhotoProcessor.prepareFromUri(
                context = appContext,
                uri = uri,
                mode = mode,
                batchId = batchId,
                scansDir = store.scansDir,
            )
            _pendingScan.value = prepared
            _analysisState.value = AnalysisUiState.Idle
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed preparing photo from Uri", e)
            false
        } finally {
            _isPreparingPhoto.value = false
        }
    }

    suspend fun preparePhotoFromFile(file: File, mode: ScanMode, batchId: String? = null): Boolean {
        _isPreparingPhoto.value = true
        return try {
            val prepared = PhotoProcessor.prepareFromFile(
                file = file,
                mode = mode,
                batchId = batchId,
                scansDir = store.scansDir,
            )
            _pendingScan.value = prepared
            _analysisState.value = AnalysisUiState.Idle
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed preparing photo from File", e)
            false
        } finally {
            _isPreparingPhoto.value = false
        }
    }

    fun loadExistingScan(scanId: String) {
        val found = _scans.value.firstOrNull { it.id == scanId }
        _analysisState.value = if (found != null) {
            AnalysisUiState.Success(found)
        } else {
            AnalysisUiState.Error("Scan not found.", canRetry = false)
        }
    }

    fun analyzePendingScan() {
        val pending = _pendingScan.value ?: return
        if (_analysisState.value is AnalysisUiState.Loading) return

        val currentLang = _lang.value
        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.Loading
            try {
                val result = analyzer.analyze(
                    bitmap = pending.analysisBitmap,
                    mode = pending.mode,
                    lang = currentLang,
                )
                val record = ScanRecord(
                    id = result.id,
                    mode = pending.mode,
                    createdAtMillis = result.createdAtMillis,
                    thumbPath = pending.thumbPath,
                    photoPath = pending.photoPath,
                    aspectRatio = pending.aspectRatio,
                    result = result,
                    batchId = pending.batchId,
                )
                val updated = listOf(record) + _scans.value
                _scans.value = updated
                store.saveScans(updated)
                _pendingScan.value = null
                _analysisState.value = AnalysisUiState.Success(record)
                // Auto read-aloud for farmers who cannot read well
                speechManager.speak("result-${result.id}", result.speak, AppLang.fromCode(result.lang))
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Plant analysis timed out", e)
                _analysisState.value = AnalysisUiState.Error(
                    message = "Timed out after 60 seconds. Please try again.",
                    canRetry = true,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Plant analysis failed", e)
                _analysisState.value = AnalysisUiState.Error(
                    message = e.localizedMessage ?: "Analysis failed.",
                    canRetry = true,
                )
            }
        }
    }

    fun clearAllScans() {
        store.clearScans()
        _scans.value = emptyList()
    }

    fun createBatch(
        name: String,
        plants: Int,
        daysAgo: Int,
        remindersEnabled: Boolean,
        linkedScanId: String?,
    ): GraftBatch {
        val nowDayStart = startOfDayMillis(System.currentTimeMillis())
        val graftedAt = nowDayStart - daysAgo * 86_400_000L
        val batch = GraftBatch(
            id = UUID.randomUUID().toString(),
            name = name,
            plants = plants,
            graftedAtMillis = graftedAt,
            doneDays = emptyList(),
            reminders = remindersEnabled,
        )
        val updatedBatches = listOf(batch) + _batches.value
        _batches.value = updatedBatches
        store.saveBatches(updatedBatches)

        if (remindersEnabled) {
            ReminderScheduler.scheduleBatch(appContext, batch, _lang.value)
        }

        if (!linkedScanId.isNullOrBlank()) {
            val updatedScans = _scans.value.map { scan ->
                if (scan.id == linkedScanId) scan.copy(batchId = batch.id) else scan
            }
            _scans.value = updatedScans
            store.saveScans(updatedScans)
            val current = _analysisState.value
            if (current is AnalysisUiState.Success && current.scan.id == linkedScanId) {
                _analysisState.value = AnalysisUiState.Success(current.scan.copy(batchId = batch.id))
            }
        }
        return batch
    }

    fun toggleBatchDayDone(batchId: String, day: Int) {
        val updated = _batches.value.map { batch ->
            if (batch.id != batchId) {
                batch
            } else {
                val newDone = if (day in batch.doneDays) {
                    batch.doneDays - day
                } else {
                    (batch.doneDays + day).sorted()
                }
                batch.copy(doneDays = newDone)
            }
        }
        _batches.value = updated
        store.saveBatches(updated)
    }

    fun enableReminders(batchId: String) {
        val updated = _batches.value.map { batch ->
            if (batch.id == batchId) {
                val withRem = batch.copy(reminders = true)
                ReminderScheduler.scheduleBatch(appContext, withRem, _lang.value)
                withRem
            } else {
                batch
            }
        }
        _batches.value = updated
        store.saveBatches(updated)
    }

    fun deleteBatch(batchId: String) {
        ReminderScheduler.cancelBatch(appContext, batchId)
        val updated = _batches.value.filterNot { it.id == batchId }
        _batches.value = updated
        store.saveBatches(updated)
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
    }
}
