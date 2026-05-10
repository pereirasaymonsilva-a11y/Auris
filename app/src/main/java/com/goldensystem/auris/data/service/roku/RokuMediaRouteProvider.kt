package com.goldensystem.auris.data.service.roku

import android.content.Context
import android.media.MediaRouter
import android.util.Log
import androidx.mediarouter.media.MediaRouteDescriptor
import androidx.mediarouter.media.MediaRouteProvider
import androidx.mediarouter.media.MediaRouteProviderDescriptor
import androidx.mediarouter.media.MediaRouter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RokuMediaRouteProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val discoveryService: RokuDiscoveryService
) : MediaRouteProvider(context) {

    companion object {
        private const val TAG = "RokuMediaRouteProvider"
        private const val CATEGORY = MediaRouter.Category.ROUTE_TYPE_REMOTE_AUDIO_VIDEO.toString()
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var discoveryJob: Job? = null

    init {
        startDiscovery()
    }

    override fun onDiscoveryRequestChanged(request: DiscoveryRequest?) {
        // Podemos usar a solicitação de descoberta para filtrar, mas por enquanto vamos publicar todos os Rokus
        publishRoutes()
        if (request?.isActive == true) {
            discoveryService.startScanning()
        } else {
            discoveryService.stopScanning()
        }
    }

    private fun startDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            discoveryService.devices.collect { devices ->
                publishRoutes()
            }
        }
        discoveryService.startScanning()
    }

    private fun publishRoutes() {
        val devices = discoveryService.devices.value
        val routeDescriptors = devices.map { device ->
            MediaRouteDescriptor.Builder(
                "roku_${device.serialNumber}",
                device.friendlyName
            )
                .setDescription("Roku")
                .addControlCategory(CATEGORY)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED)
                .setCanDisconnect(true)
                .build()
        }
        val providerDescriptor = MediaRouteProviderDescriptor.Builder()
            .addRoutes(routeDescriptors)
            .build()
        descriptor = providerDescriptor
        Log.d(TAG, "Rotas publicadas: ${devices.size} dispositivos")
    }

    fun stopDiscovery() {
        discoveryService.stopScanning()
        discoveryJob?.cancel()
    }
}