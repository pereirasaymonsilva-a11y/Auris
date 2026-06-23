package com.goldensystem.auris.data.service

import android.app.Notification
import com.goldensystem.auris.R
import android.content.Context
import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

/**
 * Wraps Media3's default provider and marks playback notifications as local-only
 * so they don't get bridged to Wear OS as generic remote media controls.
 */
@UnstableApi
class LocalOnlyMediaNotificationProvider(
    private val context: Context,
    private val delegate: DefaultMediaNotificationProvider = 
        DefaultMediaNotificationProvider.Builder(context)
            .setSmallIconResourceId(R.drawable.ic_stat_music)
            .build(),
) : MediaNotification.Provider {

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        callback: MediaNotification.Provider.Callback,
    ): MediaNotification {

        // Cria um wrapper do callback que corresponde ao que o DefaultMediaNotificationProvider espera
        val delegateCallback = object : DefaultMediaNotificationProvider.Callback {
            override fun onNotificationCreated(
                mediaSession: MediaSession,
                notification: MediaNotification
            ) {
                callback.onNotificationCreated(mediaSession, notification)
            }
        }

        val notification = delegate.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            delegateCallback
        )

        val localOnlyNotification = runCatching {
            Notification.Builder.recoverBuilder(context, notification.notification)
                .setLocalOnly(true)
                .build()
        }.getOrElse {
            notification.notification
        }

        return MediaNotification(notification.notificationId, localOnlyNotification)
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle,
    ): Boolean =
        delegate.handleCustomCommand(session, action, extras)
}