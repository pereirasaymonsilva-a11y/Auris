package com.goldensystem.auris.data.repository

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
    // ⚠️ COLOQUE SUA URL REAL AQUI
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
            val entities = mutableListOf<SongEntity>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                entities.add(
                    SongEntity(
                        id = ("auris_" + obj.optString("id", i.toString())).toLong(),
                        title = obj.optString("title", "Sem título"),
                        artistName = obj.optString("artist", "Desconhecido"),
                        albumName = obj.optString("album", ""),
                        albumId = 0L,
                        artistId = 0L,
                        duration = obj.optLong("duration", 0L),
                        dateAdded = System.currentTimeMillis(),
                        filePath = obj.optString("mp3Url", ""),
                        mimeType = "audio/mpeg",
                        bitrate = 0,
                        sampleRate = 0,
                        size = 0L,
                        dateModified = System.currentTimeMillis(),
                        albumArtUriString = obj.optString("coverUrl", null),
                        contentUriString = obj.optString("mp3Url", ""),
                        genre = null,
                        trackNumber = 0,
                        discNumber = 0,
                        year = 0,
                        lyrics = null,
                        isFavorite = false,
                        telegramChatId = null,
                        telegramFileId = null,
                        artistsJson = null,
                        sourceType = 0,
                        playCount = 0
                    )
                )
            }

            musicDao.deleteAurisOnlineSongs()
            musicDao.insertSongs(entities)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}