package com.goldensystem.auris.data.repository

import com.goldensystem.auris.data.database.MusicDao
import com.goldensystem.auris.data.model.Song
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
    // ⚠️ SUBSTITUA PELA URL DO SEU APPS SCRIPT
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
            val songs = mutableListOf<Song>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = "auris_" + obj.optString("id", i.toString())
                songs.add(
                    Song(
                        id = id,
                        title = obj.optString("title", "Sem título"),
                        artist = obj.optString("artist", "Desconhecido"),
                        album = obj.optString("album", ""),
                        albumId = 0,
                        artistId = 0,
                        duration = obj.optLong("duration", 0),
                        dateAdded = System.currentTimeMillis(),
                        data = obj.optString("mp3Url"),
                        size = 0,
                        albumArtUriString = obj.optString("coverUrl", null),
                        contentUriString = obj.optString("mp3Url")
                    )
                )
            }

            // Remove músicas antigas do Auris Online e insere as novas
            musicDao.deleteAurisOnlineSongs()
            musicDao.insertSongs(songs)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}