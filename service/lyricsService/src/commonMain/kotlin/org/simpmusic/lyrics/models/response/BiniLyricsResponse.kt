package org.simpmusic.lyrics.models.response

import kotlinx.serialization.Serializable

@Serializable
data class BiniLyricsResponse(
    val results: List<BiniResult>? = null
)

@Serializable
data class BiniResult(
    val timing_type: String? = null,
    val lyricsUrl: String? = null
)