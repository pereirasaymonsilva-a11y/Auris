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

    suspend fun playStream(
        device: RokuDevice,
        streamUrl: String,
        title: String? = null,
        format: String = "mp3"
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Converte a URL para o formato que o Roku entende
                // Exemplo: http://192.168.1.100:1234/stream
                val encodedUrl = URLEncoder.encode(streamUrl, "UTF-8")
                val encodedTitle = if (title != null) URLEncoder.encode(title, "UTF-8") else ""

                // Comando correto: /input/15985 com parâmetros
                val rokuUrl = "http://${device.ipAddress}:${device.port}/input/15985?" +
                        "t=v&u=$encodedUrl&videoName=$encodedTitle"

                Log.d(TAG, "Enviando comando playStream: $rokuUrl")

                val request = Request.Builder()
                    .url(rokuUrl)
                    .post("".toRequestBody())
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "Stream iniciado com sucesso em ${device.friendlyName}")
                    Result.success(Unit)
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Falha ao iniciar stream: HTTP ${response.code} body=$errorBody")
                    Result.failure(Exception("Erro HTTP ${response.code}: $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao iniciar stream", e)
                Result.failure(e)
            }
        }
    }

    suspend fun stop(device: RokuDevice): Result<Unit> {
        return sendKeyPress(device, "Home")
    }

    suspend fun pause(device: RokuDevice): Result<Unit> {
        return sendKeyPress(device, "Play")
    }

    suspend fun resume(device: RokuDevice): Result<Unit> {
        return sendKeyPress(device, "Play")
    }

    suspend fun next(device: RokuDevice): Result<Unit> {
        return sendKeyPress(device, "Fwd")
    }

    suspend fun previous(device: RokuDevice): Result<Unit> {
        return sendKeyPress(device, "Rev")
    }

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