package com.langtrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.langtrainer.core.database.entity.AppStateEntity

@Dao
interface AppStateDao {

    @Upsert
    suspend fun upsert(state: AppStateEntity)

    @Query("SELECT * FROM app_state WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = AppStateEntity.SINGLETON_ID): AppStateEntity?
}
