package com.langtrainer.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val masteredKanji: Int = 0,
    val todayReviews: Int = 0,
    val todayMinutes: Int = 0,
    val jpTurns: Int = 0,
    val jpTargetTurns: Int = 24,
    val enTurns: Int = 0,
    val enTargetTurns: Int = 36,
    val totalStudyDays: Int = 0,
    val streakDays: Int = 0,
    
    val latestJpNoticing: String? = null,
    
    val latestEnNoticing: String? = null,
) {
    val jpProgressText: String = "$jpTurns / $jpTargetTurns"
    val enProgressText: String = "$enTurns / $enTargetTurns"
    val hasAnyNoticing: Boolean = latestJpNoticing != null || latestEnNoticing != null
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    homeRepository: HomeRepository,
) : ViewModel() {

    private val todayStartEpochMs = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val uiState: StateFlow<HomeUiState> = homeRepository.observeHome(todayStartEpochMs).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}

