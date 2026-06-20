package com.langtrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.langtrainer.core.database.entity.CardKanjiLinkEntity
import com.langtrainer.core.database.entity.SentenceCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceCardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCard(card: SentenceCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(links: List<CardKanjiLinkEntity>)

    @Transaction
    suspend fun insertCardWithLinks(
        card: SentenceCardEntity,
        kanjiIds: Set<Long>,
    ): Long {
        val newId = insertCard(card)
        if (newId > 0L && kanjiIds.isNotEmpty()) {
            insertLinks(kanjiIds.map { CardKanjiLinkEntity(cardId = newId, kanjiId = it) })
        }
        return newId
    }

    @Query("SELECT * FROM sentence_card WHERE id = :id")
    suspend fun getById(id: Long): SentenceCardEntity?

    @Query(
        """
        SELECT sentence_card.* FROM sentence_card
        INNER JOIN card_kanji_link ON sentence_card.id = card_kanji_link.card_id
        WHERE card_kanji_link.kanji_id = :kanjiId
        ORDER BY sentence_card.id
        """,
    )
    fun observeCardsForKanji(kanjiId: Long): Flow<List<SentenceCardEntity>>

    @Query("SELECT kanji_id FROM card_kanji_link WHERE card_id = :cardId")
    suspend fun getLinkedKanjiIds(cardId: Long): List<Long>

    @Query("SELECT * FROM card_kanji_link")
    fun observeAllLinks(): Flow<List<CardKanjiLinkEntity>>

    @Query("SELECT COUNT(*) FROM sentence_card")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM sentence_card WHERE translation_ko IS NULL")
    suspend fun countMissingTranslationKo(): Int

    /**
     * Back-fills the Korean translation onto an already-imported card, matched
     * by the unique span key. Only touches rows still missing it, so it is a
     * cheap no-op once populated and never overwrites existing values.
     */
    @Query(
        """
        UPDATE sentence_card
        SET translation_ko = :translationKo
        WHERE sentence_jp = :sentenceJp
            AND target_start = :targetStart
            AND target_end = :targetEnd
            AND translation_ko IS NULL
        """,
    )
    suspend fun topUpTranslationKo(
        sentenceJp: String,
        targetStart: Int,
        targetEnd: Int,
        translationKo: String,
    ): Int

    @Query(
        """
        DELETE FROM sentence_card
        WHERE
            (
                sentence_jp = '「' || target_text || '」の読みを確認する。'
                AND translation_en = 'Check the reading of ' || target_text || '.'
            )
            OR
            (
                sentence_jp = target_text || 'を文の中で読む。'
                AND translation_en = 'Read ' || target_text || ' in a sentence.'
            )
        """,
    )
    suspend fun deleteLegacyCompletionScaffoldCards(): Int
}
