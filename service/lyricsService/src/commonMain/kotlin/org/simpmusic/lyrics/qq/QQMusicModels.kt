package org.simpmusic.lyrics.qq

import kotlinx.serialization.Serializable

@Serializable
data class QQSearchRequest(
    val comm: QQComm = QQComm(),
    val req_1: QQReq1,
) {
    @Serializable
    data class QQComm(
        val g_tk: Long = 5381,
        val uin: Long = 1152921504916411742,
        val format: String = "json",
        val inCharset: String = "utf-8",
        val outCharset: String = "utf-8",
        val notice: Int = 0,
        val platform: String = "h5",
        val needNewCode: Int = 1,
        val ct: Int = 23,
        val cv: Int = 0,
    )

    @Serializable
    data class QQReq1(
        val method: String = "DoSearchForQQMusicDesktop",
        val module: String = "music.search.SearchCgiService",
        val param: QQSearchParam,
    )

    @Serializable
    data class QQSearchParam(
        val num_per_page: Int = 10,
        val page_num: Int = 1,
        val query: String,
        val search_type: Int = 0,
    )
}

@Serializable
data class QQSearchResponse(
    val code: Int? = null,
    val req_1: QQSearchReq1Result? = null,
) {
    @Serializable
    data class QQSearchReq1Result(
        val code: Int? = null,
        val data: QQSearchData? = null,
    )

    @Serializable
    data class QQSearchData(
        val code: Int? = null,
        val body: QQSearchBody? = null,
    )

    @Serializable
    data class QQSearchBody(
        val song: QQSongContainer? = null,
    )

    @Serializable
    data class QQSongContainer(
        val list: List<QQSongItem>? = null,
    )

    @Serializable
    data class QQSongItem(
        val id: Long? = null,
        val mid: String? = null,
        val name: String? = null,
        val title: String? = null,
        val singer: List<QQSingerItem>? = null,
        val interval: Int? = null,
    )

    @Serializable
    data class QQSingerItem(
        val id: Long? = null,
        val mid: String? = null,
        val name: String? = null,
        val title: String? = null,
    )
}

@Serializable
data class QQPlainLyricResponse(
    val retcode: Int? = null,
    val code: Int? = null,
    val lyric: String? = null,
    val trans: String? = null,
)
