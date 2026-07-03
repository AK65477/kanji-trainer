package com.langtrainer.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backup: ProgressBackupRepository,
) : ViewModel() {

    data class UiState(
        val busy: Boolean = false,
        val message: String? = null,
        /** Non-null while an import file is staged, awaiting the overwrite confirmation. */
        val pendingImportText: String? = null,
        /**
         * Non-null when the staged backup is *older* than this device's latest study
         * activity, i.e. importing would overwrite newer local progress. Pre-formatted
         * caution text for the confirmation dialog.
         */
        val importWarning: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private fun formatTime(epochMs: Long): String =
        if (epochMs <= 0L) "기록 없음" else timeFormat.format(Date(epochMs))

    fun exportTo(uri: Uri) = viewModelScope.launch {
        _state.update { it.copy(busy = true, message = null) }
        val ok = backup.exportToUri(uri)
        _state.update {
            it.copy(busy = false, message = if (ok) "진도를 파일로 내보냈습니다." else "내보내기에 실패했습니다.")
        }
    }

    fun stageImportFrom(uri: Uri) = viewModelScope.launch {
        _state.update { it.copy(busy = true, message = null) }
        val text = backup.readText(uri)
        if (text == null) {
            _state.update { it.copy(busy = false, message = "파일을 읽지 못했습니다.") }
            return@launch
        }
        when (val preview = backup.previewImport(text)) {
            is ProgressBackupRepository.ImportPreviewResult.Invalid ->
                _state.update { it.copy(busy = false, message = "가져올 수 없는 파일입니다: ${preview.reason}") }
            is ProgressBackupRepository.ImportPreviewResult.Ready -> {
                val p = preview.preview
                val warning = if (p.localIsNewer) {
                    "⚠️ 이 기기에 백업보다 더 최근 학습 기록이 있습니다.\n" +
                        "· 이 기기 마지막 학습: ${formatTime(p.localLatestActivityMs)}\n" +
                        "· 백업 마지막 학습: ${formatTime(p.backupLatestActivityMs)}\n" +
                        "가져오면 이 기기의 더 최신 진도가 사라집니다."
                } else {
                    null
                }
                _state.update { it.copy(busy = false, pendingImportText = p.text, importWarning = warning) }
            }
        }
    }

    fun cancelImport() = _state.update { it.copy(pendingImportText = null, importWarning = null) }

    fun confirmImport() {
        val text = _state.value.pendingImportText ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, pendingImportText = null, importWarning = null, message = null) }
            val result = backup.importJson(text)
            _state.update {
                it.copy(
                    busy = false,
                    message = when (result) {
                        is ProgressBackupRepository.ImportResult.Success ->
                            "가져오기 완료 · 상태 ${result.srsStates} · 숙련 ${result.kanjiMastery} · " +
                                "기록 ${result.reviewLogs} · 세션 ${result.sessions}. 앱을 다시 시작하면 반영됩니다."
                        is ProgressBackupRepository.ImportResult.Failure ->
                            "가져오기 실패: ${result.reason}"
                    },
                )
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
