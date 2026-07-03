package com.langtrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.langtrainer.core.database.entity.SessionEntity
import com.langtrainer.core.model.SessionCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("SELECT * FROM session WHERE id = :id")
    suspend fun getById(id: Long): SessionEntity?

    // --- Backup / restore ---

    @Query("SELECT * FROM session")
    suspend fun getAllSessions(): List<SessionEntity>

    @Query("DELETE FROM session")
    suspend fun deleteAllSessions()

    @Insert
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Query(
        """
        SELECT * FROM session
        WHERE category = :category AND ended_at IS NULL
        ORDER BY started_at DESC LIMIT 1
        """,
    )
    suspend fun getOpenSession(category: SessionCategory): SessionEntity?

    @Query(
        """
        SELECT * FROM session
        WHERE ended_at IS NULL
        ORDER BY started_at DESC LIMIT 1
        """,
    )
    suspend fun getOpenSession(): SessionEntity?

    @Query(
        """
        SELECT * FROM session
        WHERE started_at >= :sinceEpochMs
        ORDER BY started_at DESC
        """,
    )
    fun observeSince(sinceEpochMs: Long): Flow<List<SessionEntity>>

    @Query(
        """
        SELECT * FROM session
        WHERE ended_at IS NOT NULL
        ORDER BY started_at DESC
        """,
    )
    fun observeCompleted(): Flow<List<SessionEntity>>

    @Query(
        """
        SELECT COALESCE(AVG(turns_completed), 0)
        FROM session
        WHERE category = :category
          AND turns_completed IS NOT NULL
          AND started_at >= :sinceEpochMs
        """,
    )
    suspend fun averageTurns(category: SessionCategory, sinceEpochMs: Long): Double
}
