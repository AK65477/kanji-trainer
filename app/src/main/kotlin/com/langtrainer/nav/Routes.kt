package com.langtrainer.nav

object Routes {
    const val HOME = "home"
    const val KANJI_SRS = "kanji_srs"
    const val KANJI_COLLECTION = "kanji_collection"
    const val KANJI_DETAIL_PREFIX = "kanji_detail"
    const val SOURCES = "sources"

    /** Nav arg name for the SRS deck (GENERAL | NAME). */
    const val ARG_DECK = "deck"

    fun kanjiDetail(kanjiId: Long): String = "$KANJI_DETAIL_PREFIX/$kanjiId"

    /** Kanji SRS route for a given deck name. Default GENERAL keeps the bare route working. */
    fun kanjiSrs(deck: String = "GENERAL"): String = "$KANJI_SRS?$ARG_DECK=$deck"
}
