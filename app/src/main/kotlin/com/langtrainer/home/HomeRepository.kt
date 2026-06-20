package com.langtrainer.home

import com.langtrainer.core.database.dao.SessionDao
import com.langtrainer.core.database.dao.SrsDao
import com.langtrainer.core.database.entity.SessionEntity
import com.langtrainer.core.model.SessionCategory
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class HomeRepository @Inject constructor(
    private val srsDao: SrsDao,
    private val sessionDao: SessionDao,
) {

    fun observeHome(todayStartEpochMs: Long): Flow<HomeUiState> {
        return combine(
            srsDao.observeMasteredKanjiCount(),
            sessionDao.observeSince(todayStartEpochMs),
            sessionDao.observeCompleted(),
            srsDao.observeReviewCountSince(todayStartEpochMs),
        ) { masteredKanji, todaySessions, allCompleted, todayReviews ->
            val completedToday = todaySessions.filter {
                it.endedAtEpochMs != null && it.category == SessionCategory.KANJI_SRS
            }
            val studyDays = allCompleted
                .filter { it.category == SessionCategory.KANJI_SRS }
                .map { it.startedLocalDate() }
                .toSet()
            HomeUiState(
                masteredKanji = masteredKanji,
                // Cards reviewed today (review_log based) — a meaningful effort
                // metric that, unlike a raw session count, is not inflated by
                // entering and leaving a session repeatedly.
                todayReviews = todayReviews,
                todayMinutes = completedToday.sumOf { it.durationMinutes() },
                totalStudyDays = studyDays.size,
                streakDays = studyDays.streakUntil(LocalDate.now()),
            )
        }
    }
}

private fun SessionEntity.durationMinutes(): Int {
    val endedAt = endedAtEpochMs ?: return 0
    val millis = (endedAt - startedAtEpochMs).coerceAtLeast(0)
    return (millis / 60_000L).toInt()
}

private fun SessionEntity.startedLocalDate(): LocalDate =
    java.time.Instant.ofEpochMilli(startedAtEpochMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

private fun Set<LocalDate>.streakUntil(today: LocalDate): Int {
    var streak = 0
    var cursor = today
    while (contains(cursor)) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
