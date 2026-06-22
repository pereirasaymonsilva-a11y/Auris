package com.goldensystem.auris.presentation.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.goldensystem.auris.R

@Composable
fun BetaInfoBottomSheet(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // ✅ URLs lembradas para evitar recriação
    val feedbackUrl = remember { 
        "https://github.com/pereirasaymonsilva-a11y/Auris/issues/new/choose" 
    }
    val officialWebsite = remember { 
        "https://pereirasaymonsilva-a11y.github.io/Auris-website/data/home.html" 
    }
    val youtubeUrl = remember { 
        "https://www.youtube.com/@AurisMusicPlayer" 
    }
    val instagramUrl = remember { 
        "https://www.instagram.com/aurismp" 
    }
    val tiktokUrl = remember { 
        "https://www.tiktok.com/@auris_music_player" 
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ---------- Changelog ----------
        item(key = "changelog") {
            BetaInfoCard(
                icon = Icons.Rounded.Info,
                iconTint = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.about_changelog_title),
                content = {
                    Text(
                        text = stringResource(R.string.about_changelog_text),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }

        // ---------- Site Oficial ----------
        item(key = "official_website") {
            BetaInfoCard(
                title = stringResource(R.string.about_official_website_title),
                subtitle = stringResource(R.string.about_official_website_subtitle),
                buttonText = stringResource(R.string.about_official_website_button),
                buttonIcon = Icons.Rounded.Language,
                onClick = { launchUrl(context, officialWebsite) }
            )
        }

        // ---------- YouTube ----------
        item(key = "youtube") {
            BetaInfoCard(
                title = stringResource(R.string.about_youtube_title),
                subtitle = stringResource(R.string.about_youtube_subtitle),
                buttonText = stringResource(R.string.about_youtube_button),
                buttonIconRes = R.drawable.ic_youtube,
                onClick = { launchUrl(context, youtubeUrl) }
            )
        }

        // ---------- Redes Sociais (Instagram e TikTok) ----------
        item(key = "social_media") {
            BetaInfoCard(
                title = stringResource(R.string.about_social_media_title),
                content = {
                    Column {
                        // Instagram
                        OutlinedButton(
                            onClick = { launchUrl(context, instagramUrl) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_instagram), 
                                contentDescription = null, 
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.about_instagram))
                        }
                        
                        // TikTok
                        OutlinedButton(
                            onClick = { launchUrl(context, tiktokUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_tiktok), 
                                contentDescription = null, 
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.about_tiktok))
                        }
                    }
                }
            )
        }

        // ---------- Feedback ----------
        item(key = "feedback") {
            BetaInfoCard(
                icon = Icons.Rounded.BugReport,
                iconTint = MaterialTheme.colorScheme.error,
                title = stringResource(R.string.about_feedback_title),
                subtitle = stringResource(R.string.about_feedback_subtitle),
                buttonText = stringResource(R.string.about_feedback_button),
                buttonIcon = Icons.Rounded.BugReport,
                onClick = { launchUrl(context, feedbackUrl) }
            )
        }
    }
}

// ✅ Componente reutilizável e otimizado
@Composable
private fun BetaInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: androidx.compose.ui.graphics.Color? = null,
    iconRes: Int? = null,
    title: String,
    subtitle: String? = null,
    buttonText: String? = null,
    buttonIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    buttonIconRes: Int? = null,
    onClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header com ícone opcional
            if (icon != null || iconRes != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint ?: MaterialTheme.colorScheme.primary
                        )
                    } else if (iconRes != null) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = iconTint ?: MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Subtítulo
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Conteúdo customizado
            content?.let {
                Spacer(modifier = Modifier.height(8.dp))
                it()
            }

            // Botão
            if (buttonText != null && onClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (buttonIcon != null) {
                        Icon(buttonIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (buttonIconRes != null) {
                        Icon(
                            painter = painterResource(buttonIconRes), 
                            contentDescription = null, 
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(buttonText)
                }
            }
        }
    }
}

private fun launchUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) { 
        // Ignore
    }
}
