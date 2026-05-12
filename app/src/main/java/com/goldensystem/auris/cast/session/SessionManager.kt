package com.goldensystem.auris.cast.session

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private var activeSession: CastSession? = null

    fun createSession(rokuIp: String): CastSession {
        val session = CastSession(
            sessionId = generateSessionId(),
            rokuIp = rokuIp
        )
        activeSession = session
        return session
    }

    fun getActiveSession(): CastSession? = activeSession

    fun endSession() {
        activeSession = null
    }

    private fun generateSessionId(): String {
        return System.currentTimeMillis().toString(36) + (Math.random() * 1000).toInt().toString(36)
    }
}
