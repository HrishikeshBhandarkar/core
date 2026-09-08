package org.simpmusic.lyrics

import com.maxrave.ktorext.crypto.Hmac
import com.maxrave.ktorext.crypto.HmacUri
import com.maxrave.logger.Logger
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import org.simpmusic.lyrics.am.AMArtistResource
import org.simpmusic.lyrics.am.AMSearchResponse
import org.simpmusic.lyrics.am.AppleMusicArtworkResult
import org.simpmusic.lyrics.domain.Lyrics
import org.simpmusic.lyrics.models.request.LyricsBody
import org.simpmusic.lyrics.models.request.TranslatedLyricsBody
import org.simpmusic.lyrics.models.response.BaseResponse
import org.simpmusic.lyrics.models.response.BetterLyricsResponse
import org.simpmusic.lyrics.models.response.LrclibObject
import org.simpmusic.lyrics.models.response.LyricsResponse
import org.simpmusic.lyrics.models.response.TranslatedLyricsResponse
import org.simpmusic.lyrics.parser.parseSyncedLyrics
import org.simpmusic.lyrics.parser.parseUnsyncedLyrics
import org.simpmusic.lyrics.qq.QQMusicDes
import org.simpmusic.lyrics.qq.QQMusicParser
import org.simpmusic.lyrics.qq.QQPlainLyricResponse
import org.simpmusic.lyrics.qq.QQSearchResponse
import org.simpmusic.lyrics.qq.ZlibInflater
import kotlin.math.abs

private const val TAG = "SimpMusicLyricsClient"

class SimpMusicLyricsClient {
    private val algorithm = ""

    private val hmacService = Hmac("HmacSHA256", "simpmusic-lyrics")
    private val lyricsService = SimpMusicLyrics()
    private var insertingLyrics: Pair<String?, Boolean> = (null to false)
    private val isInsertingLyrics: Boolean
        get() = insertingLyrics.second

    private var tooManyRequest: Boolean = false

    private var insertingTranslatedLyrics: Pair<String?, Boolean> = (null to false)
    private val isInsertingTranslatedLyrics: Boolean
        get() = insertingTranslatedLyrics.second

    suspend fun getLyrics(videoId: String): Result<List<LyricsResponse>> =
        runCatching {
            lyricsService.findLyricsByVideoId(videoId).bodyOrThrow<List<LyricsResponse>>()
        }

    suspend fun getTranslatedLyrics(
        videoId: String,
        language: String,
    ): Result<TranslatedLyricsResponse> =
        runCatching {
            if (language.length != 2) {
                throw IllegalArgumentException("Language code must be a 2-letter code")
            }
            lyricsService.findTranslatedLyrics(videoId, language).bodyOrThrow<TranslatedLyricsResponse>()
        }

    suspend fun insertLyrics(lyricsBody: LyricsBody): Result<LyricsResponse> =
        runCatching {
            if (tooManyRequest) {
                throw IllegalStateException("Too many requests, please wait before trying again.")
            }
            if (isInsertingLyrics && insertingLyrics.first == lyricsBody.videoId) {
                throw IllegalStateException("Already inserting lyrics, please wait until the current operation is complete.")
            }
            insertingLyrics = lyricsBody.videoId to true
            val hmacTimestamp =
                hmacService.getMacTimestampPair(
                    HmacUri.BASE_HMAC_URI,
                )
            lyricsService.insertLyrics(lyricsBody, hmacTimestamp).bodyOrThrow<LyricsResponse>()
        }

    suspend fun insertTranslatedLyrics(translatedLyricsBody: TranslatedLyricsBody): Result<TranslatedLyricsResponse> =
        runCatching {
            if (translatedLyricsBody.language.length != 2) {
                throw IllegalArgumentException("Language code must be a 2-letter code")
            }
            if (isInsertingTranslatedLyrics && insertingTranslatedLyrics.first == translatedLyricsBody.videoId) {
                throw IllegalStateException("Already inserting translated lyrics, please wait until the current operation is complete.")
            }
            insertingTranslatedLyrics = translatedLyricsBody.videoId to true
            val hmacTimestamp =
                hmacService.getMacTimestampPair(
                    HmacUri.TRANSLATED_HMAC_URI,
                )
            lyricsService.insertTranslatedLyrics(translatedLyricsBody, hmacTimestamp).bodyOrThrow<TranslatedLyricsResponse>()
        }

