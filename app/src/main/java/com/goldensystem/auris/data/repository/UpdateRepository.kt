package com.goldensystem.auris.data.repository

import android.util.Log
import com.goldensystem.auris.data.model.AppVersionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "UpdateRepository"
        // Token de segurança esperado no JSON (campo opcional, mas recomendado)
        private const val SECURITY_TOKEN = "AURIS_UPDATE_9X7K2P"
    }

    suspend fun fetchAppVersion(scriptUrl: String): Result<AppVersionInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val client = okHttpClient.newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder().url(scriptUrl).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Erro HTTP: ${response.code}")
                    return@withContext Result.failure(Exception("Erro HTTP: ${response.code}"))
                }

                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Resposta vazia"))

                Log.d(TAG, "JSON recebido: ${body.take(200)}...")

                // Parse do JSON array
                val jsonArray = JSONArray(body)
                var aurisInfo: AppVersionInfo? = null

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val appName = obj.optString("appName", "")

                    if (appName.equals("Auris", ignoreCase = true)) {
                        // Validação opcional do token de segurança
                        val token = obj.optString("token", "")
                        if (token.isNotEmpty() && token != SECURITY_TOKEN) {
                            Log.e(TAG, "Token de segurança inválido")
                            return@withContext Result.failure(Exception("Token de segurança inválido"))
                        }

                        val downloadUrl = obj.optString("downloadUrl", "")
                        if (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://")) {
                            Log.e(TAG, "URL de download inválida: $downloadUrl")
                            return@withContext Result.failure(Exception("URL de download inválida"))
                        }

                        // 🔥 NOVO: extrai o campo originalPackage
                        val originalPackage = obj.optString("originalPackage", null)

                        aurisInfo = AppVersionInfo(
                            appName = appName,
                            version = obj.optString("version", "0.0.0"),
                            id = obj.optString("id", ""),
                            downloadUrl = downloadUrl,
                            isRequired = obj.optBoolean("isRequired", false),
                            changelog = obj.optString("changelog", null),
                            originalPackage = originalPackage  // ← adicionado
                        )
                        break
                    }
                }

                if (aurisInfo != null) {
                    Log.d(TAG, "Auris encontrado: versão ${aurisInfo.version}")
                    Result.success(aurisInfo)
                } else {
                    Log.e(TAG, "App Auris não encontrado no JSON")
                    Result.failure(Exception("App Auris não encontrado no JSON"))
                }
            } catch (e: org.json.JSONException) {
                Log.e(TAG, "Erro ao fazer parse do JSON", e)
                Result.failure(Exception("Formato JSON inválido"))
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao buscar atualização", e)
                Result.failure(e)
            }
        }
    }
}