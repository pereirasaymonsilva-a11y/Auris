package com.goldensystem.auris.data.service.roku

import android.content.Context
import android.content.IntentFilter
import android.util.Log
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteDescriptor
import androidx.mediarouter.media.MediaRouteDiscoveryRequest
import androidx.mediarouter.media.MediaRouteProvider
import androidx.mediarouter.media.MediaRouteProviderDescriptor
import androidx.mediarouter.media.MediaRouter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RokuMediaRouteProvider(context: Context) : MediaRouteProvider(context) {

    companion object {
        private const val TAG = "RokuMediaRouteProvider"
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RokuMediaRouteProviderEntryPoint {
        fun discoveryService(): RokuDiscoveryService
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var discoveryJob: Job? = null

    // Inicialização preguiçosa para evitar problemas de ordem no construtor
    private val discoveryService: RokuDiscoveryService by lazy {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            RokuMediaRouteProviderEntryPoint::class.java
        )
        entryPoint.discoveryService()
    }

    init {
        // Aguarda um pequeno ciclo para que o objeto esteja completamente construído
        scope.launch {
            startDiscovery()
        }
    }

    override fun onDiscoveryRequestChanged(request: MediaRouteDiscoveryRequest?) {
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