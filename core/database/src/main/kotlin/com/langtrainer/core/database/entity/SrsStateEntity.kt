package com.langtrainer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.langtrainer.core.model.Bucket

@Entity(
    tableName = "srs_state",
    foreignKeys = [
        ForeignKey(
            entity = SentenceCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("next_due_at")],
)
data class SrsStateEntity(
    @PrimaryKey @ColumnInfo(name = "card_id") val cardId: Long,
    @ColumnInfo(name = "current_bucket") val currentBucket: Bucket,
    @ColumnInfo(name = "consecutive_mastered") val consecutiveMastered: Int,
    @ColumnInfo(name = "next_due_at") val nextDueAtEpochMs: Long,
    @ColumnInfo(name = "interval_days") val intervalDays: Int,
    @ColumnInfo(name = "ease_factor") val easeFactor: Double,
)
