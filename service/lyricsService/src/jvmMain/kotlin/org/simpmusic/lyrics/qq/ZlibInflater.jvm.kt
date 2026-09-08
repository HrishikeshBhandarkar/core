package org.simpmusic.lyrics.qq

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

actual object ZlibInflater {
    actual fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val outputStream = ByteArrayOutputStream(data.size * 2)
        val buffer = ByteArray(2048)
        try {
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                }
                outputStream.write(buffer, 0, count)
            }
            return outputStream.toByteArray()
        } finally {
            inflater.end()
        }
    }
}
