package com.langtrainer.core.database.di

import android.content.Context
import androidx.room.Room
import com.langtrainer.core.database.ALL_MIGRATIONS
import com.langtrainer.core.database.AppDatabase
import com.langtrainer.core.database.FirstCreateCallback
import com.langtrainer.core.database.dao.AppStateDao
import com.langtrainer.core.database.dao.KanjiDao
import com.langtrainer.core.database.dao.SentenceCardDao
import com.langtrainer.core.database.dao.SessionDao
import com.langtrainer.core.database.dao.SrsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addCallback(FirstCreateCallback())
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides fun provideKanjiDao(db: AppDatabase): KanjiDao = db.kanjiDao()
    @Provides fun provideSentenceCardDao(db: AppDatabase): SentenceCardDao = db.sentenceCardDao()
    @Provides fun provideSrsDao(db: AppDatabase): SrsDao = db.srsDao()
    @Provides fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()
    @Provides fun provideAppStateDao(db: AppDatabase): AppStateDao = db.appStateDao()
}
