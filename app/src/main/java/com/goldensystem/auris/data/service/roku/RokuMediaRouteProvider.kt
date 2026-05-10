package com.goldensystem.auris.data.service.roku

import android.content.Context
import android.content.IntentFilter
import android.util.Log
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteDescriptor
import androidx.mediarouter.media.MediaRouteProvider
import androidx.mediarouter.media.MediaRouteProviderDescriptor
import androidx.mediarouter.media.MediaRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RokuMediaRouteProvider(
    private val context: Context,
    private val discoveryService: RokuDiscoveryService
) : MediaRouteProvider(context) {

    companion object {
        private const val TAG = "RokuMediaRouteProvider"
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var discoveryJob: Job? = null

    init {
        startDiscovery()
    }

    override fun onDiscoveryRequestChanged(request: androidx.mediarouter.media.MediaRouteDiscoveryRequest?) {
        if (request != null) {
            discoveryService.startScanning()
        } else {
            discoveryService.stopScanning()
        }
        publishRoutes()
    }

    private fun startDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            discoveryService.devices.collect {
                publishRoutes()
            }
        }
        discoveryService.startScanning()
    }

    private fun publishRoutes() {
        val devices = discoveryService.devices.value
        val routeDescriptors = devices.map { device ->
            val controlFilter = IntentFilter().apply {
                addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
            }
            MediaRouteDescriptor.Builder(
                "roku_${device.serialNumber}",
                device.friendlyName
            )
                .setDescription("Roku")
                .addControlFilter(controlFilter)
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