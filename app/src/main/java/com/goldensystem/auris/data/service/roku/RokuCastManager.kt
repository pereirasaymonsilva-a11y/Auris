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
        private const val CONNECTION_TIMEOUT_MS = 15_000L // 15 segundos máximo
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionJob: Job? = null

    private val _connectionState = MutableStateFlow(RokuConnectionState.DISCONNECTED)
    val connectionState: StateFlow<RokuConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<RokuDevice?>(null)
    val connectedDevice: StateFlow<RokuDevice?> = _connectedDevice.asStateFlow()

    fun connectAndPlay(device: RokuDevice, song: Song) {
        connectionJob?.cancel()
        connectionJob = scope.launch {
            _connectionState.value = RokuConnectionState.CONNECTING
            _connectedDevice.value = device

            try {
                // Timeout geral para evitar "conectando para sempre"
                withTimeout(CONNECTION_TIMEOUT_MS) {
                    // Verificar se o dispositivo está acessível
                    if (!controlService.isDeviceReachable(device)) {
                        Log.w(TAG, "Dispositivo Roku inacessível: ${device.friendlyName}")
                        setDisconnected()
                        return@withTimeout
                    }

                    // Obter o arquivo de áudio local
                    val audioFile = getAudioFile(song)
                    if (audioFile == null) {
                        Log.e(TAG, "Arquivo de áudio não encontrado para a música: ${song.title}")
                        setDisconnected()
                        return@withTimeout
                    }

                    // Iniciar o servidor HTTP local
                    val streamUrl = httpServer.start(audioFile)
                    if (streamUrl == null) {
                        Log.e(TAG, "Falha ao iniciar servidor HTTP")
                        setDisconnected()
                        return@withTimeout
                    }

                    // Enviar o comando de reprodução ao Roku
                    val result = controlService.playStream(
                        device = device,
                        streamUrl = streamUrl,
                        title = song.title,
                        format = getAudioFormat(song)
                    )

                    if (result.isSuccess) {
                        _connectionState.value = RokuConnectionState.PLAYING
                        Log.d(TAG, "Reprodução iniciada no Roku: ${device.friendlyName}")
                    } else {
                        Log.e(TAG, "Falha ao iniciar reprodução no Roku: ${result.exceptionOrNull()?.message}")
                        httpServer.stop()
                        setDisconnected()
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Timeout ao conectar ao Roku")
                httpServer.stop()
                setDisconnected()
            } catch (e: Exception) {
                Log.e(TAG, "Erro na conexão com o Roku", e)
                httpServer.stop()
                setDisconnected()
            }
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
        if (song.path.isNotBlank()) {
            val file = File(song.path)
            if (file.exists()) return file
        }
        return null
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