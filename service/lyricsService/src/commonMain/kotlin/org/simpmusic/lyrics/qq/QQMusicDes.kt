package org.simpmusic.lyrics.qq

/**
 * Pure Kotlin Multiplatform implementation of QQ Music's modified DES algorithm.
 *
 * QQ Music encrypts QRC word-by-word lyric blobs with 3 rounds of DES in ECB mode:
 * 1. DES-decrypt with Key 1 ("!@#)(NHL")
 * 2. DES-encrypt with Key 2 ("123ZXC!@")
 * 3. DES-decrypt with Key 3 ("!@#)(*$%")
 *
 * NOTE: QQ Music's DLL uses Brad Conte's DES implementation which has a well-known
 * typo in sbox4 (index 53 has a duplicated '10' instead of '1'). This class reproduces
 * that exact bug so server payloads decrypt cleanly without any native C/JNI dependencies.
 */
object QQMusicDes {
    private val QRC_KEY1 = byteArrayOf(0x21, 0x40, 0x23, 0x29, 0x28, 0x4E, 0x48, 0x4C) // "!@#)(NHL"
    private val QRC_KEY2 = byteArrayOf(0x31, 0x32, 0x33, 0x5A, 0x58, 0x43, 0x21, 0x40) // "123ZXC!@"
    private val QRC_KEY3 = byteArrayOf(0x21, 0x40, 0x23, 0x29, 0x28, 0x2A, 0x24, 0x25) // "!@#)(*$%"

    private const val MODE_ENCRYPT = 0
    private const val MODE_DECRYPT = 1

    private val sbox1 = intArrayOf(
        14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7,
        0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8,
        4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0,
        15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13,
    )

    private val sbox2 = intArrayOf(
        15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10,
        3, 13, 4, 7, 15, 2, 8, 15, 12, 0, 1, 10, 6, 9, 11, 5,
        0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15,
        13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9,
    )

    private val sbox3 = intArrayOf(
        10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8,
        13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1,
        13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7,
        1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12,
    )

    // Buggy sbox4: index 53 is 10 instead of 1
    private val sbox4 = intArrayOf(
        7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15,
        13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9,
        10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4,
        3, 15, 0, 6, 10, 10, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14,
    )

    private val sbox5 = intArrayOf(
        2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9,
        14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6,
        4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14,
        11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3,
    )

    private val sbox6 = intArrayOf(
        12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11,
        10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8,
        9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6,
        4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13,
    )

    private val sbox7 = intArrayOf(
        4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1,
        13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6,
        1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2,
        6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12,
    )

    private val sbox8 = intArrayOf(
        13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7,
        1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2,
        7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8,
        2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11,
    )

    private val keyRndShift = intArrayOf(1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1)

    private val keyPermC = intArrayOf(
        56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17,
        9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35,
    )

    private val keyPermD = intArrayOf(
        62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21,
        13, 5, 60, 52, 44, 36, 28, 20, 12, 4, 27, 19, 11, 3,
    )

    private val keyCompression = intArrayOf(
        13, 16, 10, 23, 0, 4, 2, 27, 14, 5, 20, 9,
        22, 18, 11, 3, 25, 7, 15, 6, 26, 19, 12, 1,
        40, 51, 30, 36, 46, 54, 29, 39, 50, 44, 32, 47,
        43, 48, 38, 55, 33, 52, 45, 41, 49, 35, 28, 31,
    )

    private fun bitnum(a: ByteArray, b: Int, c: Int): Int {
        val byteIdx = (b / 32) * 4 + 3 - (b % 32) / 8
        val bit = ((a[byteIdx].toInt() and 0xFF) ushr (7 - (b % 8))) and 0x01
        return bit shl c
    }

    private fun bitnumIntR(a: Int, b: Int, c: Int): Int =
        ((a ushr (31 - b)) and 0x01) shl c

    private fun bitnumIntL(a: Int, b: Int, c: Int): Int =
        ((a shl b) and 0x80000000.toInt()) ushr c

    private fun sboxBit(a: Int): Int =
        (a and 0x20) or ((a and 0x1F) shr 1) or ((a and 0x01) shl 4)

