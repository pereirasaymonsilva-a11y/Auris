package com.goldensystem.auris.cast.session

import com.goldensystem.auris.cast.protocol.PlayMessage
import com.goldensystem.auris.cast.protocol.StateMessage
import com.goldensystem.auris.data.model.Song
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.CopyOnWriteArrayList

data class PlaybackState(
    val position: Long = 0L,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val volume: Int = 100
)

data class CastSession(
    val sessionId: String,
    val rokuIp: String,
    val currentTrack: MutableStateFlow<Song?> = MutableStateFlow(null),
    val playbackState: MutableStateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
) {
    val state: StateFlow<PlaybackState> = playbackState.asStateFlow()
    val websocketClients = CopyOnWriteArrayList<WebSocketSession>()
    val sessionToken: String = generateSessionToken()

    fun updateState(stateMessage: StateMessage) {
        playbackState.value = PlaybackState(
            position = stateMessage.position,
            playing = stateMessage.playing,
            buffering = stateMessage.buffering,
            volume = stateMessage.volume
        )
    }

    suspend inline fun <reified T> sendMessage(message: T) {
        val frame = Frame.Text(Json.encodeToString(message))
        val dead = mutableListOf<WebSocketSession>()
        websocketClients.forEach { client ->
            try {
                client.send(frame)
            } catch (_: Exception) {
                dead.add(client)
            }
        }
        if (dead.isNotEmpty()) websocketClients.removeAll(dead)
    }

    suspend fun sendPlayMessage(msg: PlayMessage) = sendMessage(msg)

    private fun generateSessionToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return buildString(32) { repeat(32) { append(chars.random()) } }
    }
}
