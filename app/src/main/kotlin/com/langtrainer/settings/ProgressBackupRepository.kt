package com.langtrainer.settings

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.langtrainer.core.database.AppDatabase
import com.langtrainer.core.database.dao.SentenceCardDao
import com.langtrainer.core.database.dao.SessionDao
import com.langtrainer.core.database.dao.SrsDao
import com.langtrainer.core.database.entity.KanjiMasteryEntity
import com.langtrainer.core.database.entity.ReviewLogEntity
import com.langtrainer.core.database.entity.SessionEntity
import com.langtrainer.core.database.entity.SrsStateEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads/writes the learner-progress tables for cross-device backup. Export is
 * read-only; import is destructive (replace) but gated by [ProgressBackupCodec]
 * validation and wrapped in a single transaction so a bad file cannot leave the
 * DB half-written.
 */
class ProgressBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val srsDao: SrsDao,
    private val sessionDao: SessionDao,
    private val cardDao: SentenceCardDao,
) {

    suspend fun exportJson(): String {
        val backup = ProgressBackup(
            appVersionCode = appVersionCode(),
            dbSchemaVersion = AppDatabase.VERSION,
            seedCardCount = cardDao.countCards(),
            exportedAtEpochMs = System.currentTimeMillis(),
            srsStates = srsDao.getAllStatesList().map {
                SrsStateDto(
                    it.cardId, it.currentBucket, it.consecutiveMastered,
                    it.nextDueAtEpochMs, it.intervalDays, it.easeFactor,
                )
            },
            kanjiMastery = srsDao.getAllMasteryList().map {
                KanjiMasteryDto(it.kanjiId, it.qualifyingCardCount, it.isMastered, it.updatedAtEpochMs)
            },
            reviewLogs = srsDao.getAllReviewLogsList().map {
                ReviewLogDto(it.id, it.cardId, it.shownAtEpochMs, it.outcome, it.responseMs, it.isCorrect)
            },
            sessions = sessionDao.getAllSessions().map {
                SessionDto(it.id, it.category, it.startedAtEpochMs, it.endedAtEpochMs, it.turnsCompleted)
            },
        )
        return ProgressBackupCodec.encode(backup)
    }

    sealed interface ImportResult {
        data class Success(
            val srsStates: Int,
            val kanjiMastery: Int,
            val reviewLogs: Int,
            val sessions: Int,
        ) : ImportResult

        data class Failure(val reason: String) : ImportResult
    }

    /**
     * Validates then, only if valid, atomically replaces every progress table with
     * the backup's rows. Existing progress is overwritten — callers must confirm
     * with the user first.
     */
    suspend fun importJson(text: String): ImportResult {
        val (backup, validation) = ProgressBackupCodec.decodeAndValidate(
            text = text,
            localSeedCardCount = cardDao.countCards(),
            localSchemaVersion = AppDatabase.VERSION,
        )
        if (backup == null || validation is BackupValidation.Rejected) {
            return ImportResult.Failure(
                (validation as? BackupValidation.Rejected)?.reason ?: "알 수 없는 오류",
            )
        }
        db.withTransaction {
            srsDao.deleteAllStates()
            srsDao.deleteAllMastery()
            srsDao.deleteAllReviewLogs()
            sessionDao.deleteAllSessions()

            srsDao.insertStates(
                backup.srsStates.map {
                    SrsStateEntity(
                        it.cardId, it.bucket, it.consecutiveMastered,
                        it.nextDueAtEpochMs, it.intervalDays, it.easeFactor,
                    )
                },
            )
            srsDao.insertMasteryList(
                backup.kanjiMastery.map {
                    KanjiMasteryEntity(it.kanjiId, it.qualifyingCardCount, it.isMastered, it.updatedAtEpochMs)
                },
            )
            srsDao.insertReviewLogs(
                backup.reviewLogs.map {
                    ReviewLogEntity(it.id, it.cardId, it.shownAtEpochMs, it.outcome, it.responseMs, it.isCorrect)
                },
            )
            sessionDao.insertSessions(
                backup.sessions.map {
                    SessionEntity(it.id, it.category, it.startedAtEpochMs, it.endedAtEpochMs, it.turnsCompleted)
                },
            )
        }
        return ImportResult.Success(
            srsStates = backup.srsStates.size,
            kanjiMastery = backup.kanjiMastery.size,
            reviewLogs = backup.reviewLogs.size,
            sessions = backup.sessions.size,
        )
    }

    /** Export current progress and write it to a user-picked document [uri]. */
    suspend fun exportToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = exportJson()
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                ?: return@withContext false
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Read the raw text of a user-picked document [uri] (for import). */
    suspend fun readText(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: Exception) {
            null
        }
    }

    private fun appVersionCode(): Long = try {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode
        else @Suppress("DEPRECATION") pi.versionCode.toLong()
    } catch (e: PackageManager.NameNotFoundException) {
        -1L
    }
}
