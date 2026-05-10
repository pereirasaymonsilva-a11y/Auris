package com.goldensystem.auris.data.service.roku

import android.util.Log
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RokuHttpServer @Inject constructor() {
    companion object {
        private const val TAG = "RokuHttpServer"
        private const val THREAD_POOL_SIZE = 4
    }

    private var server: HttpServer? = null
    private var currentFile: File? = null

    /**
     * Inicia o servidor HTTP em uma porta disponível e prepara o arquivo a ser servido.
     * @param audioFile O arquivo de áudio a ser transmitido.
     * @return O endereço completo (URL) que o Roku deve usar para acessar o fluxo, ou null se falhar.
     */
    fun start(audioFile: File): String? {
        stop() // Para qualquer servidor anterior

        return try {
            val port = findAvailablePort()
            val address = InetSocketAddress(port)
            server = HttpServer.create(address, 0).apply {
                createContext("/stream") { exchange ->
                    try {
                        val file = currentFile
                        if (file == null || !file.exists()) {
                            val response = "Arquivo não encontrado".toByteArray()
                            exchange.sendResponseHeaders(404, response.size.toLong())
                            exchange.responseBody.use { it.write(response) }
                            return@createContext
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

                        exchange.responseHeaders.set("Content-Type", mimeType)
                        exchange.responseHeaders.set("Accept-Ranges", "bytes")
                        exchange.sendResponseHeaders(200, file.length())

                        FileInputStream(file).use { input ->
                            exchange.responseBody.use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao servir arquivo", e)
                        try {
                            exchange.sendResponseHeaders(500, -1)
                        } catch (_: Exception) {}
                    }
                }
                executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE)
                start()
            }
            currentFile = audioFile
            val streamUrl = "http://${getLocalIpAddress()}:$port/stream"
            Log.d(TAG, "Servidor iniciado em $streamUrl")
            streamUrl
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao iniciar servidor HTTP", e)
            null
        }
    }

    /**
     * Para o servidor HTTP.
     */
    fun stop() {
        try {
            server?.stop(0)
            server = null
            currentFile = null
            Log.d(TAG, "Servidor parado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao parar servidor", e)
        }
    }

    /**
     * Encontra uma porta disponível no dispositivo.
     */
    private fun findAvailablePort(): Int {
        return try {
            val socket = java.net.ServerSocket(0)
            val port = socket.localPort
            socket.close()
            port
        } catch (e: Exception) {
            9876 // Porta fallback
        }
    }

    /**
     * Obtém o endereço IP local do dispositivo na rede Wi-Fi.
     */
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
            Log.e(TAG, "Erro ao obter IP", e)
            "127.0.0.1"
        }
    }
}