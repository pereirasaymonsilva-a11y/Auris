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

@UnstableApi
class LocalOnlyMediaNotificationProvider(
    private val context: Context,
    private val delegate: DefaultMediaNotificationProvider =
        DefaultMediaNotificationProvider.Builder(context)
            .build(),
) : MediaNotification.Provider {

    private var smallIconResId: Int = R.drawable.ic_stat_music

    fun setSmallIcon(iconResId: Int) {
        smallIconResId = iconResId
        // Tenta definir no delegate se possível
        try {
            delegate::class.java.getMethod("setSmallIcon", Int::class.java)
                ?.invoke(delegate, iconResId)
        } catch (_: Exception) {
            // Ignora se não existir
        }
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        callback: MediaNotification.Provider.Callback,
    ): MediaNotification {

        val notification = delegate.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            callback
        )

        val localOnlyNotification = runCatching {
            Notification.Builder.recoverBuilder(context, notification.notification)
                .setSmallIcon(smallIconResId)  // <-- DEFINE O ÍCONE AQUI
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