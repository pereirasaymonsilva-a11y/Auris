package com.goldensystem.auris.data.service.roku

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RokuHttpServer @Inject constructor(
    @ApplicationContext private val context: Context
) {
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

            val ip = getLocalIpAddress()
            val streamUrl = "http://$ip:$port/stream"
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

            val reader = input.bufferedReader()
            val requestLine = reader.readLine() ?: return
            Log.d(TAG, "Requisição recebida: $requestLine")

            if (requestLine.startsWith("GET")) {
                val file = currentFile
                if (file == null || !file.exists() || !file.canRead()) {
                    Log.e(TAG, "Arquivo não encontrado ou sem permissão: ${file?.absolutePath}")
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
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao obter IP via WifiManager", e)
        }
        // Fallback: percorre interfaces de rede
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao varrer interfaces", e)
        }
        return "127.0.0.1"
    }
}