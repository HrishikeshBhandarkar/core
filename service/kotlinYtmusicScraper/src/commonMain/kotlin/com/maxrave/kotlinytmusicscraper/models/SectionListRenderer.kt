package com.maxrave.kotlinytmusicscraper.models

import com.maxrave.kotlinytmusicscraper.models.response.BrowseResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class SectionListRenderer(
    val header: Header? = null,
    val contents: List<Content>? = null,
    val continuations: List<Continuation>? = null,
) {
    @Serializable
    data class Header(
        val chipCloudRenderer: ChipCloudRenderer? = null,
    ) {
        @Serializable
        data class ChipCloudRenderer(
            val chips: List<Chip>? = null,
        ) {
            @Serializable
            data class Chip(
                val chipCloudChipRenderer: ChipCloudChipRenderer,
            ) {
                @Serializable
                data class ChipCloudChipRenderer(
                    val isSelected: Boolean? = null,
                    val navigationEndpoint: NavigationEndpoint,
                    // The close button doesn't have the following two fields
                    val text: Runs? = null,
                    val uniqueId: String? = null,
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Content(
        @JsonNames("musicImmersiveCarouselShelfRenderer")
        val musicCarouselShelfRenderer: MusicCarouselShelfRenderer? = null,
        val musicShelfRenderer: MusicShelfRenderer? = null,
        val musicCardShelfRenderer: MusicCardShelfRenderer? = null,
        val musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer? = null,
        val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer? = null,
        val musicResponsiveHeaderRenderer: MusicResponsiveHeaderRenderer? = null,
        val musicEditablePlaylistDetailHeaderRenderer: BrowseResponse.Header.MusicEditablePlaylistDetailHeaderRenderer? = null,
        val gridRenderer: GridRenderer? = null,
    ) {
        @Serializable
        data class MusicResponsiveHeaderRenderer(
            val description: Description? = null,
            val straplineTextOne: StraplineTextOne? = null,
            val straplineThumbnail: StraplineThumbnail? = null,
            val subtitle: MusicShelfRenderer.Content.MusicMultiRowListItemRenderer.Subtitle? = null,
            val thumbnail: ThumbnailRenderer? = null,
            val title: Title? = null,
            val secondSubtitle: MusicShelfRenderer.Content.MusicMultiRowListItemRenderer.Subtitle? = null,
            val facepile: Facepile? = null,
            val buttons: List<Button>? = null,
        ) {
            @Serializable
            data class Facepile(
                val avatarStackViewModel: AvatarStackViewModel?,
            ) {
                @Serializable
                data class AvatarStackViewModel(
                    val avatars: List<Avatar>?,
                    val rendererContext: RendererContext?,
                    val text: Text?,
                ) {
                    @Serializable
                    data class Text(
                        val content: String?,
                    )

                    @Serializable
                    data class RendererContext(
                        val commandContext: CommandContext?,
                    ) {
                        @Serializable
                        data class CommandContext(
                            val onTap: OnTap?,
                        ) {
                            @Serializable
                            data class OnTap(
                                val innertubeCommand: InnertubeCommand?,
                            ) {
                                @Serializable
                                data class InnertubeCommand(
                                    val browseEndpoint: BrowseEndpoint?,
                                )
                            }
                        }
                    }

                    @Serializable
                    data class Avatar(
                        val avatarViewModel: AvatarViewModel?,
                    ) {
                        @Serializable
                        data class AvatarViewModel(
                            val image: Image?,
                        ) {
                            @Serializable
                            data class Image(
                                val sources: List<Source>?,
                            ) {
                                @Serializable
                                data class Source(
                                    val url: String?,
                                )
                            }
                        }
                    }
                }
            }

            @Serializable
            data class Description(
                val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer?,
            )

            @Serializable
            data class StraplineTextOne(
                val runs: List<Run>?,
            )

            @Serializable
            data class StraplineThumbnail(
                val musicThumbnailRenderer: ThumbnailRenderer.MusicThumbnailRenderer?,
            )

            @Serializable
            data class Title(
                val runs: List<Run>?,
            )
        }
    }
}