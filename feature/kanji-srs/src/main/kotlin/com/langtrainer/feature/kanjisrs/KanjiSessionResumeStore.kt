package com.langtrainer.feature.kanjisrs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class KanjiSessionResumeStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(deck: Deck): ResumeState? {
        val ids = prefs.getString(key(deck, KEY_CARD_IDS), null)
            ?.split(',')
            ?.mapNotNull { raw -> raw.toLongOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val nextIndex = prefs.getInt(key(deck, KEY_NEXT_INDEX), 0)
        return ResumeState(cardIds = ids, nextIndex = nextIndex.coerceAtLeast(0))
    }

    fun save(deck: Deck, cardIds: List<Long>, nextIndex: Int) {
        if (cardIds.isEmpty() || nextIndex >= cardIds.size) {
            clear(deck)
            return
        }
        prefs.edit()
            .putString(key(deck, KEY_CARD_IDS), cardIds.joinToString(","))
            .putInt(key(deck, KEY_NEXT_INDEX), nextIndex.coerceAtLeast(0))
            .apply()
    }

    fun clear(deck: Deck) {
        prefs.edit()
            .remove(key(deck, KEY_CARD_IDS))
            .remove(key(deck, KEY_NEXT_INDEX))
            .apply()
    }

    data class ResumeState(
        val cardIds: List<Long>,
        val nextIndex: Int,
    )

    private fun key(deck: Deck, suffix: String): String = "${deck.name}_$suffix"

    private companion object {
        const val PREFS_NAME = "kanji_srs_resume"
        const val KEY_CARD_IDS = "card_ids"
        const val KEY_NEXT_INDEX = "next_index"
    }
}
