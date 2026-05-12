package com.goldensystem.auris.cast.server

import com.goldensystem.auris.cast.protocol.StateMessage
import com.goldensystem.auris.cast.session.SessionManager
import io.ktor.server.routing.Routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

fun Routing.configureWebSockets(sessionManager: SessionManager) {
    webSocket("/ws") {
        val sessionToken = call.request.queryParameters["token"]
        val session = sessionManager.getActiveSession()

        if (session == null || session.sessionToken != sessionToken) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
            return@webSocket
        }

        session.websocketClients.add(this)

        try {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    if (text.contains("\"type\":\"state\"")) {
                        val stateMsg = Json.decodeFromString<StateMessage>(text)
                        session.updateState(stateMsg)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            session.websocketClients.remove(this)
        }
    }
}
