package com.maxrave.kotlinytmusicscraper.parser

import com.maxrave.kotlinytmusicscraper.models.Button
import com.maxrave.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.SectionListRenderer
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.models.getContinuation
import com.maxrave.kotlinytmusicscraper.models.response.BrowseResponse
import com.maxrave.kotlinytmusicscraper.models.response.LikeStatus
import com.maxrave.logger.Logger

private fun SectionListRenderer?.getPlaylistShelfContents(): List<MusicShelfRenderer.Content>? =
    this?.contents?.mapNotNull { it.musicPlaylistShelfRenderer?.contents ?: it.musicShelfRenderer?.contents }?.firstOrNull()

fun BrowseResponse.fromPlaylistToTrack(): List<SongItem> =
    (
        this.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer.getPlaylistShelfContents()
            ?: this.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer.getPlaylistShelfContents()
            ?: this.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer.getPlaylistShelfContents()
            ?: this.contents?.sectionListRenderer.getPlaylistShelfContents()
    )?.mapNotNull { content ->
        content.toSongItem()
    } ?: emptyList()

fun BrowseResponse.fromPlaylistToTrackWithSetVideoId(): List<Pair<SongItem, String>> =
    (
        this.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer.getPlaylistShelfContents()
            ?: this.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer.getPlaylistShelfContents()
            ?: this.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer.getPlaylistShelfContents()
            ?: this.contents?.sectionListRenderer.getPlaylistShelfContents()
    )?.mapNotNull { content ->
        Pair(
            content.toSongItem() ?: return@mapNotNull null,
            content.toPlaylistItemData()?.playlistSetVideoId ?: return@mapNotNull null,
        )
    } ?: emptyList()

fun BrowseResponse.fromPlaylistContinuationToTracks(): List<SongItem> =
    (
        this.continuationContents
            ?.musicPlaylistShelfContinuation
            ?.contents
            ?: this.continuationContents
                ?.sectionListContinuation
                ?.contents
                ?.mapNotNull { it.musicShelfRenderer?.contents ?: it.musicPlaylistShelfRenderer?.contents }
                ?.firstOrNull()
    )?.mapNotNull { contents ->
        contents.toSongItem()
    } ?: emptyList()

fun BrowseResponse.fromPlaylistContinuationToTrackWithSetVideoId(): List<Pair<SongItem, String>> =
    (
        this.continuationContents
            ?.musicPlaylistShelfContinuation
            ?.contents
            ?: this.continuationContents
                ?.sectionListContinuation
                ?.contents
                ?.mapNotNull { it.musicShelfRenderer?.contents ?: it.musicPlaylistShelfRenderer?.contents }
                ?.firstOrNull()
    )?.mapNotNull { contents ->
        Pair(
            contents.toSongItem() ?: return@mapNotNull null,
            contents.toPlaylistItemData()?.playlistSetVideoId ?: return@mapNotNull null,
        )
    } ?: emptyList()

fun BrowseResponse.getPlaylistContinuation(): String? =
    this.onResponseReceivedActions
        ?.firstOrNull()
        ?.appendContinuationItemsAction
        ?.continuationItems
        ?.lastOrNull()
        ?.continuationItemRenderer
        ?.continuationEndpoint
        ?.continuationCommand
        ?.token
        ?: this.contents
            ?.twoColumnBrowseResultsRenderer
            ?.secondaryContents
            ?.sectionListRenderer
            ?.contents
            ?.lastOrNull()
            ?.musicPlaylistShelfRenderer
            ?.contents
            ?.lastOrNull()
            ?.continuationItemRenderer
            ?.continuationEndpoint
            ?.continuationCommand
            ?.token
        ?: this.contents
            ?.singleColumnBrowseResultsRenderer
            ?.tabs
            ?.firstOrNull()
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
            ?.continuations
            ?.getContinuation()
        ?: this.contents
            ?.twoColumnBrowseResultsRenderer
            ?.secondaryContents
            ?.sectionListRenderer
            ?.continuations
            ?.getContinuation()
        ?: this.contents
            ?.singleColumnBrowseResultsRenderer
            ?.tabs
            ?.firstOrNull()
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.musicPlaylistShelfRenderer
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
        ?: this.contents
            ?.twoColumnBrowseResultsRenderer
            ?.secondaryContents
            ?.sectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.musicPlaylistShelfRenderer
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
        ?: this.contents
            ?.twoColumnBrowseResultsRenderer
            ?.secondaryContents
            ?.sectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.musicPlaylistShelfRenderer
            ?.contents
            ?.lastOrNull()
            ?.continuationItemRenderer
            ?.continuationEndpoint
            ?.continuationCommand
            ?.token
        ?: this.continuationContents
            ?.musicPlaylistShelfContinuation
            ?.continuations
            ?.getContinuation()

fun BrowseResponse.getContinuePlaylistContinuation(): String? =
    this.continuationContents
        ?.musicPlaylistShelfContinuation
        ?.continuations
        ?.getContinuation()

private fun BrowseResponse.findMusicResponsiveHeaderRenderer(): SectionListRenderer.Content.MusicResponsiveHeaderRenderer? =
    this.header?.musicResponsiveHeaderRenderer
        ?: this.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer
        ?: this.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull {
            it.musicResponsiveHeaderRenderer ?: it.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer
        }?.firstOrNull()
        ?: this.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull {
            it.musicResponsiveHeaderRenderer ?: it.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer
        }?.firstOrNull()

