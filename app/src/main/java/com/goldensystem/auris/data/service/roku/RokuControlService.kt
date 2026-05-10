package com.goldensystem.auris.data.service.roku

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RokuControlService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "RokuControl"
    }

    private val client = okHttpClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * Envia um comando de tecla para o Roku.
     * Exemplos de teclas: play, pause, stop, select, etc.
     */
    suspend fun sendKeyPress(device: RokuDevice, key: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://${device.ipAddress}:${device.port}/keypress/$key"
                val request = Request.Builder()
                    .url(url)
                    .post("".toRequestBody())
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "Comando '$key' enviado com sucesso para ${device.friendlyName}")
                    Result.success(Unit)
                } else {
                    Log.w(TAG, "Falha ao enviar comando '$key': HTTP ${response.code}")
                    Result.failure(Exception("Erro HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao enviar comando '$key'", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Inicia a reprodução de um fluxo de áudio no Roku.
     * @param device O dispositivo Roku alvo.
     * @param streamUrl A URL pública do fluxo de áudio (deve ser acessível pelo Roku).
     * @param title O título da mídia (opcional, aparece na interface do Roku).
     * @param format O formato do fluxo (ex: "mp3", "m4a", "aac").
     */
    suspend fun playStream(
        device: RokuDevice,
        streamUrl: String,
        title: String? = null,
        format: String = "mp3"
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedStreamUrl = URLEncoder.encode(streamUrl, "UTF-8")
                val encodedTitle = if (title != null) URLEncoder.encode(title, "UTF-8") else ""
                val url = "http://${device.ipAddress}:${device.port}/input/15985?" +
                        "t=v" +
                        "&u=$encodedStreamUrl" +
                        "&videoName=$encodedTitle" +
                        "&videoFormat=mpegts" +
                        "&songFormat=$format"

                val request = Request.Builder()
                    .url(url)
                    .post("".toRequestBody())
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "Stream iniciado em ${device.friendlyName}: $streamUrl")
                    Result.success(Unit)
                } else {
                    Log.w(TAG, "Falha ao iniciar stream: HTTP ${response.code}")
                    Result.failure(Exception("Erro HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao iniciar stream", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Para a reprodução no Roku.
     */
    suspend fun stop(device: RokuDevice): Result<Unit> {
        return sendKeyPress(device, "Home")
    }

    /**
     * Pausa a reprodução no Roku.
     */
    suspend fun pause(device: RokuDevice): Result<Unit> {
        return sendKeyPress(device, "Play") // Toggle play/pause
    }

    /**
     * Retoma a reprodução no Roku.
     */
    suspend fun resume(device: RokuDevice): Result<Unit> {
        return sendKeyPress(device, "Play")
    }

    /**
     * Avança para o próximo item.
     */
    suspend fun next(device: RokuDevice): Result<Unit> {
        return sendKeyPress(device, "Fwd")
    }

    /**
     * Volta para o item anterior.
     */
    suspend fun previous(device: RokuDevice): Result<Unit> {
        return sendKeyPress(device, "Rev")
    }

    /**
     * Verifica se o dispositivo está acessível.
     */
    suspend fun isDeviceReachable(device: RokuDevice): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://${device.ipAddress}:${device.port}/query/device-info"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }
}