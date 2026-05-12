package com.goldensystem.auris.cast.roku

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

class RokuController(
    private val rokuIp: String
) {
    private val client = OkHttpClient()

    fun launchReceiver(rokuIp: String, wsUrl: String) {
        val encodedWs = URLEncoder.encode(wsUrl, "UTF-8")
        val url = "http://$rokuIp:8060/launch/dev?ws=$encodedWs"

        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody())
            .build()

        client.newCall(request).execute().use { }
    }

    fun play(audioUrl: String) {
        val encoded = URLEncoder.encode(audioUrl, "UTF-8")
        val url = "http://$rokuIp:8060/input/15985?t=p&u=$encoded"

        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody())
            .build()

        client.newCall(request).execute().use { }
    }

    fun playPause() {
        keypress("Play")
    }

    fun home() {
        keypress("Home")
    }

    private fun keypress(key: String) {
        val request = Request.Builder()
            .url("http://$rokuIp:8060/keypress/$key")
            .post("".toRequestBody())
            .build()

        client.newCall(request).execute().use { }
    }
}