    private fun ip(inBytes: ByteArray, offset: Int, state: IntArray) {
        val src = if (offset == 0 && inBytes.size == 8) inBytes else inBytes.copyOfRange(offset, offset + 8)

        state[0] = bitnum(src, 57, 31) or bitnum(src, 49, 30) or bitnum(src, 41, 29) or bitnum(src, 33, 28) or
            bitnum(src, 25, 27) or bitnum(src, 17, 26) or bitnum(src, 9, 25) or bitnum(src, 1, 24) or
            bitnum(src, 59, 23) or bitnum(src, 51, 22) or bitnum(src, 43, 21) or bitnum(src, 35, 20) or
            bitnum(src, 27, 19) or bitnum(src, 19, 18) or bitnum(src, 11, 17) or bitnum(src, 3, 16) or
            bitnum(src, 61, 15) or bitnum(src, 53, 14) or bitnum(src, 45, 13) or bitnum(src, 37, 12) or
            bitnum(src, 29, 11) or bitnum(src, 21, 10) or bitnum(src, 13, 9) or bitnum(src, 5, 8) or
            bitnum(src, 63, 7) or bitnum(src, 55, 6) or bitnum(src, 47, 5) or bitnum(src, 39, 4) or
            bitnum(src, 31, 3) or bitnum(src, 23, 2) or bitnum(src, 15, 1) or bitnum(src, 7, 0)

        state[1] = bitnum(src, 56, 31) or bitnum(src, 48, 30) or bitnum(src, 40, 29) or bitnum(src, 32, 28) or
            bitnum(src, 24, 27) or bitnum(src, 16, 26) or bitnum(src, 8, 25) or bitnum(src, 0, 24) or
            bitnum(src, 58, 23) or bitnum(src, 50, 22) or bitnum(src, 42, 21) or bitnum(src, 34, 20) or
            bitnum(src, 26, 19) or bitnum(src, 18, 18) or bitnum(src, 10, 17) or bitnum(src, 2, 16) or
            bitnum(src, 60, 15) or bitnum(src, 52, 14) or bitnum(src, 44, 13) or bitnum(src, 36, 12) or
            bitnum(src, 28, 11) or bitnum(src, 20, 10) or bitnum(src, 12, 9) or bitnum(src, 4, 8) or
            bitnum(src, 62, 7) or bitnum(src, 54, 6) or bitnum(src, 46, 5) or bitnum(src, 38, 4) or
            bitnum(src, 30, 3) or bitnum(src, 22, 2) or bitnum(src, 14, 1) or bitnum(src, 6, 0)
    }

    private fun invIp(state: IntArray, outBytes: ByteArray, offset: Int) {
        outBytes[offset + 3] = (bitnumIntR(state[1], 7, 7) or bitnumIntR(state[0], 7, 6) or bitnumIntR(state[1], 15, 5) or
            bitnumIntR(state[0], 15, 4) or bitnumIntR(state[1], 23, 3) or bitnumIntR(state[0], 23, 2) or
            bitnumIntR(state[1], 31, 1) or bitnumIntR(state[0], 31, 0)).toByte()

        outBytes[offset + 2] = (bitnumIntR(state[1], 6, 7) or bitnumIntR(state[0], 6, 6) or bitnumIntR(state[1], 14, 5) or
            bitnumIntR(state[0], 14, 4) or bitnumIntR(state[1], 22, 3) or bitnumIntR(state[0], 22, 2) or
            bitnumIntR(state[1], 30, 1) or bitnumIntR(state[0], 30, 0)).toByte()

        outBytes[offset + 1] = (bitnumIntR(state[1], 5, 7) or bitnumIntR(state[0], 5, 6) or bitnumIntR(state[1], 13, 5) or
            bitnumIntR(state[0], 13, 4) or bitnumIntR(state[1], 21, 3) or bitnumIntR(state[0], 21, 2) or
            bitnumIntR(state[1], 29, 1) or bitnumIntR(state[0], 29, 0)).toByte()

        outBytes[offset + 0] = (bitnumIntR(state[1], 4, 7) or bitnumIntR(state[0], 4, 6) or bitnumIntR(state[1], 12, 5) or
            bitnumIntR(state[0], 12, 4) or bitnumIntR(state[1], 20, 3) or bitnumIntR(state[0], 20, 2) or
            bitnumIntR(state[1], 28, 1) or bitnumIntR(state[0], 28, 0)).toByte()

        outBytes[offset + 7] = (bitnumIntR(state[1], 3, 7) or bitnumIntR(state[0], 3, 6) or bitnumIntR(state[1], 11, 5) or
            bitnumIntR(state[0], 11, 4) or bitnumIntR(state[1], 19, 3) or bitnumIntR(state[0], 19, 2) or
            bitnumIntR(state[1], 27, 1) or bitnumIntR(state[0], 27, 0)).toByte()

        outBytes[offset + 6] = (bitnumIntR(state[1], 2, 7) or bitnumIntR(state[0], 2, 6) or bitnumIntR(state[1], 10, 5) or
            bitnumIntR(state[0], 10, 4) or bitnumIntR(state[1], 18, 3) or bitnumIntR(state[0], 18, 2) or
            bitnumIntR(state[1], 26, 1) or bitnumIntR(state[0], 26, 0)).toByte()

        outBytes[offset + 5] = (bitnumIntR(state[1], 1, 7) or bitnumIntR(state[0], 1, 6) or bitnumIntR(state[1], 9, 5) or
            bitnumIntR(state[0], 9, 4) or bitnumIntR(state[1], 17, 3) or bitnumIntR(state[0], 17, 2) or
            bitnumIntR(state[1], 25, 1) or bitnumIntR(state[0], 25, 0)).toByte()

        outBytes[offset + 4] = (bitnumIntR(state[1], 0, 7) or bitnumIntR(state[0], 0, 6) or bitnumIntR(state[1], 8, 5) or
            bitnumIntR(state[0], 8, 4) or bitnumIntR(state[1], 16, 3) or bitnumIntR(state[0], 16, 2) or
            bitnumIntR(state[1], 24, 1) or bitnumIntR(state[0], 24, 0)).toByte()
    }

