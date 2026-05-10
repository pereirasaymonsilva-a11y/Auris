package com.goldensystem.auris.data.service.roku

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RokuHttpServer @Inject constructor() {
    companion object {
        private const val TAG = "RokuHttpServer"
        private const val THREAD_POOL_SIZE = 4
    }

    private var serverSocket: ServerSocket? = null
    private var currentFile: File? = null
    private var isRunning = false

    fun start(audioFile: File): String? {
        stop()
        return try {
            val port = findAvailablePort()
            serverSocket = ServerSocket(port)
            isRunning = true
            currentFile = audioFile

            val executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE)
            executor.execute {
                while (isRunning) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        executor.execute { handleClient(client) }
                    } catch (e: Exception) {
                        if (isRunning) Log.e(TAG, "Erro ao aceitar cliente", e)
                        break
                    }
                }
            }

            val streamUrl = "http://${getLocalIpAddress()}:$port/stream"
            Log.d(TAG, "Servidor HTTP iniciado em $streamUrl")
            streamUrl
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao iniciar servidor", e)
            null
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao parar servidor", e)
        }
        serverSocket = null
        currentFile = null
    }

    private fun handleClient(socket: Socket) {
        try {
            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            // Lê a primeira linha (ex: GET /stream HTTP/1.1)
            val reader = input.bufferedReader()
            val requestLine = reader.readLine() ?: return

            if (requestLine.startsWith("GET")) {
                val file = currentFile
                if (file == null || !file.exists()) {
                    val response = "HTTP/1.1 404 Not Found\r\n\r\n".toByteArray()
                    output.write(response)
                    output.flush()
                    return
                }

                val mimeType = when (file.extension.lowercase()) {
                    "mp3" -> "audio/mpeg"
                    "m4a" -> "audio/mp4"
                    "aac" -> "audio/aac"
                    "wav" -> "audio/wav"
                    "flac" -> "audio/flac"
                    "ogg" -> "audio/ogg"
                    else -> "audio/mpeg"
                }

                val fileLength = file.length()
                val headers = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: $mimeType\r\n" +
                        "Content-Length: $fileLength\r\n" +
                        "Accept-Ranges: bytes\r\n" +
                        "Connection: close\r\n\r\n"

                output.write(headers.toByteArray())
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(output)
                }
                output.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar requisição", e)
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun findAvailablePort(): Int {
        return try {
            ServerSocket(0).use { it.localPort }
        } catch (e: Exception) {
            9876
        }
    }

    private fun getLocalIpAddress(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is java.net.Inet4Address && !address.isLoopbackAddress) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
            "127.0.0.1"
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }
}