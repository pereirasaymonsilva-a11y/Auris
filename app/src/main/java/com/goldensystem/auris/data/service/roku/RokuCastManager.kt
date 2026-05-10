package com.goldensystem.auris.data.service.roku

import android.util.Log
import com.goldensystem.auris.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var connectionJob: Job? = null

    private val _connectionState = MutableStateFlow(RokuConnectionState.DISCONNECTED)
    val connectionState: StateFlow<RokuConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<RokuDevice?>(null)
    val connectedDevice: StateFlow<RokuDevice?> = _connectedDevice.asStateFlow()

    /**
     * Conecta a um dispositivo Roku e inicia a reprodução.
     * @param device O dispositivo Roku alvo.
     * @param song A música a ser reproduzida.
     */
    fun connectAndPlay(device: RokuDevice, song: Song) {
        connectionJob?.cancel()
        connectionJob = scope.launch {
            _connectionState.value = RokuConnectionState.CONNECTING
            _connectedDevice.value = device

            try {
                // Verificar se o dispositivo está acessível
                if (!controlService.isDeviceReachable(device)) {
                    Log.w(TAG, "Dispositivo Roku inacessível: ${device.friendlyName}")
                    _connectionState.value = RokuConnectionState.DISCONNECTED
                    _connectedDevice.value = null
                    return@launch
                }

                // Obter o arquivo de áudio local
                val audioFile = getAudioFile(song)
                if (audioFile == null) {
                    Log.e(TAG, "Arquivo de áudio não encontrado para a música: ${song.title}")
                    _connectionState.value = RokuConnectionState.DISCONNECTED
                    _connectedDevice.value = null
                    return@launch
                }

                // Iniciar o servidor HTTP local
                val streamUrl = httpServer.start(audioFile)
                if (streamUrl == null) {
                    Log.e(TAG, "Falha ao iniciar servidor HTTP")
                    _connectionState.value = RokuConnectionState.DISCONNECTED
                    _connectedDevice.value = null
                    return@launch
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
                    Log.e(TAG, "Falha ao iniciar reprodução no Roku")
                    httpServer.stop()
                    _connectionState.value = RokuConnectionState.DISCONNECTED
                    _connectedDevice.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro na conexão com o Roku", e)
                httpServer.stop()
                _connectionState.value = RokuConnectionState.DISCONNECTED
                _connectedDevice.value = null
            }
        }
    }

    /**
     * Desconecta do dispositivo Roku atual.
     */
    fun disconnect() {
        connectionJob?.cancel()
        scope.launch {
            httpServer.stop()
            _connectionState.value = RokuConnectionState.DISCONNECTED
            _connectedDevice.value = null
            Log.d(TAG, "Desconectado do Roku")
        }
    }

    /**
     * Obtém o arquivo de áudio local correspondente à música.
     * Para fontes online, é necessário baixar primeiro (implementação futura).
     */
    private fun getAudioFile(song: Song): File? {
        // Tenta usar o caminho do arquivo local
        if (song.path.isNotBlank()) {
            val file = File(song.path)
            if (file.exists()) return file
        }
        // Para streams online, seria necessário fazer download antes
        // (pode ser implementado posteriormente)
        return null
    }

    /**
     * Determina o formato de áudio com base no tipo MIME ou extensão.
     */
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