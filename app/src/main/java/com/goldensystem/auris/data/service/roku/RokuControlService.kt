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

            // Tenta primeiro com o parâmetro "u" (mais comum em firmwares recentes)
            var result = tryPlayStream(device, encodedUrl, encodedTitle, useUrlParam = false)
            if (result.isSuccess) {
                return@withContext result
            }
            Log.w(TAG, "Falha com parâmetro 'u', tentando com 'url'...")

            // Se falhar, tenta com o parâmetro "url" (firmwares mais antigos)
            result = tryPlayStream(device, encodedUrl, encodedTitle, useUrlParam = true)
            if (result.isSuccess) {
                return@withContext result
            }
            Log.e(TAG, "Ambas as tentativas de playStream falharam")
            result // Retorna o último erro
        }
    }

    private suspend fun tryPlayStream(
        device: RokuDevice,
        encodedUrl: String,
        encodedTitle: String,
        useUrlParam: Boolean
    ): Result<Unit> {
        val paramName = if (useUrlParam) "url" else "u"
        val rokuUrl = "http://${device.ipAddress}:${device.port}/input/15985?" +
                "t=a" +
                "&$paramName=$encodedUrl" +
                "&videoName=$encodedTitle"

        Log.d(TAG, "Tentando playStream com '$paramName': $rokuUrl")

        val request = Request.Builder()
            .url(rokuUrl)
            .post("".toRequestBody())
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Stream iniciado com sucesso em ${device.friendlyName} (usando '$paramName')")
                    Result.success(Unit)
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Falha ao iniciar stream com '$paramName': HTTP ${response.code} body=$errorBody")
                    Result.failure(Exception("Erro HTTP ${response.code}: $errorBody"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar stream com '$paramName'", e)
            Result.failure(e)
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