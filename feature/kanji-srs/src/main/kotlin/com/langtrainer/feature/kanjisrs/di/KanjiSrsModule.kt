package com.langtrainer.feature.kanjisrs.di

import com.langtrainer.core.model.SrsConfig
import com.langtrainer.core.srs.SrsEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KanjiSrsModule {

    @Provides
    @Singleton
    fun provideSrsConfig(): SrsConfig = SrsConfig.Default

    @Provides
    @Singleton
    fun provideSrsEngine(config: SrsConfig): SrsEngine = SrsEngine(config)
}
