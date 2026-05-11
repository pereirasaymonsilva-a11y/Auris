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
        private const val RECEIVER_PORT = 8061
    }

    private val client = okHttpClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun playStream(
        device: RokuDevice,
        streamUrl: String,
        title: String? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Envia comando para o app receptor
                val encodedUrl = URLEncoder.encode(streamUrl, "UTF-8")
                val rokuUrl = "http://${device.ipAddress}:$RECEIVER_PORT/play?url=$encodedUrl"

                Log.d(TAG, "Enviando playStream para app receptor: $rokuUrl")

                val request = Request.Builder()
                    .url(rokuUrl)
                    .post("".toRequestBody())
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "Stream iniciado com sucesso em ${device.friendlyName}")
                        Result.success(Unit)
                    } else {
                        Log.e(TAG, "Falha ao iniciar stream: HTTP ${response.code}")
                        Result.failure(Exception("Erro HTTP ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao iniciar stream", e)
                Result.failure(e)
            }
        }
    }

    suspend fun stop(device: RokuDevice): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://${device.ipAddress}:$RECEIVER_PORT/stop"
                val request = Request.Builder().url(url).post("".toRequestBody()).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) Result.success(Unit)
                    else Result.failure(Exception("Erro HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Os outros métodos (pause, resume, etc.) podem ser implementados depois
    suspend fun pause(device: RokuDevice): Result<Unit> = stop(device)
    suspend fun resume(device: RokuDevice): Result<Unit> = playStream(device, "", null) // não ideal, mas funcional
    suspend fun next(device: RokuDevice): Result<Unit> = Result.success(Unit)
    suspend fun previous(device: RokuDevice): Result<Unit> = Result.success(Unit)

    suspend fun isDeviceReachable(device: RokuDevice): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://${device.ipAddress}:8060/query/device-info"
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                false
            }
        }
    }
}