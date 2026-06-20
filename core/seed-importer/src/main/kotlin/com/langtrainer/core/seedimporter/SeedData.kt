package com.langtrainer.core.seedimporter

import com.langtrainer.core.model.CardSource
import com.langtrainer.core.model.JlptLevel
import com.langtrainer.core.model.KanjiUsage
import kotlinx.serialization.Serializable

/**
 * Wire format for bundled seed JSON. Versioned at the top level so future seed
 * format changes can be detected and refused with a clear error rather than
 * silently mis-importing.
 */
@Serializable
data class SeedDataJson(
    val version: Int,
    val kanji: List<KanjiSeed>,
    val cards: List<CardSeed>,
)

@Serializable
data class KanjiSeed(
    val char: String,
    val jlpt: JlptLevel,
    /** Raw JSON string for readings. Forwarded as-is into KanjiEntity.readings_json. */
    val readingsJson: String,
    /** Visual components or radicals, in the order they appear. */
    val components: List<String>? = null,
    /** Short Korean keyword. */
    val keywordKo: String? = null,
    /** Study track. Omitted entries default to JOUYOU. Name-only kanji use JINMEI. */
    val usage: KanjiUsage = KanjiUsage.JOUYOU,
)

@Serializable
data class CardSeed(
    val sentenceJp: String,
    val targetText: String,
    val targetStart: Int,
    val targetEnd: Int,
    val correctReading: String,
    val distractors: List<String>,
    val translationEn: String? = null,
    val translationKo: String? = null,
    val source: CardSource = CardSource.USER_ADD,
    /** Chars from the target span that link to Kanji rows. Resolved at import time. */
    val linkedKanjiChars: List<String>,
)
