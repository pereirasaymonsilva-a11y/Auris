package com.goldensystem.auris.utils

fun isNewerVersion(remote: String, local: String): Boolean {
    // Remove sufixos como -beta, -alpha, -rc, etc.
    val remoteClean = remote.replace(Regex("-[a-zA-Z]+.*"), "")
    val localClean = local.replace(Regex("-[a-zA-Z]+.*"), "")

    val remoteParts = remoteClean.split(".").map { it.toIntOrNull() ?: 0 }
    val localParts = localClean.split(".").map { it.toIntOrNull() ?: 0 }

    for (i in 0 until maxOf(remoteParts.size, localParts.size)) {
        val r = remoteParts.getOrElse(i) { 0 }
        val l = localParts.getOrElse(i) { 0 }

        if (r > l) return true
        if (r < l) return false
    }

    // Se os números forem iguais, verifica os sufixos:
    // Uma versão SEM sufixo é considerada mais nova que uma COM sufixo.
    val remoteHasSuffix = remote.contains(Regex("-[a-zA-Z]"))
    val localHasSuffix = local.contains(Regex("-[a-zA-Z]"))

    return !remoteHasSuffix && localHasSuffix
}