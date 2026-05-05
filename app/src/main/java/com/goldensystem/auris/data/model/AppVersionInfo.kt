package com.goldensystem.auris.data.model

data class AppVersionInfo(
    val appName: String,
    val version: String,
    val id: String,
    val downloadUrl: String,
    val isRequired: Boolean = false,
    val changelog: String? = null
)