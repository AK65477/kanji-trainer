package com.langtrainer.settings

import com.langtrainer.core.model.Bucket
import com.langtrainer.core.model.ReviewOutcome
import com.langtrainer.core.model.SessionCategory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Portable, storage-agnostic backup format for one learner's progress, so it can
 * be moved between the user's own devices (export to file → transfer → import).
 *
 * Only *progress* is carried — SRS scheduling state, kanji mastery, review history
 * and session history. The seed cards themselves live in the APK and are never
 * exported. Progress rows reference `card_id`/`kanji_id`, which are assigned
 * deterministically by seed import order, so a backup is only safe to apply on a
 * device whose seed matches. [ProgressBackupCodec.validate] enforces that with a
 * seed-card-count + schema-version guard before any destructive import.
 *
 * This file is pure Kotlin (no Android / Room types) so it is unit-testable.
 */
@Serializable
data class SrsStateDto(
    val cardId: Long,
    val bucket: Bucket,
    val consecutiveMastered: Int,
    val nextDueAtEpochMs: Long,
    val intervalDays: Int,
    val easeFactor: Double,
)

@Serializable
data class KanjiMasteryDto(
    val kanjiId: Long,
    val qualifyingCardCount: Int,
    val isMastered: Boolean,
    val updatedAtEpochMs: Long,
)

@Serializable
data class ReviewLogDto(
    val id: Long,
    val cardId: Long,
    val shownAtEpochMs: Long,
    val outcome: ReviewOutcome,
    val responseMs: Long?,
    val isCorrect: Boolean?,
)

@Serializable
data class SessionDto(
    val id: Long,
    val category: SessionCategory,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val turnsCompleted: Int?,
)

@Serializable
data class ProgressBackup(
    val format: String = FORMAT,
    val formatVersion: Int = FORMAT_VERSION,
    val appVersionCode: Long,
    val dbSchemaVersion: Int,
    val seedCardCount: Int,
    val exportedAtEpochMs: Long,
    val srsStates: List<SrsStateDto>,
    val kanjiMastery: List<KanjiMasteryDto>,
    val reviewLogs: List<ReviewLogDto>,
    val sessions: List<SessionDto>,
) {
    companion object {
        const val FORMAT = "kanji-trainer-progress"
        const val FORMAT_VERSION = 1
    }
}

sealed interface BackupValidation {
    data object Ok : BackupValidation
    data class Rejected(val reason: String) : BackupValidation
}

object ProgressBackupCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(backup: ProgressBackup): String =
        json.encodeToString(ProgressBackup.serializer(), backup)

    fun decode(text: String): ProgressBackup =
        json.decodeFromString(ProgressBackup.serializer(), text)

    /**
     * Structural + compatibility check run **before** a destructive import. Rejects
     * anything that would corrupt local data (wrong format, newer format version,
     * different DB schema, or a different seed — the card-count guard).
     */
    fun validate(
        backup: ProgressBackup,
        localSeedCardCount: Int,
        localSchemaVersion: Int,
    ): BackupValidation = when {
        backup.format != ProgressBackup.FORMAT ->
            BackupValidation.Rejected("올바른 진도 백업 파일이 아닙니다.")
        backup.formatVersion > ProgressBackup.FORMAT_VERSION ->
            BackupValidation.Rejected("더 최신 버전에서 만든 백업입니다. 앱을 먼저 업데이트하세요.")
        backup.dbSchemaVersion != localSchemaVersion ->
            BackupValidation.Rejected("데이터베이스 버전이 달라 가져올 수 없습니다 (${backup.dbSchemaVersion} ≠ $localSchemaVersion).")
        backup.seedCardCount != localSeedCardCount ->
            BackupValidation.Rejected("카드 세트가 다릅니다 (${backup.seedCardCount} ≠ $localSeedCardCount). 같은 버전의 앱에서 내보낸 백업만 가져올 수 있습니다.")
        else -> BackupValidation.Ok
    }

    /** Decode + validate in one step; never throws (parse errors become Rejected). */
    fun decodeAndValidate(
        text: String,
        localSeedCardCount: Int,
        localSchemaVersion: Int,
    ): Pair<ProgressBackup?, BackupValidation> = try {
        val backup = decode(text)
        backup to validate(backup, localSeedCardCount, localSchemaVersion)
    } catch (e: Exception) {
        null to BackupValidation.Rejected("파일을 읽을 수 없습니다: ${e.message}")
    }
}
