package com.langtrainer.core.seedimporter

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.serialization.json.Json

/**
 * Pluggable seed loader. Production wires [AssetSeedSource]; tests can pass an
 * in-memory implementation.
 */
fun interface SeedSource {
    suspend fun load(): SeedDataJson
}

/**
 * Reads `assets/seed/kanji_seed.json` once on demand.
 *
 * The file is expected to be present in the :app module's assets — the importer is
 * a library module without its own assets directory.
 */
class AssetSeedSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : SeedSource {
    override suspend fun load(): SeedDataJson {
        val raw = context.assets.open(SEED_PATH).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return json.decodeFromString(SeedDataJson.serializer(), raw)
    }

    companion object {
        const val SEED_PATH = "seed/kanji_seed.json"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
