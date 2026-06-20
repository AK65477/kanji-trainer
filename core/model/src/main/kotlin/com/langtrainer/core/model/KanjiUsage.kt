package com.langtrainer.core.model

/**
 * How a kanji is used, which decides which study track it belongs to.
 *
 * [JOUYOU] — general-use (常用) kanji. The JLPT/reading-focused deck and the
 *   "Mastered 한자" KPI are built only from these.
 * [JINMEI] — kanji that appear almost exclusively in personal names (人名用).
 *   Studied in a separate "name" deck and fully excluded from the general
 *   session and the Mastered KPI so the JLPT-focused metric stays clean.
 */
enum class KanjiUsage {
    JOUYOU,
    JINMEI,
}
