package com.goldensystem.auris.data.service.roku

import android.util.Log
import com.goldensystem.auris.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class RokuConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    PLAYING
}

@Singleton
class RokuCastManager @Inject constructor(
    private val httpServer: RokuHttpServer,
    private val controlService: RokuControlService
) {
    companion object {
        private const val TAG = "RokuCastManager"
        private const val CONNECTION_TIMEOUT_MS = 15_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionJob: Job? = null

    private val _connectionState = MutableStateFlow(RokuConnectionState.DISCONNECTED)
    val connectionState: StateFlow<RokuConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<RokuDevice?>(null)
    val connectedDevice: StateFlow<RokuDevice?> = _connectedDevice.asStateFlow()

    suspend fun connectAndPlay(device: RokuDevice, song: Song): Result<Unit> {
        connectionJob?.cancel()
        _connectionState.value = RokuConnectionState.CONNECTING
        _connectedDevice.value = device

        return try {
            withTimeout(CONNECTION_TIMEOUT_MS) {
                if (!controlService.isDeviceReachable(device)) {
                    setDisconnected()
                    return@withTimeout Result.failure<Unit>(Exception("Roku inacessível"))
                }

                val audioFile = getAudioFile(song)
                if (audioFile == null) {
                    setDisconnected()
                    return@withTimeout Result.failure<Unit>(Exception("Arquivo de áudio não encontrado (apenas músicas locais são suportadas)"))
                }

                val streamUrl = httpServer.start(audioFile)
                if (streamUrl == null) {
                    setDisconnected()
                    return@withTimeout Result.failure<Unit>(Exception("Falha ao iniciar servidor HTTP"))
                }

                Log.d(TAG, "Tentando iniciar stream: $streamUrl")

                val result = controlService.playStream(
                    device = device,
                    streamUrl = streamUrl,
                    title = song.title
                )

                if (result.isSuccess) {
                    _connectionState.value = RokuConnectionState.PLAYING
                    Result.success(Unit)
                } else {
                    httpServer.stop()
                    setDisconnected()
                    Result.failure(result.exceptionOrNull() ?: Exception("Erro ao iniciar reprodução"))
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Timeout ao conectar ao Roku")
            httpServer.stop()
            setDisconnected()
            Result.failure(Exception("Tempo limite excedido ao conectar"))
        } catch (e: Exception) {
            Log.e(TAG, "Erro na conexão com o Roku", e)
            httpServer.stop()
            setDisconnected()
            Result.failure(e)
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        scope.launch {
            httpServer.stop()
            setDisconnected()
        }
    }

    private fun setDisconnected() {
        _connectionState.value = RokuConnectionState.DISCONNECTED
        _connectedDevice.value = null
    }

    private fun getAudioFile(song: Song): File? {
        if (song.path.isBlank()) {
            Log.e(TAG, "Caminho do arquivo vazio para ${song.title}")
            return null
        }
        val file = File(song.path)
        if (!file.exists()) {
            Log.e(TAG, "Arquivo não existe: ${file.absolutePath}")
            return null
        }
        if (!file.canRead()) {
            Log.e(TAG, "Sem permissão de leitura: ${file.absolutePath}")
            return null
        }
        return file
    }

    private fun getAudioFormat(song: Song): String {
        return when {
            song.mimeType?.contains("m4a") == true -> "m4a"
            song.mimeType?.contains("aac") == true -> "aac"
            song.mimeType?.contains("flac") == true -> "flac"
            song.mimeType?.contains("ogg") == true -> "ogg"
            song.mimeType?.contains("wav") == true -> "wav"
            else -> "mp3"
        }
    }

    fun onCleared() {
        disconnect()
    }
}