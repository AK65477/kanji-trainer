package com.langtrainer.feature.kanjisrs.collection

import com.langtrainer.core.database.entity.KanjiEntity
import com.langtrainer.core.database.entity.SentenceCardEntity
import com.langtrainer.core.model.Bucket

data class KanjiWithStatus(
    val kanji: KanjiEntity,
    val status: KanjiStatus,
    val qualifyingCardCount: Int,
    val totalCardCount: Int,
    val recentMedianMs: Long?,
)

enum class KanjiStatus {
    MASTERED,
    NEAR,
    WOBBLING,
    LEARNING,
    NEW,
}

enum class KanjiCollectionFilter {
    ALL,
    MASTERED,
    NEAR,
    WOBBLING,
    LEARNING,
    NEW,
}

data class SentenceCardWithSrs(
    val card: SentenceCardEntity,
    val bucket: Bucket?,
    val recentResponseMs: Long?,
)

data class KanjiReadings(
    val on: List<String> = emptyList(),
    val kun: List<String> = emptyList(),
)
