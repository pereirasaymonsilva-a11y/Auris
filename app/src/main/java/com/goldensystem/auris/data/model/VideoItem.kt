package com.goldensystem.auris.data.model

data class VideoItem(
    val id: Long,
    val title: String,
    val contentUri: String,      // content:// URI do vídeo no MediaStore
    val duration: Long           // duração em milissegundos
)