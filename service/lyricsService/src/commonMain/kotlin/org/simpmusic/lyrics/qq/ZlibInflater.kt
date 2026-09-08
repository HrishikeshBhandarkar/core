package org.simpmusic.lyrics.qq

expect object ZlibInflater {
    fun inflate(data: ByteArray): ByteArray
}
