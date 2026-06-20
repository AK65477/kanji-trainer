package com.langtrainer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "kanji_mastery",
    foreignKeys = [
        ForeignKey(
            entity = KanjiEntity::class,
            parentColumns = ["id"],
            childColumns = ["kanji_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("is_mastered")],
)
data class KanjiMasteryEntity(
    @PrimaryKey @ColumnInfo(name = "kanji_id") val kanjiId: Long,
    @ColumnInfo(name = "qualifying_card_count") val qualifyingCardCount: Int,
    @ColumnInfo(name = "is_mastered") val isMastered: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMs: Long,
)
