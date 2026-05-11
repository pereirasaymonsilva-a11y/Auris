package com.goldensystem.auris.cast.roku

import fi.iki.elonen.NanoHTTPD
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAudioServer @Inject constructor() : NanoHTTPD(8080) {

    private var audioFile: File? = null

    fun setFile(file: File) {
        audioFile = file
    }

    override fun serve(session: IHTTPSession): Response {
        val file = audioFile
        if (file == null || !file.exists() || !file.canRead()) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                "text/plain",
                "Arquivo não encontrado"
            )
        }

        return newChunkedResponse(
            Response.Status.OK,
            "audio/mpeg",
            file.inputStream()
        ).apply {
            addHeader("Accept-Ranges", "bytes")
        }
    }
}