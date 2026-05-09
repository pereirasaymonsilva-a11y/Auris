package com.goldensystem.auris.data.repository

import android.util.Log
import com.goldensystem.auris.data.database.MusicDao
import com.goldensystem.auris.data.database.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AurisOnlineRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val musicDao: MusicDao
) {
    // ⚠️ SUBSTITUA PELA SUA URL REAL
    private val scriptUrl = "https://script.google.com/macros/s/AKfycbyHt5qIgRp_Nw2gUog5eKxFJ6BVXYK9_ie1xrn3GsHMVi3tuyzMgQu8q2bfjLvau9OW6g/exec"

    suspend fun syncSongs(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = okHttpClient.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(scriptUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Erro HTTP ${response.code}")
            val body = response.body?.string() ?: throw Exception("Resposta vazia")
            val array = JSONArray(body)
            Log.d("AurisOnline", "Recebidos ${array.length()} itens da planilha")
            val entities = mutableListOf<SongEntity>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                // Gera um ID único negativo para não conflitar com os locais (positivos)
                val id = -(i + 1).toLong() * 1000000L + obj.optString("id", i.toString()).hashCode().toLong()
                entities.add(
                    SongEntity(
                        id = id,
                        title = obj.optString("title", "Sem título"),
                        artistName = obj.optString("artist", "Desconhecido"),
                        albumName = obj.optString("album", ""),
                        albumId = 0L,
                        artistId = 0L,
                        contentUriString = obj.optString("mp3Url", ""),
                        albumArtUriString = obj.optString("coverUrl", null),
                        duration = obj.optLong("duration", 0L),
                        genre = null,
                        filePath = obj.optString("mp3Url", ""),
                        parentDirectoryPath = "",
                        isFavorite = false,
                        lyrics = null,
                        trackNumber = 0,
                        discNumber = null,
                        year = 0,
                        dateAdded = System.currentTimeMillis(),
                        mimeType = "audio/mpeg",
                        bitrate = null,
                        sampleRate = null,
                        sourceType = 7  // Auris Online
                    )
                )
            }

            musicDao.deleteAurisOnlineSongs()
            musicDao.insertSongs(entities)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AurisOnline", "Falha na sincronização", e)
            Result.failure(e)
        }
    }
}