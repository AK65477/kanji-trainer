package com.langtrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.langtrainer.core.database.entity.KanjiEntity
import com.langtrainer.core.model.KanjiUsage
import kotlinx.coroutines.flow.Flow

@Dao
interface KanjiDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<KanjiEntity>): List<Long>

    @Query("SELECT * FROM kanji WHERE id = :id")
    suspend fun getById(id: Long): KanjiEntity?

    @Query("SELECT * FROM kanji WHERE id = :id")
    fun observeById(id: Long): Flow<KanjiEntity?>

    @Query("SELECT * FROM kanji WHERE `char` = :char LIMIT 1")
    suspend fun getByChar(char: String): KanjiEntity?

    @Query("SELECT COUNT(*) FROM kanji")
    suspend fun count(): Int

    @Query("SELECT * FROM kanji ORDER BY jlpt_level, id")
    fun observeAll(): Flow<List<KanjiEntity>>

    /**
     * Fill in v2 decomposition/keyword fields for existing rows. Used by
     * [com.langtrainer.core.seedimporter.SeedImporter] to top up legacy v1 rows
     * after migration. Never touches [KanjiEntity.mnemonic] — that's user data.
     *
     * Only writes when the existing column is NULL so user-curated edits via the
     * detail screen are never clobbered.
     */
    @Query(
        """
        UPDATE kanji
        SET components_json = COALESCE(components_json, :componentsJson),
            keyword_ko = COALESCE(keyword_ko, :keywordKo)
        WHERE id = :id
        """,
    )
    suspend fun topUpDecomposition(id: Long, componentsJson: String?, keywordKo: String?)

    @Query("UPDATE kanji SET mnemonic = :mnemonic WHERE id = :id")
    suspend fun updateMnemonic(id: Long, mnemonic: String?)

    /**
     * Reconcile an existing row's study track from the seed. `usage` is seed-owned
     * (not user data like [KanjiEntity.mnemonic]), so re-classifying a kanji in the
     * seed JSON should propagate to already-imported rows on the next boot.
     */
    @Query("UPDATE kanji SET usage = :usage WHERE id = :id")
    suspend fun updateUsage(id: Long, usage: KanjiUsage)
}
