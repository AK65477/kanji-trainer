package com.langtrainer.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

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
        } else {
            _state.update { it.copy(busy = false, pendingImportText = text) }
        }
    }

    fun cancelImport() = _state.update { it.copy(pendingImportText = null) }

    fun confirmImport() {
        val text = _state.value.pendingImportText ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, pendingImportText = null, message = null) }
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
