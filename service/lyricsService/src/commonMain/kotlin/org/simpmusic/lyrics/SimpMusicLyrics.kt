package org.simpmusic.lyrics

import com.maxrave.ktorext.curl.CurlLogger
import com.maxrave.ktorext.encoding.brotli
import com.maxrave.ktorext.getEngine
import com.maxrave.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyConfig
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.simpmusic.lyrics.am.AMTokenManager
import org.simpmusic.lyrics.am.AppleMusicArtworkService
import org.simpmusic.lyrics.models.request.LyricsBody
import org.simpmusic.lyrics.models.request.TranslatedLyricsBody
import org.simpmusic.lyrics.models.request.VoteBody
import org.simpmusic.lyrics.qq.QQSearchRequest

class SimpMusicLyrics {
    internal var httpClient = createClient()
    private var amArtworkService = AppleMusicArtworkService(httpClient)
    var proxy: ProxyConfig? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
            amArtworkService = AppleMusicArtworkService(httpClient)
        }

    private val baseUrl = "https://api-lyrics.simpmusic.org/v1/"

    private val amTokenManager = AMTokenManager()

    private fun createClient() =
        HttpClient(getEngine()) {
            expectSuccess = false
            followRedirects = true
            install(HttpCache)
            install(HttpTimeout)
            install(CurlLogger) {
                logger = { Logger.d("SimpMusicLyrics", it) }
            }
            install(HttpSend) {
                maxSendCount = 100
            }
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    },
                )
            }
            install(ContentEncoding) {
                brotli(1.0F)
                gzip(0.9F)
                deflate(0.8F)
            }
            defaultRequest {
                url("https://api-lyrics.simpmusic.org/v1")
            }
            if (proxy != null) {
                engine {
                    proxy = this@SimpMusicLyrics.proxy
                }
            }
        }

    private fun HttpRequestBuilder.buildDefaultHeaders(
        timestamp: String? = null,
        hmac: String? = null,
    ) {
        headers {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.UserAgent, "SimpMusicLyrics/1.0")
            header(HttpHeaders.ContentType, "application/json")
            timestamp?.let {
                header("X-Timestamp", it)
            }
            hmac?.let {
                header("X-HMAC", it)
            }
        }
    }

    private fun HttpRequestBuilder.buildAMHeaders(token: String) {
        headers {
            header("Authorization", "Bearer $token")
            header("Origin", "https://music.apple.com")
            header("Referer", "https://music.apple.com/")
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:152.0) Gecko/20100101 Firefox/152.0",
            )
            header(HttpHeaders.Accept, "application/json")
        }
    }

    suspend fun findLyricsByVideoId(videoId: String) =
        httpClient.get(baseUrl + videoId) {
            buildDefaultHeaders()
        }

    suspend fun findTranslatedLyrics(
        videoId: String,
        language: String,
    ) = httpClient.get(baseUrl + "translated/$videoId/$language") {
        buildDefaultHeaders()
    }

    suspend fun insertLyrics(
        lyricsBody: LyricsBody,
        hmacTimestamp: Pair<String, String>,
    ) = httpClient.post {
        buildDefaultHeaders(
            timestamp = hmacTimestamp.second,
            hmac = hmacTimestamp.first,
        )
        setBody(lyricsBody)
    }

    suspend fun insertTranslatedLyrics(
        translatedLyricsBody: TranslatedLyricsBody,
        hmacTimestamp: Pair<String, String>,
    ) = httpClient.post(baseUrl + "translated") {
        buildDefaultHeaders(
            timestamp = hmacTimestamp.second,
            hmac = hmacTimestamp.first,
        )
        setBody(translatedLyricsBody)
    }

    suspend fun voteLyrics(
        id: String,
        upvote: Boolean,
        hmacTimestamp: Pair<String, String>,
    ) = httpClient.post(baseUrl + "vote") {
        buildDefaultHeaders(
            timestamp = hmacTimestamp.second,
            hmac = hmacTimestamp.first,
        )
        setBody(
            VoteBody(
                id = id,
                vote = if (upvote) 1 else 0, // 1 for upvote, 0 for downvote
            ),
        )
    }

    suspend fun voteTranslatedLyrics(
        id: String,
        upvote: Boolean,
        hmacTimestamp: Pair<String, String>,
    ) = httpClient.post(baseUrl + "translated/vote") {
        buildDefaultHeaders(
            timestamp = hmacTimestamp.second,
            hmac = hmacTimestamp.first,
        )
        setBody(
            VoteBody(
                id = id,
                vote = if (upvote) 1 else 0, // 1 for upvote, 0 for downvote
            ),
        )
    }

    suspend fun searchLrclibLyrics(
        q_track: String,
        q_artist: String,
    ) = httpClient.get("https://lrclib.net/api/search") {
        buildDefaultHeaders()
        parameter("q", "$q_artist $q_track")
    }

    suspend fun searchBetterLyrics(
        q_track: String,
        q_artist: String,
        durationSeconds: Int?,
    ) = httpClient.get("https://lyrics-api.boidu.dev/getLyrics") {
        buildDefaultHeaders()
        parameter("s", q_track)
        parameter("a", q_artist)
        durationSeconds?.let {
            parameter("d", it)
        }
    }

    suspend fun searchBiniLyrics(
        q_track: String,
        q_artist: String,
    ) = httpClient.get("https://lyrics-api.binimum.org/getLyrics") {
        buildDefaultHeaders()
        parameter("q", "$q_artist $q_track")
    }

    suspend fun searchAMArtist(
        name: String,
        limit: Int,
    ): HttpResponse {
        val response = requestAMSearch(name, limit, amTokenManager.getToken(httpClient))
        // Expired bearer -> refresh the token once and retry on a fresh request.
        if (response.status.value == 401) {
            amTokenManager.clearToken()
            return requestAMSearch(name, limit, amTokenManager.getToken(httpClient))
        }
        return response
    }

    private suspend fun requestAMSearch(
        name: String,
        limit: Int,
        token: String,
    ) = httpClient.get("https://amp-api-edge.music.apple.com/v1/catalog/us/search") {
        parameter("term", name)
        parameter("types", "artists")
        parameter("fields[artists]", "url,name,artwork")
        parameter("art[url]", "f")
        parameter("format[resources]", "map")
        parameter("extend", "artistUrl")
        parameter("l", "en-US")
        parameter("limit", limit)
        parameter("platform", "web")
        buildAMHeaders(token)
    }

    suspend fun getAMArtist(id: String): HttpResponse {
        val response = requestAMArtist(id, amTokenManager.getToken(httpClient))
        // Expired bearer -> refresh the token once and retry on a fresh request.
        if (response.status.value == 401) {
            amTokenManager.clearToken()
            return requestAMArtist(id, amTokenManager.getToken(httpClient))
        }
        return response
    }

    private suspend fun requestAMArtist(
        id: String,
        token: String,
    ) = httpClient.get("https://amp-api.music.apple.com/v1/catalog/us/artists/$id") {
        parameter("art[url]", "c,f")
        parameter("extend", "editorialArtwork,hero,keyColor")
        parameter("format[resources]", "map")
        parameter("l", "en-US")
        parameter("platform", "web")
        buildAMHeaders(token)
    }

    suspend fun getAppleMusicAlbumArtwork(albumName: String, artistName: String) =
        amArtworkService.getAlbumArtwork(albumName, artistName)

    suspend fun getAppleMusicSongArtwork(songTitle: String, artistName: String, albumName: String? = null) =
        amArtworkService.getSongArtwork(songTitle, artistName, albumName)

    suspend fun getSongwriters(songTitle: String, artistName: String) =
        amArtworkService.getSongwriters(songTitle, artistName)

    suspend fun searchQQMusic(query: String): HttpResponse {
        val req = QQSearchRequest(
            req_1 = QQSearchRequest.QQReq1(
                param = QQSearchRequest.QQSearchParam(
                    query = query,
                ),
            ),
        )
        val jsonStr = Json { encodeDefaults = true }.encodeToString(req)
        return httpClient.get("https://u.y.qq.com/cgi-bin/musicu.fcg") {
            timeout {
                requestTimeoutMillis = 45_000L
                connectTimeoutMillis = 45_000L
                socketTimeoutMillis = 45_000L
            }
            header("Referer", "https://y.qq.com/")
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            )
            parameter("data", jsonStr)
        }
    }

    suspend fun fetchQQQrcRaw(songId: Long): HttpResponse {
        return httpClient.get("https://c.y.qq.com/qqmusic/fcgi-bin/lyric_download.fcg") {
            timeout {
                requestTimeoutMillis = 45_000L
                connectTimeoutMillis = 45_000L
                socketTimeoutMillis = 45_000L
            }
            header("Referer", "https://y.qq.com/")
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            )
            parameter("version", "15")
            parameter("miniversion", "82")
            parameter("lrctype", "4")
            parameter("musicid", songId.toString())
        }
    }

    suspend fun fetchQQPlainLrc(songMid: String): HttpResponse {
        return httpClient.get("https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg") {
            timeout {
                requestTimeoutMillis = 45_000L
                connectTimeoutMillis = 45_000L
                socketTimeoutMillis = 45_000L
            }
            header("Referer", "https://y.qq.com/")
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            )
            parameter("nobase64", "1")
            parameter("songmid", songMid)
            parameter("g_tk", "5381")
            parameter("loginUin", "0")
            parameter("hostUin", "0")
            parameter("format", "json")
            parameter("inCharset", "utf8")
            parameter("outCharset", "utf-8")
            parameter("notice", "0")
            parameter("platform", "yqq.json")
            parameter("needNewCode", "0")
        }
    }
}