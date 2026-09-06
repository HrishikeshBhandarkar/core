package org.simpmusic.lyrics.am

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    val service = AppleMusicArtworkService(client)
    
    println("Testing Album Artwork: Hit Me Hard And Soft by Billie Eilish")
    val albumResult = service.getAlbumArtwork("HIT ME HARD AND SOFT", "Billie Eilish")
    println(albumResult)
    
    println("\nTesting Song Artwork: Blinding Lights by The Weeknd")
    val songResult = service.getSongArtwork("Blinding Lights", "The Weeknd", "After Hours")
    println(songResult)
    
    println("\nTesting Song Artwork: Fortnight by Taylor Swift")
    val songResult2 = service.getSongArtwork("Fortnight (feat. Post Malone)", "Taylor Swift", "THE TORTURED POETS DEPARTMENT")
    println(songResult2)

    client.close()
}