    suspend fun voteLyrics(
        lyricsId: String,
        upvote: Boolean,
    ): Result<LyricsResponse> =
        runCatching {
            val hmacTimestamp =
                hmacService.getMacTimestampPair(
                    HmacUri.VOTE_HMAC_URI,
                )
            lyricsService.voteLyrics(lyricsId, upvote, hmacTimestamp).bodyOrThrow<LyricsResponse>()
        }

    suspend fun voteTranslatedLyrics(
        translatedLyricsId: String,
        upvote: Boolean,
    ): Result<TranslatedLyricsResponse> =
        runCatching {
            val hmacTimestamp =
                hmacService.getMacTimestampPair(
                    HmacUri.VOTE_TRANSLATED_HMAC_URI,
                )
            lyricsService.voteTranslatedLyrics(translatedLyricsId, upvote, hmacTimestamp).bodyOrThrow<TranslatedLyricsResponse>()
        }

    suspend fun searchLrclibLyrics(
        q_track: String,
        q_artist: String,
        duration: Int?,
    ) = runCatching {
        val rs =
            lyricsService
                .searchLrclibLyrics(
                    q_track = q_track,
                    q_artist = q_artist,
                ).body<List<LrclibObject>>()
        val lrclibObject: LrclibObject? =
            if (duration != null) {
                rs.find { abs(it.duration.toInt() - duration) <= 10 }
            } else {
                rs.firstOrNull()
            }
        if (lrclibObject != null) {
            val syncedLyrics = lrclibObject.syncedLyrics
            val plainLyrics = lrclibObject.plainLyrics
            if (!syncedLyrics.isNullOrEmpty()) {
                parseSyncedLyrics(syncedLyrics)
            } else if (!plainLyrics.isNullOrEmpty()) {
                parseUnsyncedLyrics(plainLyrics)
            } else {
                null
            }
        } else {
            null
        }
    }

    suspend fun searchBetterLyrics(
        q_track: String,
        q_artist: String,
        durationSeconds: Int?,
    ) = runCatching {
        val rs =
            lyricsService
                .searchBetterLyrics(
                    q_track = q_track,
                    q_artist = q_artist,
                    durationSeconds = durationSeconds,
                ).body<BetterLyricsResponse>()
        rs.ttml
    }

    suspend fun searchBiniLyrics(
        q_track: String,
        q_artist: String,
    ): String? = runCatching {
        val rs = lyricsService.searchBiniLyrics(q_track, q_artist).body<org.simpmusic.lyrics.models.response.BiniLyricsResponse>()
        val url = rs.results?.find { it.timing_type == "word" }?.lyricsUrl
            ?: rs.results?.firstOrNull()?.lyricsUrl
            ?: return@runCatching null

        lyricsService.httpClient.get(url).bodyAsText()
    }.onFailure { it.printStackTrace() }.getOrNull()

    suspend fun searchAMArtist(
        name: String,
        limit: Int = 5,
    ): Result<List<AMArtistResource>> =
        runCatching {
            val response = lyricsService.searchAMArtist(name, limit)
            if (response.status.value !in 200..299) {
                throw Exception("AM search failed: ${response.status.value}")
            }
            val parsed = response.body<AMSearchResponse>()
            val resources = parsed.resources?.artists.orEmpty()
            // Keep the search ranking from results.data; fall back to the resources map order.
            parsed.results
                ?.artists
                ?.data
                ?.mapNotNull { resources[it.id] }
                ?: resources.values.toList()
        }

    /**
     * Fetch a single artist by id, including [AMEditorialArtwork] (name-logo PNG) and keyColor.
     * Returns null when the id is not present in the response.
     */
    suspend fun getAMArtist(id: String): Result<AMArtistResource?> =
        runCatching {
            val response = lyricsService.getAMArtist(id)
            if (response.status.value !in 200..299) {
                throw Exception("AM artist fetch failed: ${response.status.value}")
            }
            response
                .body<AMSearchResponse>()
                .resources
                ?.artists
                ?.get(id)
        }

