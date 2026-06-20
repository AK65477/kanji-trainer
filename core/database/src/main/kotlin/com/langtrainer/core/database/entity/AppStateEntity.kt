package com.langtrainer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "app_seed") val appSeed: String,
    @ColumnInfo(name = "first_launch_at") val firstLaunchAtEpochMs: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
