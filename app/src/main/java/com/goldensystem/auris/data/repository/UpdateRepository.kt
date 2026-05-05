package com.goldensystem.auris.data.repository

import com.goldensystem.auris.data.model.AppVersionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val SECURITY_TOKEN = "AURIS_UPDATE_9X7K2P"
    }

    suspend fun fetchAppVersion(sheetUrl: String): Result<AppVersionInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val client = okHttpClient.newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder().url(sheetUrl).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Erro HTTP: ${response.code}"))
                }

                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Resposta vazia"))

                val lineRegex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()

                val aurisLine = body.lines()
                    .drop(1)
                    .firstOrNull { line ->
                        line.split(lineRegex).firstOrNull()?.trim()?.equals("Auris", ignoreCase = true) == true
                    }
                    ?: return@withContext Result.failure(Exception("App Auris não encontrado"))

                val parts = aurisLine.split(lineRegex).map { it.trim().removeSurrounding("\"") }
                if (parts.size < 5) return@withContext Result.failure(Exception("Formato inválido"))

                if (parts.getOrElse(5) { "" } != SECURITY_TOKEN) {
                    return@withContext Result.failure(Exception("Token inválido"))
                }

                val downloadUrl = parts[3]
                if (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://")) {
                    return@withContext Result.failure(Exception("URL inválida"))
                }

                Result.success(
                    AppVersionInfo(
                        appName = parts[0],
                        version = parts[1],
                        id = parts[2],
                        downloadUrl = downloadUrl,
                        isRequired = parts.getOrElse(4) { "false" }.toBoolean(),
                        changelog = parts.getOrNull(6)
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}