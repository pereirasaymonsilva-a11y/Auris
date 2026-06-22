package com.goldensystem.auris.presentation.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.request.ImageRequest
import coil.size.Size
import com.goldensystem.auris.R
import com.goldensystem.auris.presentation.components.CollapsibleCommonTopBar
import com.goldensystem.auris.presentation.components.MiniPlayerHeight
import com.goldensystem.auris.presentation.components.SmartImage
import com.goldensystem.auris.presentation.navigation.Screen
import com.goldensystem.auris.presentation.navigation.navigateSafely
import com.goldensystem.auris.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.math.roundToInt

// ---------- Dados de Contribuidores (apenas mantenedores) ----------

private data class Contributor(
    val id: String,
    val displayName: String,
    val role: String,
    val detail: String? = null,
    val badge: String? = null,
    val avatarUrl: String? = null,
    val iconRes: Int? = null,
    val githubUrl: String? = null,
    val telegramUrl: String? = null,
)

// ---------- Tela Principal ----------

@androidx.annotation.OptIn(UnstableApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun AboutScreen(
    navController: NavController,
    viewModel: PlayerViewModel,
    onNavigationIconClick: () -> Unit,
) {
    val context = LocalContext.current
    val versionName: String = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "N/A"
    } catch (_: Exception) {
        "N/A"
    }

    // URLs
    val officialWebsite = "https://pereirasaymonsilva-a11y.github.io/Auris-website/data/home.html"
    val youtubeUrl = "https://www.youtube.com/@AurisMusicPlayer"
    val instagramUrl = "https://www.instagram.com/aurismp"
    val tiktokUrl = "https://www.tiktok.com/@auris_music_player"

    // ---------- Dados dos mantenedores ----------
    val coreMaintainer = Contributor(
        id = "theovilardo",
        displayName = stringResource(R.string.contributor_theo_display_name),
        role = stringResource(R.string.contributor_theo_role),
        detail = stringResource(R.string.contributor_theo_detail),
        avatarUrl = "https://avatars.githubusercontent.com/u/26845343?v=4",
        iconRes = R.drawable.round_developer_board_24,
        githubUrl = "https://github.com/theovilardo",
        telegramUrl = "https://t.me/thevelopersupport",
    )

    val aurisMaintainer = Contributor(
        id = "pereirasaymonsilva-a11y",
        displayName = stringResource(R.string.contributor_auris_display_name),
        role = stringResource(R.string.contributor_auris_role),
        detail = stringResource(R.string.contributor_auris_detail),
        avatarUrl = "https://avatars.githubusercontent.com/u/255678043?v=4&size=64",
        iconRes = R.drawable.ic_music_placeholder,
        githubUrl = "https://github.com/pereirasaymonsilva-a11y",
        telegramUrl = "about.scream",
    )

    // ---------- Animações de entrada ----------
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        transitionState.targetState = true
    }
    val transition = rememberTransition(transitionState, label = "AboutAppearTransition")

    val contentAlpha by transition.animateFloat(
        label = "ContentAlpha",
        transitionSpec = { tween(durationMillis = 500) },
    ) { if (it) 1f else 0f }

    val contentOffset by transition.animateDp(
        label = "ContentOffset",
        transitionSpec = { tween(durationMillis = 400, easing = FastOutSlowInEasing) },
    ) { if (it) 0.dp else 40.dp }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val statusBarHeight = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 170.dp

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    // ✅ STATE-DRIVEN APPROACH - SEM coroutine no scroll!
    val topBarHeight = remember(maxTopBarHeightPx) {
        Animatable(maxTopBarHeightPx)
    }
    
    // ✅ State único: targetHeight - atualizado SINCronamente no scroll
    var targetHeight by remember { mutableFloatStateOf(maxTopBarHeightPx) }
    
    // ✅ Altura atual para UI (derivada do Animatable)
    var currentHeight by remember { mutableFloatStateOf(maxTopBarHeightPx) }
    
    // ✅ Sincroniza o Animatable com o targetHeight via LaunchedEffect
    LaunchedEffect(targetHeight) {
        // Evita animação se já estiver no valor correto
        if (topBarHeight.value != targetHeight) {
            topBarHeight.animateTo(
                targetHeight,
                spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }
    
    // ✅ Monitora mudanças do Animatable para atualizar currentHeight
    LaunchedEffect(topBarHeight) {
        snapshotFlow { topBarHeight.value }
            .distinctUntilChanged()
            .collectLatest { height ->
                currentHeight = height
            }
    }

    // ✅ collapseFraction derivado do currentHeight
    val collapseFraction by remember {
        derivedStateOf {
            val range = maxTopBarHeightPx - minTopBarHeightPx
            if (range == 0f) 0f else {
                (1f - ((currentHeight - minTopBarHeightPx) / range)).coerceIn(0f, 1f)
            }
        }
    }

    // ✅ NESTEDSCROLL SEM COROUTINE - só atualiza o state!
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0

                if (
                    !isScrollingDown &&
                    (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)
                ) {
                    return Offset.Zero
                }

                val newHeight = (currentHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - currentHeight

                if (consumed.roundToInt() != 0) {
                    // ✅ SEM coroutine! Só atualiza o state
                    targetHeight = newHeight
                }

                val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    // ✅ Hysteresis para expand/collapse com thresholds
    val expandThreshold = 0.7f
    val collapseThreshold = 0.3f
    
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val currentFraction = collapseFraction
            val shouldExpand = when {
                currentFraction < collapseThreshold -> true
                currentFraction > expandThreshold -> false
                else -> {
                    // Zona morta - manter estado atual
                    val middlePoint = (minTopBarHeightPx + maxTopBarHeightPx) / 2
                    currentHeight > middlePoint
                }
            }
            
            val canExpand = lazyListState.firstVisibleItemIndex == 0 && 
                           lazyListState.firstVisibleItemScrollOffset == 0
            val targetValue = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx

            if (targetHeight != targetValue) {
                targetHeight = targetValue
            }
        }
    }

    Box(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection)
            .fillMaxSize()
            .graphicsLayer {
                alpha = contentAlpha
                translationY = contentOffset.toPx()
            },
    ) {
        val currentTopBarHeightDp = with(density) { currentHeight.toDp() }
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                top = currentTopBarHeightDp + 8.dp,
                bottom = MiniPlayerHeight +
                    WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding() + 12.dp,
            ),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ---------- Card principal (Auris) ----------
            item(key = "hero_card") {
                AboutHeroCard(
                    versionName = versionName,
                    onVersionLongPress = {
                        navController.navigateSafely(Screen.EasterEgg.route)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
                )
            }

            // ---------- Seção: Changelog ----------
            item(key = "changelog_section") {
                AboutSectionHeader(
                    title = stringResource(R.string.about_changelog_title),
                    subtitle = stringResource(R.string.about_version_format, versionName),
                    modifier = Modifier.padding(top = 24.dp),
                )
                ChangelogCard(
                    text = stringResource(R.string.about_changelog_text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ---------- Seção: Site Oficial ----------
            item(key = "official_website_section") {
                AboutSectionHeader(
                    title = stringResource(R.string.about_official_website_title),
                    subtitle = stringResource(R.string.about_official_website_subtitle),
                    modifier = Modifier.padding(top = 24.dp),
                )
                OutlinedButton(
                    onClick = { openUrl(context, officialWebsite) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.about_official_website_button))
                }
            }

            // ---------- Seção: YouTube ----------
            item(key = "youtube_section") {
                AboutSectionHeader(
                    title = stringResource(R.string.about_youtube_title),
                    subtitle = stringResource(R.string.about_youtube_subtitle),
                    modifier = Modifier.padding(top = 24.dp),
                )
                OutlinedButton(
                    onClick = { openUrl(context, youtubeUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(painterResource(R.drawable.ic_youtube), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.about_youtube_button))
                }
            }

            // ---------- Seção: Redes Sociais (Instagram e TikTok) ----------
            item(key = "social_media_section") {
                AboutSectionHeader(
                    title = stringResource(R.string.about_social_media_title),
                    subtitle = "",
                    modifier = Modifier.padding(top = 24.dp),
                )
                
                // Instagram
                OutlinedButton(
                    onClick = { openUrl(context, instagramUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(painterResource(R.drawable.ic_instagram), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.about_instagram))
                }
                
                // TikTok
                OutlinedButton(
                    onClick = { openUrl(context, tiktokUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(painterResource(R.drawable.ic_tiktok), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.about_tiktok))
                }
            }

            // ---------- Seção: Feedback ----------
            item(key = "feedback_section") {
                AboutSectionHeader(
                    title = stringResource(R.string.about_feedback_title),
                    subtitle = stringResource(R.string.about_feedback_subtitle),
                    modifier = Modifier.padding(top = 24.dp),
                )
                OutlinedButton(
                    onClick = {
                        openUrl(context, "https://github.com/pereirasaymonsilva-a11y/Auris/issues/new?template=blank")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.about_feedback_button))
                }
            }

            // ---------- Mantenedores ----------
            item(key = "maintainer_title") {
                AboutSectionHeader(
                    title = stringResource(R.string.about_maintainer_title),
                    subtitle = stringResource(R.string.about_maintainer_subtitle),
                    modifier = Modifier.padding(top = 24.dp),
                )
            }

            item(key = "maintainer_card_theo") {
                ContributorCard(
                    contributor = coreMaintainer,
                    shape = expressiveListShape(index = 0, count = 2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onCardClick = coreMaintainer.githubUrl?.let { url -> { openUrl(context, url) } },
                )
            }

            item(key = "maintainer_card_saymon") {
                ContributorCard(
                    contributor = aurisMaintainer,
                    shape = expressiveListShape(index = 1, count = 2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 3.dp),
                    onCardClick = aurisMaintainer.githubUrl?.let { url -> { openUrl(context, url) } },
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        CollapsibleCommonTopBar(
            title = stringResource(R.string.screen_about),
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackClick = onNavigationIconClick,
            expandedTitleStartPadding = 20.dp,
            collapsedTitleStartPadding = 68.dp
        )
    }
}

// ---------- Componentes Auxiliares (mantidos iguais) ----------

@Composable
private fun AboutHeroCard(
    versionName: String,
    onVersionLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val heroShape = AbsoluteSmoothCornerShape(30.dp, 60)
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier,
        shape = heroShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.auris_base_monochrome),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp).size(28.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.about_app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.about_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onVersionLongPress()
                                },
                            )
                        },
                ) {
                    Text(
                        text = stringResource(R.string.about_version_format, versionName),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                CommunitySignalsRow()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommunitySignalsRow() {
    val labels = listOf(
        stringResource(R.string.about_signal_community_first) to Icons.Rounded.AutoAwesome,
        stringResource(R.string.about_signal_material3) to Icons.Rounded.Palette,
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEach { (label, icon) ->
            Surface(
                shape = AbsoluteSmoothCornerShape(16.dp, 60),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun ChangelogCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}

@Composable
private fun ContributorCard(
    contributor: Contributor,
    shape: AbsoluteSmoothCornerShape,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onCardClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = LocalIndication.current,
            role = Role.Button,
            onClick = onCardClick,
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickableModifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContributorAvatar(
                name = contributor.displayName,
                avatarUrl = contributor.avatarUrl,
                iconRes = contributor.iconRes ?: R.drawable.rounded_person_24,
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            ) {
                Text(
                    text = contributor.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = contributor.role,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )

                contributor.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                contributor.badge?.let { badge ->
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        ContributorLabel(text = badge)
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SocialIconButton(
                    painterRes = R.drawable.github,
                    contentDescription = stringResource(R.string.cd_open_github_profile),
                    url = contributor.githubUrl,
                )
                SocialIconButton(
                    painterRes = R.drawable.telegram,
                    contentDescription = stringResource(R.string.cd_open_telegram),
                    url = contributor.telegramUrl,
                )
            }
        }
    }
}

@Composable
private fun ContributorLabel(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ContributorAvatar(
    name: String,
    avatarUrl: String?,
    @DrawableRes iconRes: Int?,
    modifier: Modifier = Modifier,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val letterBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val letterTint = MaterialTheme.colorScheme.onSurfaceVariant
    val initial = name.removePrefix("@").firstOrNull()?.uppercase() ?: "?"

    Surface(
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = containerColor,
        tonalElevation = 2.dp,
    ) {
        when {
            !avatarUrl.isNullOrBlank() -> {
                SmartImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .size(Size(96, 96))
                        .build(),
                    contentDescription = stringResource(R.string.cd_contributor_avatar, name),
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    contentScale = ContentScale.Crop,
                    placeholderResId = iconRes ?: R.drawable.ic_music_placeholder,
                    errorResId = R.drawable.rounded_broken_image_24,
                )
            }
            iconRes != null -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(letterBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = stringResource(R.string.cd_contributor_icon, name),
                        tint = iconTint,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            else -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(letterBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = letterTint,
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialIconButton(
    painterRes: Int,
    contentDescription: String,
    url: String?,
    modifier: Modifier = Modifier,
) {
    if (url.isNullOrBlank()) return
    val context = LocalContext.current
    
    IconButton(
        onClick = { openUrl(context, url) },
        modifier = modifier.size(40.dp),
    ) {
        Icon(
            painter = painterResource(painterRes),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun expressiveListShape(index: Int, count: Int): AbsoluteSmoothCornerShape {
    val outer = 22.dp
    val inner = 8.dp

    return when {
        count <= 1 -> AbsoluteSmoothCornerShape(outer, 60)
        index == 0 -> AbsoluteSmoothCornerShape(
            cornerRadiusTL = outer,
            cornerRadiusTR = outer,
            cornerRadiusBL = inner,
            cornerRadiusBR = inner,
            smoothnessAsPercentTL = 60,
            smoothnessAsPercentTR = 60,
            smoothnessAsPercentBL = 60,
            smoothnessAsPercentBR = 60,
        )
        index == count - 1 -> AbsoluteSmoothCornerShape(
            cornerRadiusTL = inner,
            cornerRadiusTR = inner,
            cornerRadiusBL = outer,
            cornerRadiusBR = outer,
            smoothnessAsPercentTL = 60,
            smoothnessAsPercentTR = 60,
            smoothnessAsPercentBL = 60,
            smoothnessAsPercentBR = 60,
        )
        else -> AbsoluteSmoothCornerShape(inner, 60)
    }
}

private fun openUrl(context: Context, url: String) {
    val uri = try {
        url.toUri()
    } catch (_: Throwable) {
        return
    }

    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Ignore if no handler is available.
    }
}