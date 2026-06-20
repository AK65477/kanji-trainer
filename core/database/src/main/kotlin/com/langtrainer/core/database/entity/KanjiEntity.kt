package com.langtrainer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.langtrainer.core.model.JlptLevel
import com.langtrainer.core.model.KanjiUsage

@Entity(
    tableName = "kanji",
    indices = [Index(value = ["char"], unique = true)],
)
data class KanjiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "char") val char: String,
    @ColumnInfo(name = "jlpt_level") val jlptLevel: JlptLevel,
    /** JSON-serialized readings: { "on": [...], "kun": [...] }. Kept as raw string to avoid
     *  premature schema lock-in for readings shape. */
    @ColumnInfo(name = "readings_json") val readingsJson: String,
    /** v2: JSON array of visual components (e.g. ["害","刀"] for 割). Nullable so legacy
     *  rows survive migration; SeedImporter fills missing values from the seed JSON. */
    @ColumnInfo(name = "components_json") val componentsJson: String? = null,
    /** v2: Short Korean keyword used in the detail/collection screens. Nullable. */
    @ColumnInfo(name = "keyword_ko") val keywordKo: String? = null,
    /** v2: User-owned mnemonic memo. Never overwritten by seed top-up. Nullable. */
    @ColumnInfo(name = "mnemonic") val mnemonic: String? = null,
    /** v3: study track. JOUYOU = general/JLPT deck + Mastered KPI; JINMEI = name-only
     *  deck, fully excluded from the general session and the Mastered KPI. NOT NULL
     *  with a JOUYOU default so all pre-v3 rows migrate cleanly. */
    @ColumnInfo(name = "usage", defaultValue = "'JOUYOU'") val usage: KanjiUsage = KanjiUsage.JOUYOU,
)