fun BrowseResponse.getPlaylistRadioEndpoint(): WatchEndpoint? {
    val header =
        this.header?.musicDetailHeaderRenderer
            ?: this.header
                ?.musicEditablePlaylistDetailHeaderRenderer
                ?.header
                ?.musicDetailHeaderRenderer
    if (header != null) {
        return header.menu?.menuRenderer?.items
            ?.find {
                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
            }?.menuNavigationItemRenderer
            ?.navigationEndpoint
            ?.watchPlaylistEndpoint
    } else {
        val responsiveHeader = findMusicResponsiveHeaderRenderer()
        return responsiveHeader?.buttons
            ?.mapNotNull { it.menuRenderer }
            ?.flatMap { it.items.orEmpty() }
            ?.find {
                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
            }?.menuNavigationItemRenderer
            ?.navigationEndpoint
            ?.watchPlaylistEndpoint
    }
}

fun BrowseResponse.getPlaylistShuffleEndpoint(): WatchEndpoint? {
    val header =
        this.header?.musicDetailHeaderRenderer
            ?: this.header
                ?.musicEditablePlaylistDetailHeaderRenderer
                ?.header
                ?.musicDetailHeaderRenderer
    if (header != null) {
        return header.menu?.menuRenderer?.topLevelButtons
            ?.firstOrNull()
            ?.buttonRenderer
            ?.navigationEndpoint
            ?.watchPlaylistEndpoint
    } else {
        val responsiveHeader = findMusicResponsiveHeaderRenderer()
        return responsiveHeader?.buttons
            ?.mapNotNull { it.menuRenderer }
            ?.flatMap { it.items.orEmpty() }
            ?.find {
                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
            }?.menuNavigationItemRenderer
            ?.navigationEndpoint
            ?.watchPlaylistEndpoint
            ?: responsiveHeader?.buttons
                ?.mapNotNull { it.buttonRenderer }
                ?.find { it.icon?.iconType == "MUSIC_SHUFFLE" }
                ?.navigationEndpoint
                ?.watchPlaylistEndpoint
    }
}

fun MusicShelfRenderer.Content.toPlaylistItemData(): MusicResponsiveListItemRenderer.PlaylistItemData? =
    this.musicResponsiveListItemRenderer?.playlistItemData

fun MusicShelfRenderer.Content.toSongItem(): SongItem? {
    val flexColumns = this.musicResponsiveListItemRenderer?.flexColumns
    val fixedColumns = this.musicResponsiveListItemRenderer?.fixedColumns
    val menu = this.musicResponsiveListItemRenderer?.menu
    return SongItem(
        id =
            flexColumns
                ?.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.firstOrNull()
                ?.navigationEndpoint
                ?.watchEndpoint
                ?.videoId ?: return null,
        title =
            flexColumns
                .firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.firstOrNull()
                ?.text ?: return null,
        artists =
            flexColumns
                .apply {
                    Logger.w(
                        "PlaylistParser",
                        "Artists: ${this.map {
                            it.musicResponsiveListItemFlexColumnRenderer.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                        }}",
                    )
                }.filter {
                    it.musicResponsiveListItemFlexColumnRenderer.isArtist()
                }.apply {
                    Logger.w(
                        "PlaylistParser",
                        "Artists after filter: ${this.map {
                            it.musicResponsiveListItemFlexColumnRenderer.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                        }}",
                    )
                }.mapNotNull { it.musicResponsiveListItemFlexColumnRenderer.toArtist() },
        album =
            flexColumns
                .find {
                    it.musicResponsiveListItemFlexColumnRenderer.isAlbum()
                }?.musicResponsiveListItemFlexColumnRenderer
                ?.toAlbum(),
        duration =
            fixedColumns
                ?.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.firstOrNull()
                ?.text
                .toDurationSeconds(),
        thumbnail =
            this.musicResponsiveListItemRenderer
                ?.thumbnail
                ?.musicThumbnailRenderer
                ?.getThumbnailUrl() ?: "",
        endpoint =
            flexColumns
                ?.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.firstOrNull()
                ?.navigationEndpoint
                ?.watchEndpoint,
        explicit =
            this.musicResponsiveListItemRenderer?.badges?.toSongBadges()?.contains(
                SongItem.SongBadges.Explicit,
            ) ?: false,
        thumbnails =
            this.musicResponsiveListItemRenderer
                ?.thumbnail
                ?.musicThumbnailRenderer
                ?.thumbnail,
        likeStatus =
            menu
                ?.menuRenderer
                ?.topLevelButtons
                ?.firstOrNull()
                ?.likeButtonRenderer
                ?.toLikeStatus()
                ?: LikeStatus.INDIFFERENT,
        badges = this.musicResponsiveListItemRenderer?.badges?.toSongBadges(),
        musicVideoType = this.musicResponsiveListItemRenderer?.musicVideoType,
    )
}

/**
 * Check if the browse response has reload params, if true, this has the suggestion tracks
 */
fun BrowseResponse.hasReloadParams(): Boolean =
    this.continuationContents
        ?.sectionListContinuation
        ?.contents
        ?.firstOrNull()
        ?.musicShelfRenderer
        ?.continuations
        ?.firstOrNull()
        ?.reloadContinuationData
        ?.continuation != null

fun BrowseResponse.getReloadParams(): String? =
    this.continuationContents
        ?.sectionListContinuation
        ?.contents
        ?.firstOrNull()
        ?.musicShelfRenderer
        ?.continuations
        ?.firstOrNull()
        ?.reloadContinuationData
        ?.continuation

fun BrowseResponse.getSuggestionSongItems(): List<SongItem> =
    if (hasReloadParams()) {
        this.continuationContents
            ?.sectionListContinuation
            ?.contents
            ?.firstOrNull()
            ?.musicShelfRenderer
            ?.contents
            ?.mapNotNull { content ->
                content.toSongItem()
            } ?: emptyList()
    } else {
        emptyList()
    }