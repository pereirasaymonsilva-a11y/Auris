package com.goldensystem.auris.cast.roku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object RokuDiscovery {

    suspend fun discover(): List<RokuDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<RokuDevice>()

        val search = """
            M-SEARCH * HTTP/1.1
            HOST: 239.255.255.250:1900
            MAN: "ssdp:discover"
            ST: roku:ecp
            MX: 3
        """.trimIndent()

        val socket = DatagramSocket()
        socket.soTimeout = 4000

        val packet = DatagramPacket(
            search.toByteArray(),
            search.length,
            InetAddress.getByName("239.255.255.250"),
            1900
        )

        socket.send(packet)

        val buffer = ByteArray(4096)

        try {
            while (true) {
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)

                val ip = response.address.hostAddress
                if (devices.none { it.ip == ip }) {
                    devices.add(
                        RokuDevice(
                            ip = ip,
                            name = "Roku ($ip)"
                        )
                    )
                }
            }
        } catch (_: Exception) {
        } finally {
            socket.close()
        }

        devices
    }
}
