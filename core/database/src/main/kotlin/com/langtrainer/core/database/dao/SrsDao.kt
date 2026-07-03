package com.langtrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.langtrainer.core.database.entity.KanjiMasteryEntity
import com.langtrainer.core.database.entity.ReviewLogEntity
import com.langtrainer.core.database.entity.SrsStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SrsDao {

    // --- SrsState ---

    @Upsert
    suspend fun upsertState(state: SrsStateEntity)

    @Query("SELECT * FROM srs_state WHERE card_id = :cardId")
    suspend fun getState(cardId: Long): SrsStateEntity?

    @Query("SELECT * FROM srs_state")
    fun observeAllStates(): Flow<List<SrsStateEntity>>

    // --- Backup / restore: bulk read + replace of learner progress ---

    @Query("SELECT * FROM srs_state")
    suspend fun getAllStatesList(): List<SrsStateEntity>

    @Query("DELETE FROM srs_state")
    suspend fun deleteAllStates()

    @Insert
    suspend fun insertStates(states: List<SrsStateEntity>)

    @Query("SELECT * FROM review_log")
    suspend fun getAllReviewLogsList(): List<ReviewLogEntity>

    @Query("DELETE FROM review_log")
    suspend fun deleteAllReviewLogs()

    @Insert
    suspend fun insertReviewLogs(logs: List<ReviewLogEntity>)

    @Query("SELECT * FROM kanji_mastery")
    suspend fun getAllMasteryList(): List<KanjiMasteryEntity>

    @Query("DELETE FROM kanji_mastery")
    suspend fun deleteAllMastery()

    @Insert
    suspend fun insertMasteryList(rows: List<KanjiMasteryEntity>)

    /**
     * Due **review** cards for the **general (JLPT) deck** — cards already seen at
     * least once (bucket != NEW) whose interval has elapsed. Excludes name-only
     * (JINMEI) cards so the name deck stays separate. Ordered most-overdue first.
     *
     * New cards are deliberately excluded here and fetched via [getNewCardsGeneral]
     * so the session composer can serve due reviews before introducing new cards;
     * see SrsSessionPlanner.
     */
    @Query(
        """
        SELECT * FROM srs_state
        WHERE next_due_at <= :nowEpochMs
          AND current_bucket != 'NEW'
          AND card_id NOT IN (
            SELECT l.card_id FROM card_kanji_link l
            JOIN kanji k ON k.id = l.kanji_id
            WHERE k.usage = 'JINMEI'
          )
        ORDER BY next_due_at ASC
        LIMIT :limit
        """,
    )
    suspend fun getDueReviewsGeneral(nowEpochMs: Long, limit: Int): List<SrsStateEntity>

    /**
     * Brand-new (never-reviewed) cards for the **general (JLPT) deck**, in
     * introduction order (card_id ascending = seed/curriculum order). Excludes
     * name-only (JINMEI) cards.
     */
    @Query(
        """
        SELECT * FROM srs_state
        WHERE current_bucket = 'NEW'
          AND card_id NOT IN (
            SELECT l.card_id FROM card_kanji_link l
            JOIN kanji k ON k.id = l.kanji_id
            WHERE k.usage = 'JINMEI'
          )
        ORDER BY card_id ASC
        LIMIT :limit
        """,
    )
    suspend fun getNewCardsGeneral(limit: Int): List<SrsStateEntity>

    /**
     * Due **review** cards for the **name-only (JINMEI) deck** — only cards linked
     * to a name-only kanji, already seen (bucket != NEW) and now due.
     */
    @Query(
        """
        SELECT * FROM srs_state
        WHERE next_due_at <= :nowEpochMs
          AND current_bucket != 'NEW'
          AND card_id IN (
            SELECT l.card_id FROM card_kanji_link l
            JOIN kanji k ON k.id = l.kanji_id
            WHERE k.usage = 'JINMEI'
          )
        ORDER BY next_due_at ASC
        LIMIT :limit
        """,
    )
    suspend fun getDueReviewsNameOnly(nowEpochMs: Long, limit: Int): List<SrsStateEntity>

    /**
     * Brand-new (never-reviewed) cards for the **name-only (JINMEI) deck**, in
     * introduction order.
     */
    @Query(
        """
        SELECT * FROM srs_state
        WHERE current_bucket = 'NEW'
          AND card_id IN (
            SELECT l.card_id FROM card_kanji_link l
            JOIN kanji k ON k.id = l.kanji_id
            WHERE k.usage = 'JINMEI'
          )
        ORDER BY card_id ASC
        LIMIT :limit
        """,
    )
    suspend fun getNewCardsNameOnly(limit: Int): List<SrsStateEntity>

    // General-deck counts: exclude cards linked to name-only (JINMEI) kanji so any
    // future "due/qualifying" stats stay JLPT-focused like the Mastered KPI.
    @Query(
        """
        SELECT COUNT(*) FROM srs_state
        WHERE next_due_at <= :nowEpochMs
          AND card_id NOT IN (
            SELECT l.card_id FROM card_kanji_link l
            JOIN kanji k ON k.id = l.kanji_id
            WHERE k.usage = 'JINMEI'
          )
        """,
    )
    suspend fun countDue(nowEpochMs: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM srs_state
        WHERE consecutive_mastered >= :threshold
          AND card_id NOT IN (
            SELECT l.card_id FROM card_kanji_link l
            JOIN kanji k ON k.id = l.kanji_id
            WHERE k.usage = 'JINMEI'
          )
        """,
    )
    suspend fun countQualifying(threshold: Int): Int

    // --- ReviewLog ---

    @Insert
    suspend fun insertReviewLog(log: ReviewLogEntity): Long

    @Query("SELECT * FROM review_log ORDER BY shown_at DESC")
    fun observeAllReviewLogs(): Flow<List<ReviewLogEntity>>

    /** Count of cards reviewed since [sinceEpochMs] (e.g. start of today). */
    @Query("SELECT COUNT(*) FROM review_log WHERE shown_at >= :sinceEpochMs")
    fun observeReviewCountSince(sinceEpochMs: Long): Flow<Int>

    // --- KanjiMastery ---

    @Upsert
    suspend fun upsertMastery(mastery: KanjiMasteryEntity)

    @Query("SELECT * FROM kanji_mastery WHERE kanji_id = :kanjiId")
    suspend fun getMastery(kanjiId: Long): KanjiMasteryEntity?

    @Query("SELECT * FROM kanji_mastery")
    fun observeAllMastery(): Flow<List<KanjiMasteryEntity>>

    // Mastered KPI is JLPT-focused: name-only (JINMEI) kanji are excluded via the
    // join even though their mastery rows are still recorded.
    @Query(
        """
        SELECT COUNT(*) FROM kanji_mastery m
        JOIN kanji k ON k.id = m.kanji_id
        WHERE m.is_mastered = 1 AND k.usage <> 'JINMEI'
        """,
    )
    suspend fun countMasteredKanji(): Int

    @Query(
        """
        SELECT COUNT(*) FROM kanji_mastery m
        JOIN kanji k ON k.id = m.kanji_id
        WHERE m.is_mastered = 1 AND k.usage <> 'JINMEI'
        """,
    )
    fun observeMasteredKanjiCount(): Flow<Int>

    /**
     * Apply the SRS-engine output atomically:
     *   1) write [stateEntity] (upsert)
     *   2) append a log row
     *   3) bump KanjiMastery rows by [kanjiDeltas]
     *
     * The is_mastered recompute uses [masteryMinCards].
     */
    @Transaction
    suspend fun applyReview(
        stateEntity: SrsStateEntity,
        log: ReviewLogEntity,
        kanjiDeltas: Map<Long, Int>,
        masteryMinCards: Int,
        nowEpochMs: Long,
    ) {
        upsertState(stateEntity)
        insertReviewLog(log)
        for ((kanjiId, delta) in kanjiDeltas) {
            val current = getMastery(kanjiId) ?: KanjiMasteryEntity(
                kanjiId = kanjiId,
                qualifyingCardCount = 0,
                isMastered = false,
                updatedAtEpochMs = nowEpochMs,
            )
            val nextCount = (current.qualifyingCardCount + delta).coerceAtLeast(0)
            upsertMastery(
                current.copy(
                    qualifyingCardCount = nextCount,
                    isMastered = nextCount >= masteryMinCards,
                    updatedAtEpochMs = nowEpochMs,
                ),
            )
        }
    }
}
