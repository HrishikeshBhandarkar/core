package org.simpmusic.lyrics.qq

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QQMusicDesTest {
    @Test
    fun testDes8ByteVector() {
        val input = "3132333435363738" // "12345678"
        val bytes = QQMusicDes.hexToBytes(input)
        val decrypted = QQMusicDes.decryptQrc(bytes)

        // From test_des_python.py reference:
        // Pass 1 dec: 15b848a152de7ded
        // Pass 2 enc: 863dc8e1c3ddbe88
        // Pass 3 dec: fa56074a8b5cac66
        val hex = decrypted.joinToString("") { "%02x".format(it) }
        assertEquals("fa56074a8b5cac66", hex)
    }

    @Test
    fun testParseQrcXml() {
        val sampleXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <QrcInfos>
            <LyricInfo>
            <Lyric_1 LyricType="1" LyricContent="
            [0,4690]Blinding(0,1042)Lights(1042,1042)-(2084,1042)The(3126,1042)Weeknd(4168,521)
            [4690,4690]Lyrics by ： Max Martin / Abel Tesfaye(0,4690)
            [27672,2784]I've(0,200)been(200,144)tryna(344,176)call(520,632)
            [30456,2224](I've(0,184)been(184,177)on(361,191)my(552,176)own)(728,1496)
            "/>
            </LyricInfo>
            </QrcInfos>
        """.trimIndent()

        val lyrics = QQMusicParser.parseQrcToLyrics(sampleXml)
        val lyricsX = lyrics.lyrics
        assertNotNull(lyricsX)
        assertEquals("RICH_SYNCED", lyricsX.syncType)
        val lines = lyricsX.lines
        assertNotNull(lines)
        assertTrue(lines.isNotEmpty())
        assertEquals(listOf("Max Martin", "Abel Tesfaye"), lyrics.songwriters)

        // Verify line 0
        val line0 = lines[0]
        assertEquals("0", line0.startTimeMs)
        assertTrue(line0.words.contains("<00:00.000>Blinding"))
        assertTrue(line0.words.contains("<00:01.042>Lights"))

        // Verify background vocal line (parentheses)
        val bgLine = lines.find { it.words.startsWith("[bg]") }
        assertNotNull(bgLine, "Background vocal line should start with [bg]")
        assertTrue(bgLine.words.contains("I've"))
    }

    @Test
    fun testLiveQQMusicSearch() = kotlinx.coroutines.runBlocking {
        val client = org.simpmusic.lyrics.SimpMusicLyricsClient()
        val result = client.searchQQMusicLyrics("Just Keep Watching", "Tate McRae")
        assertNotNull(result, "QQ Music search should return lyrics")
        assertEquals("RICH_SYNCED", result.lyrics?.syncType)
        assertTrue(!result.lyrics?.lines.isNullOrEmpty(), "Should have parsed lines")
        assertTrue(!result.songwriters.isNullOrEmpty(), "Should extract songwriters")
    }
}
