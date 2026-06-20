package com.langtrainer.feature.kanjisrs.collection

import com.langtrainer.core.database.dao.KanjiDao
import com.langtrainer.core.database.dao.SentenceCardDao
import com.langtrainer.core.database.dao.SrsDao
import com.langtrainer.core.database.entity.CardKanjiLinkEntity
import com.langtrainer.core.database.entity.KanjiEntity
import com.langtrainer.core.database.entity.KanjiMasteryEntity
import com.langtrainer.core.database.entity.ReviewLogEntity
import com.langtrainer.core.model.KanjiUsage
import com.langtrainer.core.model.ReviewOutcome
import com.langtrainer.core.model.SrsConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class KanjiCollectionRepository @Inject constructor(
    private val kanjiDao: KanjiDao,
    private val cardDao: SentenceCardDao,
    private val srsDao: SrsDao,
    private val srsConfig: SrsConfig,
) {
    fun observeAll(): Flow<List<KanjiWithStatus>> = combine(
        kanjiDao.observeAll(),
        cardDao.observeAllLinks(),
        srsDao.observeAllMastery(),
        srsDao.observeAllReviewLogs(),
    ) { kanji, links, mastery, logs ->
        val now = System.currentTimeMillis()
        // Name-only (人名用) kanji live in their own deck and are excluded from the
        // main collection, matching their exclusion from the general session + KPI.
        kanji.asSequence()
            .filter { it.usage != KanjiUsage.JINMEI }
            .map { row ->
                row.toStatus(
                    links = links,
                    mastery = mastery,
                    logs = logs,
                    nowEpochMs = now,
                )
            }
            .toList()
    }

    private fun KanjiEntity.toStatus(
        links: List<CardKanjiLinkEntity>,
        mastery: List<KanjiMasteryEntity>,
        logs: List<ReviewLogEntity>,
        nowEpochMs: Long,
    ): KanjiWithStatus {
        val cardIds = links.asSequence()
            .filter { it.kanjiId == id }
            .map { it.cardId }
            .toSet()
        val logsForKanji = logs.filter { it.cardId in cardIds }
        val masteryRow = mastery.firstOrNull { it.kanjiId == id }
        val hasReviewed = logsForKanji.isNotEmpty()
        val hasRecentLapse = logsForKanji.any {
            it.outcome == ReviewOutcome.LAPSED &&
                it.shownAtEpochMs >= nowEpochMs - RECENT_LAPSE_WINDOW_MS
        }
        val qualifyingCount = masteryRow?.qualifyingCardCount ?: 0
        val status = when {
            masteryRow?.isMastered == true -> KanjiStatus.MASTERED
            qualifyingCount == srsConfig.masteryMinCards - 1 -> KanjiStatus.NEAR
            hasRecentLapse -> KanjiStatus.WOBBLING
            hasReviewed -> KanjiStatus.LEARNING
            else -> KanjiStatus.NEW
        }
        return KanjiWithStatus(
            kanji = this,
            status = status,
            qualifyingCardCount = qualifyingCount,
            totalCardCount = cardIds.size,
            recentMedianMs = logsForKanji.asSequence()
                .mapNotNull { it.responseMs }
                .take(RECENT_RESPONSE_LIMIT)
                .toList()
                .medianOrNull(),
        )
    }

    private fun List<Long>.medianOrNull(): Long? {
        if (isEmpty()) return null
        val sorted = sorted()
        return sorted[sorted.lastIndex / 2]
    }

    companion object {
        private const val RECENT_RESPONSE_LIMIT = 7
        private const val RECENT_LAPSE_WINDOW_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
