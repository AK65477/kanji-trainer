package com.langtrainer.core.seedimporter

import androidx.room.withTransaction
import com.langtrainer.core.database.AppDatabase
import com.langtrainer.core.database.dao.KanjiDao
import com.langtrainer.core.database.dao.SentenceCardDao
import com.langtrainer.core.database.dao.SrsDao
import com.langtrainer.core.database.entity.KanjiEntity
import com.langtrainer.core.database.entity.SentenceCardEntity
import com.langtrainer.core.database.entity.SrsStateEntity
import com.langtrainer.core.model.Bucket
import com.langtrainer.core.model.SrsConfig
import javax.inject.Inject
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Idempotent, atomic import for the bundled kanji seed.
 *
 * Existing rows keep their ids, so SRS progress and KanjiMastery survive seed
 * top-ups. New cards get an initial SRS state and become due immediately.
 */
class SeedImporter @Inject constructor(
    private val source: SeedSource,
    private val db: AppDatabase,
    private val kanjiDao: KanjiDao,
    private val cardDao: SentenceCardDao,
    private val srsDao: SrsDao,
) {

    suspend fun importIfEmpty(nowEpochMs: Long): ImportSummary {
        val data = source.load()
        require(data.version == SUPPORTED_VERSION) {
            "Seed JSON version ${data.version} not supported (expected $SUPPORTED_VERSION)"
        }

        topUpSeedManagedFields(data)

        return db.withTransaction {
            val legacyCardsDeleted = cardDao.deleteLegacyCompletionScaffoldCards()

            val kanjiRows = data.kanji.map {
                KanjiEntity(
                    char = it.char,
                    jlptLevel = it.jlpt,
                    readingsJson = it.readingsJson,
                    componentsJson = it.components?.let { c -> encodeStringList(c) },
                    keywordKo = it.keywordKo,
                    usage = it.usage,
                )
            }
            val kanjiInserted = kanjiDao.insertAll(kanjiRows).count { it > 0L }

            val charToId: Map<String, Long> = data.kanji.associate { seed ->
                seed.char to (kanjiDao.getByChar(seed.char)?.id
                    ?: error("Kanji '${seed.char}' missing after insert"))
            }

            var cardsInserted = 0
            for (seed in data.cards) {
                val cardEntity = SentenceCardEntity(
                    sentenceJp = seed.sentenceJp,
                    targetText = seed.targetText,
                    targetStart = seed.targetStart,
                    targetEnd = seed.targetEnd,
                    correctReading = seed.correctReading,
                    distractorsJson = encodeStringList(seed.distractors),
                    translationEn = seed.translationEn,
                    translationKo = seed.translationKo,
                    source = seed.source,
                )
                val linkedIds = seed.linkedKanjiChars.mapNotNull { charToId[it] }.toSet()
                val newCardId = cardDao.insertCardWithLinks(cardEntity, linkedIds)
                if (newCardId > 0L) {
                    srsDao.upsertState(
                        SrsStateEntity(
                            cardId = newCardId,
                            currentBucket = Bucket.NEW,
                            consecutiveMastered = 0,
                            nextDueAtEpochMs = nowEpochMs,
                            intervalDays = 0,
                            easeFactor = SrsConfig.INITIAL_EASE_FACTOR,
                        ),
                    )
                    cardsInserted++
                }
            }

            ImportSummary(
                kanjiInserted = kanjiInserted,
                cardsInserted = cardsInserted,
                legacyCardsDeleted = legacyCardsDeleted,
            )
        }
    }

    private fun encodeStringList(list: List<String>): String =
        Json.encodeToString(ListSerializer(String.serializer()), list)

    private suspend fun topUpSeedManagedFields(data: SeedDataJson) {
        for (seed in data.kanji) {
            val existing = kanjiDao.getByChar(seed.char) ?: continue
            if (existing.usage != seed.usage) {
                kanjiDao.updateUsage(id = existing.id, usage = seed.usage)
            }
            if (existing.componentsJson != null && existing.keywordKo != null) continue
            if (seed.components == null && seed.keywordKo == null) continue
            kanjiDao.topUpDecomposition(
                id = existing.id,
                componentsJson = seed.components?.let { encodeStringList(it) },
                keywordKo = seed.keywordKo,
            )
        }

        // Back-fill Korean translations onto cards that were imported before the
        // seed carried translationKo. Gated by a count so it is a single query
        // once every card already has its translation.
        if (cardDao.countMissingTranslationKo() > 0) {
            db.withTransaction {
                for (seed in data.cards) {
                    val ko = seed.translationKo ?: continue
                    cardDao.topUpTranslationKo(
                        sentenceJp = seed.sentenceJp,
                        targetStart = seed.targetStart,
                        targetEnd = seed.targetEnd,
                        translationKo = ko,
                    )
                }
            }
        }
    }

    companion object {
        const val SUPPORTED_VERSION = 1
    }
}

data class ImportSummary(
    val kanjiInserted: Int,
    val cardsInserted: Int,
    val legacyCardsDeleted: Int = 0,
)
