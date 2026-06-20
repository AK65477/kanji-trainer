package com.langtrainer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "card_kanji_link",
    primaryKeys = ["card_id", "kanji_id"],
    foreignKeys = [
        ForeignKey(
            entity = SentenceCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = KanjiEntity::class,
            parentColumns = ["id"],
            childColumns = ["kanji_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("kanji_id")],
)
data class CardKanjiLinkEntity(
    @ColumnInfo(name = "card_id") val cardId: Long,
    @ColumnInfo(name = "kanji_id") val kanjiId: Long,
)
