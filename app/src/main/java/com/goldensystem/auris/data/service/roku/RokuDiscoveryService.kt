package com.goldensystem.auris.data.service.roku

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

data class RokuDevice(
    val serialNumber: String,
    val friendlyName: String,
    val ipAddress: String,
    val port: Int = 8060
)

@Singleton
class RokuDiscoveryService @Inject constructor() {
    companion object {
        private const val TAG = "RokuDiscovery"
        private const val SSDP_MULTICAST_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val SEARCH_HEADER =
            "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: 239.255.255.250:1900\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "MT: roku:ecp\r\n" +
                "ST: roku:ecp\r\n" +
                "\r\n"
        private const val SCAN_INTERVAL_MS = 30_000L
    }

    private val _devices = MutableStateFlow<List<RokuDevice>>(emptyList())
    val devices: StateFlow<List<RokuDevice>> = _devices.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var scanJob: Job? = null
    private val discoveredDevices = mutableListOf<RokuDevice>()

    fun startScanning() {
        scanJob?.cancel()
        scanJob = scope.launch {
            while (true) {
                scanForRokus()
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    fun stopScanning() {
        scanJob?.cancel()
    }

    private suspend fun scanForRokus() {
        try {
            val socket = DatagramSocket(null)
            socket.reuseAddress = true
            socket.broadcast = true
            val timeout = 5000
            socket.soTimeout = timeout
            val address = InetSocketAddress(SSDP_MULTICAST_ADDRESS, SSDP_PORT)
            val requestData = SEARCH_HEADER.toByteArray(StandardCharsets.UTF_8)
            val packet = DatagramPacket(requestData, requestData.size, address)
            socket.send(packet)

            val buffer = ByteArray(4096)
            val response = DatagramPacket(buffer, buffer.size)
            var newDevices = false

            while (true) {
                try {
                    socket.receive(response)
                    val responseText = String(response.data, 0, response.length)
                    if (responseText.contains("roku")) {
                        val location = extractHeader(responseText, "LOCATION:")
                        if (location != null) {
                            val ip = extractIpFromLocation(location)
                            if (ip != null && discoveredDevices.none { it.ipAddress == ip }) {
                                // Busca detalhes do dispositivo
                                val device = fetchDeviceDetails(ip)
                                if (device != null) {
                                    discoveredDevices.add(device)
                                    newDevices = true
                                    Log.d(TAG, "Novo Roku encontrado: $device")
                                }
                            }
                        }
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao receber resposta SSDP", e)
                    break
                }
            }
            socket.close()
            if (newDevices) {
                _devices.value = discoveredDevices.toList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no scan SSDP", e)
        }
    }

    private suspend fun fetchDeviceDetails(ip: String): RokuDevice? {
        return with(Dispatchers.IO) {
            try {
                val url = java.net.URL("http://$ip:8060/query/device-info")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val serial = extractXmlTag(body, "serial-number")
                    val name = extractXmlTag(body, "friendly-device-name")
                        ?: extractXmlTag(body, "user-device-name")
                        ?: "Roku ($ip)"
                    RokuDevice(
                        serialNumber = serial ?: ip,
                        friendlyName = name,
                        ipAddress = ip,
                        port = 8060
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao obter detalhes do Roku em $ip", e)
                null
            }
        }
    }

    private fun extractHeader(response: String, headerName: String): String? {
        val lines = response.split("\r\n")
        for (line in lines) {
            if (line.startsWith(headerName, ignoreCase = true)) {
                return line.substring(headerName.length).trim()
            }
        }
        return null
    }

    private fun extractIpFromLocation(location: String): String? {
        return try {
            val url = java.net.URL(location)
            url.host
        } catch (e: Exception) {
            null
        }
    }

    private fun extractXmlTag(xml: String, tagName: String): String? {
        val regex = Regex("<$tagName>(.*?)</$tagName>", RegexOption.IGNORE_CASE)
        return regex.find(xml)?.groupValues?.get(1)
    }
}