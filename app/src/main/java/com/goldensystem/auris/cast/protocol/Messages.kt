package com.goldensystem.auris.cast.protocol

import kotlinx.serialization.Serializable

@Serializable
data class PlayMessage(
    val type: String = "play",
    val trackId: String,
    val url: String,
    val title: String,
    val artist: String,
    val album: String,
    val cover: String,
    val lyrics: String,
    val mime: String,
    val duration: Long
)

@Serializable
data class SeekMessage(
    val type: String = "seek",
    val position: Long
)

@Serializable
data class StateMessage(
    val type: String = "state",
    val position: Long,
    val playing: Boolean,
    val buffering: Boolean,
    val volume: Int = 100
)

@Serializable
data class StopMessage(
    val type: String = "stop"
)
