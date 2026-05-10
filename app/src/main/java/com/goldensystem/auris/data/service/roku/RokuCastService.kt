package com.goldensystem.auris.data.service.roku

import android.content.Intent
import androidx.mediarouter.media.MediaRouteProvider
import androidx.mediarouter.media.MediaRouteProviderService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class RokuCastService : MediaRouteProviderService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RokuCastServiceEntryPoint {
        fun discoveryService(): RokuDiscoveryService
    }

    private lateinit var provider: RokuMediaRouteProvider

    override fun onCreate() {
        super.onCreate()
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            RokuCastServiceEntryPoint::class.java
        )
        val discoveryService = entryPoint.discoveryService()
        provider = RokuMediaRouteProvider(applicationContext, discoveryService)
    }

    override fun onBind(intent: Intent?) = super.onBind(intent)

    override fun onGetMediaRouteProvider(): MediaRouteProvider = provider

    override fun onUnbind(intent: Intent?): Boolean {
        provider.stopDiscovery()
        return super.onUnbind(intent)
    }
}