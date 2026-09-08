package org.simpmusic.lyrics.qq

import org.simpmusic.lyrics.domain.Lyrics

object QQMusicParser {
    private val lineRegex = Regex("""\[(\d+),(\d+)\](.*?)(?=\[\d+,\d+\]|$)""", RegexOption.DOT_MATCHES_ALL)
    private val wordRegex = Regex("""(.*?)\((\d+),(\d+)\)""")
    private val creditLineRegex = Regex("""^(?:Lyrics\s+by|Composed\s+by|Written\s+by|作词|作曲)\s*[：:]\s*(.+)""", RegexOption.IGNORE_CASE)

    fun parseQrcToLyrics(qrcXml: String): Lyrics {
        val linesLyrics = ArrayList<Lyrics.LyricsX.Line>()
        val songwritersList = LinkedHashSet<String>()
        var hasWordTiming = false

        for (match in lineRegex.findAll(qrcXml)) {
            val lineStartMs = match.groupValues[1].toLongOrNull() ?: continue
            val lineDurMs = match.groupValues[2].toLongOrNull() ?: 0L
            val body = match.groupValues[3]

            val rawWords = wordRegex.findAll(body)
                .map { Triple(it.groupValues[1], it.groupValues[2].toLongOrNull() ?: 0L, it.groupValues[3].toLongOrNull() ?: 0L) }
                .filter { it.first.isNotEmpty() }
                .toList()

            if (rawWords.isEmpty()) continue

            val joinedText = rawWords.joinToString("") { it.first }.trim()

            // Check if this is a credit / songwriter line
            val creditMatch = creditLineRegex.find(joinedText)
            if (creditMatch != null) {
                val names = creditMatch.groupValues[1]
                    .split(Regex("""[/,&、]"""))
                    .map { it.replace("\"", "").trim() }
                    .filter { it.isNotEmpty() && !it.equals("Various Artists", ignoreCase = true) }
                songwritersList.addAll(names)
            }

            val isBg = joinedText.startsWith("(") || joinedText.startsWith("（")
            val prefix = if (isBg) "[bg]" else ""

            val wordParts = mutableListOf<String>()
            var lastWordEndMs = lineStartMs

            for ((text, wStartMs, wDurMs) in rawWords) {
                val cleanW = if (isBg) text.trim('(', ')', '（', '）') else text
                val wStr = cleanW.trim()
                if (wStr.isEmpty()) continue

                val absStartMs = if (wStartMs >= lineStartMs) wStartMs else lineStartMs + wStartMs
                val absEndMs = absStartMs + wDurMs
                lastWordEndMs = maxOf(lastWordEndMs, absEndMs)

                wordParts.add("<${formatMsToLrc(absStartMs)}>$wStr")
                hasWordTiming = true
            }

            if (wordParts.isNotEmpty()) {
                val endTag = "<${formatMsToLrc(lastWordEndMs)}>"
                val content = "$prefix${wordParts.joinToString(" ")} $endTag"

                linesLyrics.add(
                    Lyrics.LyricsX.Line(
                        startTimeMs = lineStartMs.toString(),
                        endTimeMs = (lineStartMs + lineDurMs).toString(),
                        syllables = listOf(),
                        words = content,
                    ),
                )
            }
        }

        return Lyrics(
            lyrics = Lyrics.LyricsX(
                lines = linesLyrics,
                syncType = if (hasWordTiming) "RICH_SYNCED" else "LINE_SYNCED",
            ),
            songwriters = songwritersList.toList().ifEmpty { null },
        )
    }

    private fun formatMsToLrc(ms: Long): String {
        val minutes = ms / 60_000L
        val seconds = (ms % 60_000L) / 1000L
        val millis = ms % 1000L
        val m = if (minutes < 10) "0$minutes" else "$minutes"
        val s = if (seconds < 10) "0$seconds" else "$seconds"
        val msPadded = millis.toString().padStart(3, '0')
        return "$m:$s.$msPadded"
    }
}
