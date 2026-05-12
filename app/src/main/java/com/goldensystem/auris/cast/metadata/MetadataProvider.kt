package com.goldensystem.auris.cast.metadata

import com.goldensystem.auris.data.model.Song
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataProvider @Inject constructor() {
    fun extractMeta(song: Song, baseUrl: String): Map<String, String> {
        return mapOf(
            "trackId" to song.id,
            "title" to song.title,
            "artist" to song.artist,
            "album" to song.album,
            "cover" to "$baseUrl/cover?id=${song.id}",
            "lyrics" to "$baseUrl/lyrics?id=${song.id}",
            "duration" to song.duration.toString(),
            "mime" to (song.mimeType ?: "audio/mpeg")
        )
    }
}
