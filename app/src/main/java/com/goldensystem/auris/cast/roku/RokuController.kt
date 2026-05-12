package com.goldensystem.auris.cast.roku

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RokuController @Inject constructor() {
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

    fun play(rokuIp: String, audioUrl: String) {
        val encoded = URLEncoder.encode(audioUrl, "UTF-8")
        val url = "http://$rokuIp:8060/input/15985?t=p&u=$encoded"

        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody())
            .build()

        client.newCall(request).execute().use { }
    }

    fun playPause(rokuIp: String) {
        keypress(rokuIp, "Play")
    }

    fun home(rokuIp: String) {
        keypress(rokuIp, "Home")
    }

    private fun keypress(rokuIp: String, key: String) {
        val request = Request.Builder()
            .url("http://$rokuIp:8060/keypress/$key")
            .post("".toRequestBody())
            .build()

        client.newCall(request).execute().use { }
    }
}
