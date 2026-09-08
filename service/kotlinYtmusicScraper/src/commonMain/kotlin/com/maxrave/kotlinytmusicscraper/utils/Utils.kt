package com.maxrave.kotlinytmusicscraper.utils

import com.maxrave.kotlinytmusicscraper.models.response.AudioData
import com.maxrave.logger.Logger
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.time.Instant
import kotlin.io.encoding.Base64

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun sha1(str: String): String = MessageDigest.getInstance("SHA-1").digest(str.toByteArray()).toHex()

fun parseCookieString(cookie: String): Map<String, String> {
    val trimmed = cookie.trim()
    if (trimmed.isEmpty()) return emptyMap()

    // Check if it's Netscape HTTP Cookie File format
    if (trimmed.startsWith("# Netscape") || trimmed.contains("\t")) {
        val map = mutableMapOf<String, String>()
        trimmed.lineSequence().forEach { line ->
            val l = line.trim()
            if (l.isNotEmpty() && !l.startsWith("#")) {
                val parts = l.split("\t")
                if (parts.size >= 7) {
                    val name = parts[5].trim()
                    val value = parts[6].trim()
                    if (name.isNotEmpty()) {
                        map[name] = value
                    }
                }
            }
        }
        if (map.isNotEmpty()) return map
    }

    // Standard Cookie header string: "key1=val1; key2=val2" or separated by newlines
    return trimmed
        .split(";", "\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.contains("=") }
        .associate {
            val key = it.substringBefore("=").trim()
            val value = it.substringAfter("=").trim()
            key to value
        }
}


fun String.parseTime(): Int? {
    try {
        val parts =
            if (this.contains(":")) split(":").map { it.toInt() } else split(".").map { it.toInt() }
        if (parts.size == 2) {
            return parts[0] * 60 + parts[1]
        }
        if (parts.size == 3) {
            return parts[0] * 3600 + parts[1] * 60 + parts[2]
        }
    } catch (e: Exception) {
        return null
    }
    return null
}

fun generateNetscapeCookies(
    cookies: Map<String, String>,
    domain: String = ".example.com",
    path: String = "/",
    secure: Boolean = false,
    httpOnly: Boolean = false,
    expirationTimeSeconds: Long = Instant.now().epochSecond + 86400 * 365,
): String {
    val header =
        "# Netscape HTTP Cookie File\n" +
            "# This is a generated file! Do not edit.\n\n"

    val cookieLines =
        cookies
            .map { (name, value) ->
                // Netscape format: domain, domainFlag, path, secure, expiration, name, value
                buildString {
                    append(domain)
                    append("\t")
                    append("TRUE") // domain flag - TRUE means domain includes subdomains
                    append("\t")
                    append(path)
                    append("\t")
                    append(if (secure) "TRUE" else "FALSE")
                    append("\t")
                    append(expirationTimeSeconds)
                    append("\t")
                    append(name)
                    append("\t")
                    append(value)
                }
            }.joinToString("\n")

    return header + cookieLines
}

fun String.decodeTidalManifest(): AudioData? {
    val decodedBytes = Base64.decode(this)
    val jsonString = decodedBytes.decodeToString()
    val json =
        Json {
            explicitNulls = false
            ignoreUnknownKeys = true
        }
    return try {
        json.decodeFromString<AudioData?>(jsonString)
    } catch (e: Exception) {
        Logger.e("Utils", "Failed to decode Tidal manifest: ${e.message}")
        e.printStackTrace()
        null
    }
}

fun String.decodeBase64(): String {
    val decodedBytes = Base64.decode(this)
    return decodedBytes.decodeToString()
}