package com.langtrainer.feature.kanjisrs.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.langtrainer.core.database.entity.KanjiEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KanjiDetailUiState(
    val kanji: KanjiEntity? = null,
    val status: KanjiWithStatus? = null,
    val readings: KanjiReadings = KanjiReadings(),
    val components: List<String> = emptyList(),
    val mnemonic: String = "",
    val cards: List<SentenceCardWithSrs> = emptyList(),
)

@HiltViewModel
class KanjiDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: KanjiDetailRepository,
) : ViewModel() {
    private val kanjiId: Long = checkNotNull(savedStateHandle.get<String>("kanjiId")).toLong()
    private val mnemonicDraft = MutableStateFlow<String?>(null)
    private var saveJob: Job? = null

    val uiState: StateFlow<KanjiDetailUiState> = combine(
        repository.observeKanji(kanjiId),
        repository.observeStatus(kanjiId),
        repository.observeCardsForKanji(kanjiId),
        mnemonicDraft,
    ) { kanji, status, cards, draft ->
        KanjiDetailUiState(
            kanji = kanji,
            status = status,
            readings = kanji?.let { repository.decodeReadings(it.readingsJson) } ?: KanjiReadings(),
            components = repository.decodeComponents(kanji?.componentsJson),
            mnemonic = draft ?: kanji?.mnemonic.orEmpty(),
            cards = cards,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = KanjiDetailUiState(),
    )

    fun saveMnemonic(text: String) {
        mnemonicDraft.value = text
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            repository.updateMnemonic(kanjiId, text)
        }
    }

    fun flushMnemonic() {
        val text = mnemonicDraft.value ?: return
        saveJob?.cancel()
        viewModelScope.launch {
            repository.updateMnemonic(kanjiId, text)
        }
    }

    companion object {
        private const val SAVE_DEBOUNCE_MS = 300L
    }
}
