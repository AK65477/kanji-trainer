package com.langtrainer.core.seedimporter.di

import com.langtrainer.core.seedimporter.AssetSeedSource
import com.langtrainer.core.seedimporter.SeedSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SeedImporterModule {
    @Binds
    @Singleton
    abstract fun bindSeedSource(impl: AssetSeedSource): SeedSource
}
