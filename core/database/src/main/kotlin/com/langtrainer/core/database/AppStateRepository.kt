package com.langtrainer.core.database

import com.langtrainer.core.database.dao.AppStateDao
import com.langtrainer.core.database.entity.AppStateEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStateRepository @Inject constructor(
    private val dao: AppStateDao,
) {
    suspend fun ensureInitialized(nowEpochMs: Long): AppStateEntity {
        val existing = dao.get()
        if (existing != null && existing.appSeed.isNotEmpty()) return existing

        val seeded = AppStateEntity(
            id = AppStateEntity.SINGLETON_ID,
            appSeed = UUID.randomUUID().toString(),
            firstLaunchAtEpochMs = nowEpochMs,
        )
        dao.upsert(seeded)
        return seeded
    }

    suspend fun get(): AppStateEntity? = dao.get()
}
