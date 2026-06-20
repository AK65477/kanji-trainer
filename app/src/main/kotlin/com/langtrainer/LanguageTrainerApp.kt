package com.langtrainer

import android.app.Application
import android.util.Log
import com.langtrainer.core.database.AppStateRepository
import com.langtrainer.core.seedimporter.SeedImporter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class LanguageTrainerApp : Application() {

    @Inject lateinit var appStateRepository: AppStateRepository
    @Inject lateinit var seedImporter: SeedImporter

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            val now = System.currentTimeMillis()
            appStateRepository.ensureInitialized(nowEpochMs = now)
            val summary = runCatching { seedImporter.importIfEmpty(nowEpochMs = now) }
                .onFailure { Log.e(TAG, "Seed import failed", it) }
                .getOrNull()
            if (summary != null && summary.kanjiInserted > 0) {
                Log.i(
                    TAG,
                    "Seeded ${summary.kanjiInserted} kanji and ${summary.cardsInserted} cards; " +
                        "removed ${summary.legacyCardsDeleted} legacy scaffold cards.",
                )
            }
        }
    }

    companion object {
        private const val TAG = "LanguageTrainerApp"
    }
}
