package com.langtrainer.feature.kanjisrs.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class KanjiCollectionUiState(
    val filter: KanjiCollectionFilter = KanjiCollectionFilter.ALL,
    val rows: List<KanjiWithStatus> = emptyList(),
    val counts: Map<KanjiCollectionFilter, Int> = emptyMap(),
)

@HiltViewModel
class KanjiCollectionViewModel @Inject constructor(
    repository: KanjiCollectionRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(KanjiCollectionFilter.ALL)

    val uiState: StateFlow<KanjiCollectionUiState> = combine(
        repository.observeAll(),
        filter,
    ) { allRows, selected ->
        val counts = buildMap {
            put(KanjiCollectionFilter.ALL, allRows.size)
            KanjiStatus.entries.forEach { status ->
                put(status.toFilter(), allRows.count { it.status == status })
            }
        }
        KanjiCollectionUiState(
            filter = selected,
            rows = allRows.filter { selected == KanjiCollectionFilter.ALL || it.status.name == selected.name },
            counts = counts,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = KanjiCollectionUiState(),
    )

    fun setFilter(next: KanjiCollectionFilter) {
        filter.value = next
    }

    private fun KanjiStatus.toFilter(): KanjiCollectionFilter =
        KanjiCollectionFilter.valueOf(name)
}
