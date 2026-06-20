package com.langtrainer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.langtrainer.core.model.SessionCategory

@Entity(
    tableName = "session",
    indices = [
        Index("category"),
        Index("started_at"),
    ],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category") val category: SessionCategory,
    @ColumnInfo(name = "started_at") val startedAtEpochMs: Long,
    @ColumnInfo(name = "ended_at") val endedAtEpochMs: Long?,
    @ColumnInfo(name = "turns_completed") val turnsCompleted: Int?,
)
