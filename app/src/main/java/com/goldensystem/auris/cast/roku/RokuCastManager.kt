package com.goldensystem.auris.cast.roku

import com.goldensystem.auris.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RokuCastManager @Inject constructor(
    private val discovery: RokuDiscovery,
    private val server: LocalAudioServer,
    private val networkUtils: NetworkUtils,
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

                server.setFile(audioFile)
                if (!server.isAlive) server.start()

                val localIp = networkUtils.getLocalIp()
                val audioUrl = "http://$localIp:8080/audio.mp3"

                controller.play(device.ip, audioUrl)

                Result.success(Unit)
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
    }

    fun onCleared() {
        server.stop()
    }
}