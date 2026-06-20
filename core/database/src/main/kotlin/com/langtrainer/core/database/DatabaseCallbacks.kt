package com.langtrainer.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

internal class FirstCreateCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT OR IGNORE INTO app_state (id, app_seed, first_launch_at)
            VALUES (0, '', 0)
            """.trimIndent(),
        )
    }
}
