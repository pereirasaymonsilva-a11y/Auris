package com.goldensystem.auris.cast.roku

import com.goldensystem.auris.cast.metadata.MetadataProvider
import com.goldensystem.auris.cast.protocol.PlayMessage
import com.goldensystem.auris.cast.server.AurisHttpServer
import com.goldensystem.auris.cast.session.SessionManager
import com.goldensystem.auris.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RokuCastManager @Inject constructor(
    private val discovery: RokuDiscovery,
    private val server: AurisHttpServer,
    private val sessionManager: SessionManager,
    private val metadataProvider: MetadataProvider,
    private val controller: RokuController
) {
    suspend fun discoverDevices(): List<RokuDevice> {
        return discovery.discover()
    }

    suspend fun castToDevice(device: RokuDevice, song: Song): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val audioFile = getAudioFile(song)
                    ?: return@withContext Result.failure(Exception("Arquivo de áudio não encontrado"))

                val baseUrl = server.start(port = 9876, audioFile = audioFile, song = song)
                val session = sessionManager.createSession(rokuIp = device.ip)

                val wsUrl = "ws://${NetworkUtils.getStaticLocalIp()}:9876/ws?token=${session.sessionToken}"
                controller.launchReceiver(device.ip, wsUrl)

                withTimeout(15_000L) {
                    while (session.websocketClients.isEmpty()) {
                        delay(100)
                    }

                    val meta = metadataProvider.extractMeta(song, baseUrl)
                    val playMsg = PlayMessage(
                        trackId = meta["trackId"]!!,
                        url = "${baseUrl}/audio",
                        title = meta["title"]!!,
                        artist = meta["artist"]!!,
                        album = meta["album"]!!,
                        cover = meta["cover"]!!,
                        lyrics = meta["lyrics"]!!,
                        mime = meta["mime"]!!,
                        duration = meta["duration"]!!.toLong()
                    )
                    session.sendPlayMessage(playMsg)
                }

                Result.success(Unit)
            } catch (e: TimeoutCancellationException) {
                Result.failure(Exception("Roku não conectou ao WebSocket"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getAudioFile(song: Song): File? {
        if (song.path.isBlank()) return null
        val file = File(song.path)
        return if (file.exists() && file.canRead()) file else null
    }

    fun stopServer() {
        server.stop()
        sessionManager.endSession()
    }

    fun onCleared() {
        server.onCleared()
        sessionManager.endSession()
    }
}
