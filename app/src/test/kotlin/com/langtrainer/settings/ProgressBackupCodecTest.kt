package com.langtrainer.settings

import com.langtrainer.core.model.Bucket
import com.langtrainer.core.model.ReviewOutcome
import com.langtrainer.core.model.SessionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressBackupCodecTest {

    private fun sample(seedCardCount: Int = 4493, schema: Int = 1) = ProgressBackup(
        appVersionCode = 2,
        dbSchemaVersion = schema,
        seedCardCount = seedCardCount,
        exportedAtEpochMs = 1_700_000_000_000L,
        srsStates = listOf(
            SrsStateDto(1L, Bucket.MASTERED, 3, 1_700_100_000_000L, 8, 2.5),
            SrsStateDto(2L, Bucket.LAPSED, 0, 1_700_050_000_000L, 1, 2.35),
        ),
        kanjiMastery = listOf(KanjiMasteryDto(10L, 2, true, 1_700_000_500_000L)),
        reviewLogs = listOf(
            ReviewLogDto(1L, 1L, 1_700_000_100_000L, ReviewOutcome.MASTERED, 1200L, true),
            ReviewLogDto(2L, 2L, 1_700_000_200_000L, ReviewOutcome.LAPSED, null, false),
        ),
        sessions = listOf(SessionDto(1L, SessionCategory.KANJI_SRS, 1_700_000_000_000L, 1_700_000_900_000L, 20)),
    )

    @Test
    fun `encode then decode round-trips exactly`() {
        val original = sample()
        val decoded = ProgressBackupCodec.decode(ProgressBackupCodec.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun `validate accepts matching seed count and schema`() {
        val result = ProgressBackupCodec.validate(sample(seedCardCount = 4493), localSeedCardCount = 4493, localSchemaVersion = 1)
        assertTrue(result is BackupValidation.Ok)
    }

    @Test
    fun `validate rejects a different seed card count`() {
        val result = ProgressBackupCodec.validate(sample(seedCardCount = 4400), localSeedCardCount = 4493, localSchemaVersion = 1)
        assertTrue(result is BackupValidation.Rejected)
    }

    @Test
    fun `validate rejects a different db schema version`() {
        val result = ProgressBackupCodec.validate(sample(schema = 2), localSeedCardCount = 4493, localSchemaVersion = 1)
        assertTrue(result is BackupValidation.Rejected)
    }

    @Test
    fun `validate rejects a newer format version`() {
        val newer = sample().copy(formatVersion = ProgressBackup.FORMAT_VERSION + 1)
        val result = ProgressBackupCodec.validate(newer, localSeedCardCount = 4493, localSchemaVersion = 1)
        assertTrue(result is BackupValidation.Rejected)
    }

    @Test
    fun `validate rejects a wrong format tag`() {
        val bad = sample().copy(format = "something-else")
        val result = ProgressBackupCodec.validate(bad, localSeedCardCount = 4493, localSchemaVersion = 1)
        assertTrue(result is BackupValidation.Rejected)
    }

    @Test
    fun `decodeAndValidate turns garbage into a Rejected without throwing`() {
        val (backup, validation) = ProgressBackupCodec.decodeAndValidate("not json at all", 4493, 1)
        assertNull(backup)
        assertTrue(validation is BackupValidation.Rejected)
    }
}
