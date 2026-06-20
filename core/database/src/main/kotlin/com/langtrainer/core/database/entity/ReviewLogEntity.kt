package com.langtrainer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.langtrainer.core.model.ReviewOutcome

@Entity(
    tableName = "review_log",
    foreignKeys = [
        ForeignKey(
            entity = SentenceCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("card_id"),
        Index("shown_at"),
    ],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "card_id") val cardId: Long,
    @ColumnInfo(name = "shown_at") val shownAtEpochMs: Long,
    @ColumnInfo(name = "outcome") val outcome: ReviewOutcome,
    @ColumnInfo(name = "response_ms") val responseMs: Long?,
    @ColumnInfo(name = "is_correct") val isCorrect: Boolean?,
)