    suspend fun getAppleMusicAlbumArtwork(
        albumTitle: String,
        artistName: String,
    ): Result<AppleMusicArtworkResult?> =
        runCatching {
            lyricsService.getAppleMusicAlbumArtwork(albumTitle, artistName)
        }

    suspend fun getAppleMusicSongArtwork(
        songTitle: String,
        artistName: String,
        albumTitle: String? = null,
    ): Result<AppleMusicArtworkResult?> =
        runCatching {
            lyricsService.getAppleMusicSongArtwork(songTitle, artistName, albumTitle)
        }

    suspend fun getSongwriters(
        songTitle: String,
        artistName: String,
    ): List<String>? =
        runCatching {
            lyricsService.getSongwriters(songTitle, artistName)
        }.getOrNull()

    suspend fun searchQQMusicLyrics(
        q_track: String,
        q_artist: String,
        durationSeconds: Int? = null,
    ): Lyrics? = runCatching {
        val query = "$q_track $q_artist".trim()
        val searchResp = lyricsService.searchQQMusic(query)
        if (searchResp.status.value !in 200..299) return@runCatching null

        val searchBody = searchResp.bodyAsText()
        val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
        val searchData = jsonParser.decodeFromString<QQSearchResponse>(searchBody)
        val songList = searchData.req_1?.data?.body?.song?.list
        val topSong = if (durationSeconds != null && durationSeconds > 0) {
            songList?.find { it.interval != null && kotlin.math.abs(it.interval - durationSeconds) <= 5 }
                ?: songList?.firstOrNull()
        } else {
            songList?.firstOrNull()
        } ?: return@runCatching null

        val songId = topSong.id ?: 0L
        val songMid = topSong.mid.orEmpty()

        // 1. Try word-by-word QRC
        if (songId > 0L) {
            try {
                val qrcResp = lyricsService.fetchQQQrcRaw(songId)
                if (qrcResp.status.value in 200..299) {
                    val qrcText = qrcResp.bodyAsText().trim()
                    val contentRegex = Regex("""<content[^>]*>\s*(?:<!\[CDATA\[)?(.*?)(?:\]\]>)?\s*</content>""", RegexOption.DOT_MATCHES_ALL)
                    val match = contentRegex.find(qrcText)
                    val hexBlob = match?.groupValues?.get(1)?.trim()
                        ?: if (qrcText.length > 100 && qrcText.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) qrcText else null

                    if (!hexBlob.isNullOrEmpty() && hexBlob != "0") {
                        val cipherBytes = QQMusicDes.hexToBytes(hexBlob)
                        val decrypted = QQMusicDes.decryptQrc(cipherBytes)
                        val inflated = ZlibInflater.inflate(decrypted)
                        val qrcXml = inflated.decodeToString()
                        val lyrics = QQMusicParser.parseQrcToLyrics(qrcXml)
                        if (!lyrics.lyrics?.lines.isNullOrEmpty()) {
                            return@runCatching lyrics
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "QQ Music QRC error: ${e.message}")
            }
        }

        // 2. Fallback to plain LRC
        if (songMid.isNotEmpty()) {
            try {
                val lrcResp = lyricsService.fetchQQPlainLrc(songMid)
                if (lrcResp.status.value in 200..299) {
                    val body = lrcResp.bodyAsText()
                    val plainData = jsonParser.decodeFromString<QQPlainLyricResponse>(body)
                    val rawLrc = plainData.lyric
                    if (!rawLrc.isNullOrBlank()) {
                        val lyrics = parseSyncedLyrics(rawLrc)
                        if (!lyrics.lyrics?.lines.isNullOrEmpty()) {
                            return@runCatching lyrics
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "QQ Music LRC fallback error: ${e.message}")
            }
        }

        null
    }.onFailure { it.printStackTrace() }.getOrNull()

    private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
        if (this.status.value == 429) {
            tooManyRequest = true
            Logger.e(TAG, "Too many requests: ${this.status.value}")
        } else {
            tooManyRequest = false
        }
        try {
            val data = body<BaseResponse<T>>()
            if (data.error != null) {
                val error = data.error
                Logger.e(TAG, "Error response: ${error.reason} (code: ${error.code})")
                throw Exception("Error response: ${error.reason} (code: ${error.code})")
            }
            return data.data ?: throw Exception("Response data is null")
        } catch (e: Exception) {
            throw e
        }
    }
}