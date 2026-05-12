package com.goldensystem.auris.cast.server

import com.goldensystem.auris.cast.roku.NetworkUtils
import com.goldensystem.auris.cast.session.SessionManager
import com.goldensystem.auris.data.model.Song
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import java.io.File
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AurisHttpServer @Inject constructor(
    private val sessionManager: SessionManager
) {
    private var server: ApplicationEngine? = null
    private var currentFile: File? = null

    fun start(port: Int = 9876, audioFile: File, song: Song): String {
        stop()
        currentFile = audioFile

        server = embeddedServer(Netty, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(30)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            install(PartialContent)
            install(Compression)

            routing {
                configureWebSockets(sessionManager)
                audioRoute(currentFile!!, song)
                coverRoute(song)
                lyricsRoute(song)
            }
        }.start(wait = false)

        return "http://${NetworkUtils.getStaticLocalIp()}:$port"
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        currentFile = null
    }

    fun onCleared() {
        stop()
    }
}

private fun Routing.audioRoute(file: File, song: Song) {
    get("/audio") {
        if (!file.exists() || !file.canRead()) {
            call.respondText("404: Not Found", status = HttpStatusCode.NotFound)
            return@get
        }

        call.respondFile(
            file = file,
            configure = {
                contentType(ContentType.parse(song.mimeType ?: "audio/mpeg"))
            }
        )
    }
}

private fun Routing.coverRoute(song: Song) {
    get("/cover") {
        val coverUri = song.albumArtUriString
        if (coverUri.isNullOrBlank()) {
            call.respondText("No cover", status = HttpStatusCode.NotFound)
            return@get
        }
        call.respondText(coverUri)
    }
}

private fun Routing.lyricsRoute(song: Song) {
    get("/lyrics") {
        val lyrics = song.lyrics ?: ""
        call.respondText(lyrics)
    }
}