    private fun fFunc(state: Int, key: IntArray): Int {
        val t1 = bitnumIntL(state, 31, 0) or ((state and 0xF0000000.toInt()) ushr 1) or bitnumIntL(state, 4, 5) or
            bitnumIntL(state, 3, 6) or ((state and 0x0F000000) ushr 3) or bitnumIntL(state, 8, 11) or
            bitnumIntL(state, 7, 12) or ((state and 0x00F00000) ushr 5) or bitnumIntL(state, 12, 17) or
            bitnumIntL(state, 11, 18) or ((state and 0x000F0000) ushr 7) or bitnumIntL(state, 16, 23)

        val t2 = bitnumIntL(state, 15, 0) or ((state and 0x0000F000) shl 15) or bitnumIntL(state, 20, 5) or
            bitnumIntL(state, 19, 6) or ((state and 0x00000F00) shl 13) or bitnumIntL(state, 24, 11) or
            bitnumIntL(state, 23, 12) or ((state and 0x000000F0) shl 11) or bitnumIntL(state, 28, 17) or
            bitnumIntL(state, 27, 18) or ((state and 0x0000000F) shl 9) or bitnumIntL(state, 0, 23)

        val s0 = ((t1 ushr 24) and 0xFF) xor key[0]
        val s1 = ((t1 ushr 16) and 0xFF) xor key[1]
        val s2 = ((t1 ushr 8) and 0xFF) xor key[2]
        val s3 = ((t2 ushr 24) and 0xFF) xor key[3]
        val s4 = ((t2 ushr 16) and 0xFF) xor key[4]
        val s5 = ((t2 ushr 8) and 0xFF) xor key[5]

        val st = (sbox1[sboxBit(s0 ushr 2)] shl 28) or
            (sbox2[sboxBit(((s0 and 0x03) shl 4) or (s1 ushr 4))] shl 24) or
            (sbox3[sboxBit(((s1 and 0x0F) shl 2) or (s2 ushr 6))] shl 20) or
            (sbox4[sboxBit(s2 and 0x3F)] shl 16) or
            (sbox5[sboxBit(s3 ushr 2)] shl 12) or
            (sbox6[sboxBit(((s3 and 0x03) shl 4) or (s4 ushr 4))] shl 8) or
            (sbox7[sboxBit(((s4 and 0x0F) shl 2) or (s5 ushr 6))] shl 4) or
            sbox8[sboxBit(s5 and 0x3F)]

        return bitnumIntL(st, 15, 0) or bitnumIntL(st, 6, 1) or bitnumIntL(st, 19, 2) or
            bitnumIntL(st, 20, 3) or bitnumIntL(st, 28, 4) or bitnumIntL(st, 11, 5) or
            bitnumIntL(st, 27, 6) or bitnumIntL(st, 16, 7) or bitnumIntL(st, 0, 8) or
            bitnumIntL(st, 14, 9) or bitnumIntL(st, 22, 10) or bitnumIntL(st, 25, 11) or
            bitnumIntL(st, 4, 12) or bitnumIntL(st, 17, 13) or bitnumIntL(st, 30, 14) or
            bitnumIntL(st, 9, 15) or bitnumIntL(st, 1, 16) or bitnumIntL(st, 7, 17) or
            bitnumIntL(st, 23, 18) or bitnumIntL(st, 13, 19) or bitnumIntL(st, 31, 20) or
            bitnumIntL(st, 26, 21) or bitnumIntL(st, 2, 22) or bitnumIntL(st, 8, 23) or
            bitnumIntL(st, 18, 24) or bitnumIntL(st, 12, 25) or bitnumIntL(st, 29, 26) or
            bitnumIntL(st, 5, 27) or bitnumIntL(st, 21, 28) or bitnumIntL(st, 10, 29) or
            bitnumIntL(st, 3, 30) or bitnumIntL(st, 24, 31)
    }

