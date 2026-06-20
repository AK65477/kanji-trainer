package com.langtrainer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.langtrainer.core.database.converter.Converters
import com.langtrainer.core.database.dao.AppStateDao
import com.langtrainer.core.database.dao.KanjiDao
import com.langtrainer.core.database.dao.SentenceCardDao
import com.langtrainer.core.database.dao.SessionDao
import com.langtrainer.core.database.dao.SrsDao
import com.langtrainer.core.database.entity.AppStateEntity
import com.langtrainer.core.database.entity.CardKanjiLinkEntity
import com.langtrainer.core.database.entity.KanjiEntity
import com.langtrainer.core.database.entity.KanjiMasteryEntity
import com.langtrainer.core.database.entity.ReviewLogEntity
import com.langtrainer.core.database.entity.SentenceCardEntity
import com.langtrainer.core.database.entity.SessionEntity
import com.langtrainer.core.database.entity.SrsStateEntity

@Database(
    entities = [
        KanjiEntity::class,
        SentenceCardEntity::class,
        CardKanjiLinkEntity::class,
        ReviewLogEntity::class,
        SrsStateEntity::class,
        KanjiMasteryEntity::class,
        SessionEntity::class,
        AppStateEntity::class,
    ],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun kanjiDao(): KanjiDao
    abstract fun sentenceCardDao(): SentenceCardDao
    abstract fun srsDao(): SrsDao
    abstract fun sessionDao(): SessionDao
    abstract fun appStateDao(): AppStateDao

    companion object {
        const val NAME = "kanji_trainer.db"
        const val VERSION = 1
    }
}
