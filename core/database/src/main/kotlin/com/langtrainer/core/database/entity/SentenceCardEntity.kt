package com.langtrainer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.langtrainer.core.model.CardSource

@Entity(
    tableName = "sentence_card",
    indices = [
        Index(
            value = ["sentence_jp", "target_start", "target_end"],
            unique = true,
            name = "idx_sentence_card_span_unique",
        ),
    ],
)
data class SentenceCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sentence_jp") val sentenceJp: String,
    /** Span text shown highlighted in the sentence (e.g. "割と"). */
    @ColumnInfo(name = "target_text") val targetText: String,
    @ColumnInfo(name = "target_start") val targetStart: Int,
    @ColumnInfo(name = "target_end") val targetEnd: Int,
    /** Correct reading in hiragana (e.g. "わりと"). */
    @ColumnInfo(name = "correct_reading") val correctReading: String,
    /** JSON-encoded 3 incorrect readings. */
    @ColumnInfo(name = "distractors_json") val distractorsJson: String,
    @ColumnInfo(name = "translation_en") val translationEn: String?,
    @ColumnInfo(name = "translation_ko") val translationKo: String?,
    @ColumnInfo(name = "source") val source: CardSource,
)