    private fun desKeySetup(key: ByteArray, mode: Int): Array<IntArray> {
        val schedule = Array(16) { IntArray(6) }
        var c = 0
        var d = 0
        for (i in 0 until 28) {
            c = c or bitnum(key, keyPermC[i], 31 - i)
            d = d or bitnum(key, keyPermD[i], 31 - i)
        }

        for (i in 0 until 16) {
            val shift = keyRndShift[i]
            c = ((c shl shift) or (c ushr (28 - shift))) and 0xFFFFFFF0.toInt()
            d = ((d shl shift) or (d ushr (28 - shift))) and 0xFFFFFFF0.toInt()

            val toGen = if (mode == MODE_DECRYPT) 15 - i else i
            for (j in 0 until 24) {
                schedule[toGen][j / 8] = schedule[toGen][j / 8] or bitnumIntR(c, keyCompression[j], 7 - (j % 8))
            }
            for (j in 24 until 48) {
                schedule[toGen][j / 8] = schedule[toGen][j / 8] or bitnumIntR(d, keyCompression[j] - 27, 7 - (j % 8))
            }
        }
        return schedule
    }

    private fun desCryptBlock(inBytes: ByteArray, inOffset: Int, outBytes: ByteArray, outOffset: Int, schedule: Array<IntArray>) {
        val state = IntArray(2)
        ip(inBytes, inOffset, state)

        for (idx in 0 until 15) {
            val t = state[1]
            state[1] = fFunc(state[1], schedule[idx]) xor state[0]
            state[0] = t
        }
        state[0] = fFunc(state[1], schedule[15]) xor state[0]

        invIp(state, outBytes, outOffset)
    }

    private fun desEcb(data: ByteArray, key: ByteArray, mode: Int): ByteArray {
        val schedule = desKeySetup(key, mode)
        val out = ByteArray(data.size)
        val blocks = data.size / 8
        for (i in 0 until blocks) {
            val offset = i * 8
            desCryptBlock(data, offset, out, offset, schedule)
        }
        return out
    }

    /**
     * Decrypts raw QQ Music QRC bytes using 3-round DES ECB.
     */
    fun decryptQrc(data: ByteArray): ByteArray {
        if (data.size % 8 != 0) {
            throw IllegalArgumentException("QRC cipher data length must be a multiple of 8, got ${data.size}")
        }
        val step1 = desEcb(data, QRC_KEY1, MODE_DECRYPT)
        val step2 = desEcb(step1, QRC_KEY2, MODE_ENCRYPT)
        return desEcb(step2, QRC_KEY3, MODE_DECRYPT)
    }

    /**
     * Converts a hex string into a byte array.
     */
    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim().filter { !it.isWhitespace() }
        val len = clean.length
        val result = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            val hi = clean[i].digitToInt(16)
            val lo = clean[i + 1].digitToInt(16)
            result[i / 2] = ((hi shl 4) or lo).toByte()
            i += 2
        }
        return result
    }
}
