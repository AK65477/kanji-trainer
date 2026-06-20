package com.langtrainer.core.database.converter

import androidx.room.TypeConverter
import com.langtrainer.core.model.Bucket
import com.langtrainer.core.model.CardSource
import com.langtrainer.core.model.JlptLevel
import com.langtrainer.core.model.KanjiUsage
import com.langtrainer.core.model.ReviewOutcome
import com.langtrainer.core.model.SessionCategory
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter fun fromBucket(v: Bucket): String = v.name
    @TypeConverter fun toBucket(v: String): Bucket = Bucket.valueOf(v)

    @TypeConverter fun fromReviewOutcome(v: ReviewOutcome): String = v.name
    @TypeConverter fun toReviewOutcome(v: String): ReviewOutcome = ReviewOutcome.valueOf(v)

    @TypeConverter fun fromSessionCategory(v: SessionCategory): String = v.name
    @TypeConverter fun toSessionCategory(v: String): SessionCategory = SessionCategory.valueOf(v)

    @TypeConverter fun fromJlpt(v: JlptLevel): String = v.name
    @TypeConverter fun toJlpt(v: String): JlptLevel = JlptLevel.valueOf(v)

    @TypeConverter fun fromKanjiUsage(v: KanjiUsage): String = v.name
    @TypeConverter fun toKanjiUsage(v: String): KanjiUsage = KanjiUsage.valueOf(v)

    @TypeConverter fun fromCardSource(v: CardSource): String = v.name
    @TypeConverter fun toCardSource(v: String): CardSource = CardSource.valueOf(v)

    @TypeConverter
    fun fromStringList(v: List<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), v)

    @TypeConverter
    fun toStringList(v: String): List<String> =
        json.decodeFromString(ListSerializer(String.serializer()), v)

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }
    }
}
