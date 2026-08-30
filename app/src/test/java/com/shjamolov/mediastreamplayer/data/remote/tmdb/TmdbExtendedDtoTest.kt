package com.shjamolov.mediastreamplayer.data.remote.tmdb

import com.shjamolov.mediastreamplayer.data.remote.tmdb.dto.TmdbDetailsDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbExtendedDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun details_decodesAppendedResponses() {
        val json = """
            {
              "id": 10,
              "title": "Film",
              "runtime": 125,
              "videos": {"results":[{"name":"Trailer","key":"abc","site":"YouTube","type":"Trailer","official":true}]},
              "credits": {"cast":[{"id":1,"name":"Actor","character":"Hero","order":0}]},
              "external_ids": {"imdb_id":"tt0010"},
              "watch/providers": {"results":{"UZ":{"flatrate":[{"provider_name":"Provider"}]}}}
            }
        """.trimIndent()
        val details = this.json.decodeFromString<TmdbDetailsDto>(json)
        assertEquals(125, details.runtime)
        assertEquals("abc", details.videos.results.single().key)
        assertEquals("Actor", details.credits.cast.single().name)
        assertEquals("tt0010", details.externalIds.imdbId)
        assertEquals("Provider", details.watchProviders.results.getValue("UZ").flatrate.single().name)
    }
}
