package com.maxrave.kotlinytmusicscraper.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicPlaylistShelfRenderer(
    val playlistId: String? = null,
    val contents: List<MusicShelfRenderer.Content>? = null,
    val collapsedItemCount: Int = 0,
    val continuations: List<Continuation>? = null,
)