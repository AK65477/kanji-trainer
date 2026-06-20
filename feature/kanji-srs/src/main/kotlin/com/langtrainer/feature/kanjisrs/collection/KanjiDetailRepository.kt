package com.langtrainer.feature.kanjisrs.collection

import com.langtrainer.core.database.dao.KanjiDao
import com.langtrainer.core.database.dao.SentenceCardDao
import com.langtrainer.core.database.dao.SrsDao
import com.langtrainer.core.database.entity.KanjiEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KanjiDetailRepository @Inject constructor(
    private val kanjiDao: KanjiDao,
    private val cardDao: SentenceCardDao,
    private val srsDao: SrsDao,
    private val collectionRepository: KanjiCollectionRepository,
) {
    fun observeKanji(id: Long): Flow<KanjiEntity?> = kanjiDao.observeById(id)

    fun observeStatus(id: Long): Flow<KanjiWithStatus?> =
        collectionRepository.observeAll().map { rows -> rows.firstOrNull { it.kanji.id == id } }

    fun observeCardsForKanji(id: Long): Flow<List<SentenceCardWithSrs>> = combine(
        cardDao.observeCardsForKanji(id),
        srsDao.observeAllStates(),
        srsDao.observeAllReviewLogs(),
    ) { cards, states, logs ->
        val statesByCard = states.associateBy { it.cardId }
        val logsByCard = logs.groupBy { it.cardId }
        cards.map { card ->
            SentenceCardWithSrs(
                card = card,
                bucket = statesByCard[card.id]?.currentBucket,
                recentResponseMs = logsByCard[card.id]
                    .orEmpty()
                    .asSequence()
                    .mapNotNull { it.responseMs }
                    .take(RECENT_RESPONSE_LIMIT)
                    .toList()
                    .medianOrNull(),
            )
        }
    }

    suspend fun updateMnemonic(id: Long, text: String) {
        kanjiDao.updateMnemonic(id, text.trim().ifBlank { null })
    }

    fun decodeComponents(value: String?): List<String> = runCatching {
        if (value.isNullOrBlank()) emptyList() else json.decodeFromString(
            ListSerializer(String.serializer()),
            value,
        )
    }.getOrDefault(emptyList())

    fun decodeReadings(value: String): KanjiReadings = runCatching {
        val root = json.parseToJsonElement(value).jsonObject
        KanjiReadings(
            on = root["on"].asStringList(),
            kun = root["kun"].asStringList(),
        )
    }.getOrDefault(KanjiReadings())

    private fun kotlinx.serialization.json.JsonElement?.asStringList(): List<String> =
        this?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.content.takeIf(String::isNotBlank) }
            .orEmpty()

    private fun List<Long>.medianOrNull(): Long? {
        if (isEmpty()) return null
        val sorted = sorted()
        return sorted[sorted.lastIndex / 2]
    }

    companion object {
        private const val RECENT_RESPONSE_LIMIT = 5
        private val json = Json { ignoreUnknownKeys = true }
    }
}
