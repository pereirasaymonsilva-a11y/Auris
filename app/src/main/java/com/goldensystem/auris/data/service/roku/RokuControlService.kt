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
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Erro HTTP ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun playStream(
        device: RokuDevice,
        streamUrl: String,
        title: String? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val encodedUrl = URLEncoder.encode(streamUrl, "UTF-8")
            val encodedTitle = URLEncoder.encode(title ?: "", "UTF-8")

            // Lista de variações de parâmetros para tentar
            val attempts = listOf(
                "t=a&u=$encodedUrl&videoName=$encodedTitle",
                "t=a&url=$encodedUrl&videoName=$encodedTitle",
                "t=a&u=$encodedUrl",
                "t=a&url=$encodedUrl",
                "t=a&songUrl=$encodedUrl"
            )

            for (params in attempts) {
                val rokuUrl = "http://${device.ipAddress}:${device.port}/input/15985?$params"
                Log.d(TAG, "Tentando playStream: $rokuUrl")

                val request = Request.Builder()
                    .url(rokuUrl)
                    .post("".toRequestBody())
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            Log.d(TAG, "Sucesso com parâmetros: $params")
                            return@withContext Result.success(Unit)
                        } else {
                            Log.w(TAG, "Falha (${response.code}) para: $params")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Exceção para $params: ${e.message}")
                }
            }

            Result.failure(Exception("Todas as tentativas de playStream falharam (404/outros). Verifique se o canal Roku Media Player está instalado."))
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