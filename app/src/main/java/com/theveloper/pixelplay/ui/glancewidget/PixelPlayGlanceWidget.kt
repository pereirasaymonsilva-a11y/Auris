package com.theveloper.pixelplay.ui.glancewidget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.theveloper.pixelplay.MainActivity
import com.theveloper.pixelplay.data.model.PlayerInfo
import com.theveloper.pixelplay.R
import androidx.glance.unit.ColorProvider
import com.theveloper.pixelplay.data.model.QueueItem
import com.theveloper.pixelplay.utils.createScalableBackgroundBitmap
import timber.log.Timber

/* ========== PALETAS DE CORES FIXAS ========== */

// --- Tema Claro (Amarelo Dourado / Preto) ---
private val lightSurface = ColorProvider(Color(0xFFFFF8E1))
private val lightBackground = ColorProvider(Color(0xFFFFFFFF))
private val lightPrimaryContainer = ColorProvider(Color(0xFFFFC107))
private val lightOnPrimaryContainer = ColorProvider(Color(0xFF000000))
private val lightSecondaryContainer = ColorProvider(Color(0xFF000000))
private val lightOnSecondaryContainer = ColorProvider(Color(0xFFFFC107))
private val lightOnSurface = ColorProvider(Color(0xFF000000))
private val lightSurfaceVariant = ColorProvider(Color(0xFFFFF3CD))
private val lightOnSurfaceVariant = ColorProvider(Color(0xFF4D3E00))

// --- Tema Escuro (Roxo personalizado) ---
private val darkSurface = ColorProvider(Color(0xFF2A1F40))
private val darkBackground = ColorProvider(Color(0xFF1E1234))
private val darkPrimaryContainer = ColorProvider(Color(0xFF6750A4))
private val darkOnPrimaryContainer = ColorProvider(Color(0xFFFFFFFF))
private val darkSecondaryContainer = ColorProvider(Color(0xFFF06292))
private val darkOnSecondaryContainer = ColorProvider(Color(0xFFFFFFFF))
private val darkOnSurface = ColorProvider(Color(0xFFE1BEE7))
private val darkSurfaceVariant = ColorProvider(Color(0xFF2A1F40))
private val darkOnSurfaceVariant = ColorProvider(Color(0xFFE1BEE7))

/* =========================================== */

class PixelPlayGlanceWidget : GlanceAppWidget() {

    companion object {
        private val VERY_THIN_LAYOUT_SIZE = DpSize(width = 200.dp, height = 60.dp)
        private val THIN_LAYOUT_SIZE = DpSize(width = 250.dp, height = 80.dp)
        private val SMALL_HORIZONTAL_LAYOUT_SIZE = DpSize(width = 110.dp, height = 60.dp)
        private val ONE_BY_ONE_LAYOUT_SIZE = DpSize(width = 110.dp, height = 110.dp)
        private val GABE_LAYOUT_SIZE = DpSize(width = 110.dp, height = 220.dp)
        private val GABE_TWO_HEIGHT_LAYOUT_SIZE = DpSize(width = 110.dp, height = 200.dp)
        private val SMALL_LAYOUT_SIZE = DpSize(width = 120.dp, height = 100.dp)
        private val MEDIUM_LAYOUT_SIZE = DpSize(width = 250.dp, height = 150.dp)
        private val LARGE_LAYOUT_SIZE = DpSize(width = 300.dp, height = 180.dp)
        private val EXTRA_LARGE_LAYOUT_SIZE = DpSize(width = 300.dp, height = 220.dp)
        private val EXTRA_LARGE_PLUS_LAYOUT_SIZE = DpSize(width = 350.dp, height = 260.dp)
        private val HUGE_LAYOUT_SIZE = DpSize(width = 400.dp, height = 300.dp)
    }

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PlayerInfoStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playerInfo = currentState<PlayerInfo>()
            val currentSize = LocalSize.current

            Timber.tag("PixelPlayGlanceWidget")
                .d("Providing Glance. PlayerInfo: title='${playerInfo.songTitle}', artist='${playerInfo.artistName}', isPlaying=${playerInfo.isPlaying}, progress=${playerInfo.currentPositionMs}/${playerInfo.totalDurationMs}")

            WidgetUi(playerInfo = playerInfo, size = currentSize, context = context)
        }
    }

    /**
     * Retorna as cores do widget conforme o modo escuro do sistema.
     */
    @Composable
    private fun widgetColors(context: Context): WidgetColors {
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return if (isDark) {
            WidgetColors(
                surface = darkSurface,
                background = darkBackground,
                primaryContainer = darkPrimaryContainer,
                onPrimaryContainer = darkOnPrimaryContainer,
                secondaryContainer = darkSecondaryContainer,
                onSecondaryContainer = darkOnSecondaryContainer,
                onSurface = darkOnSurface,
                surfaceVariant = darkSurfaceVariant,
                onSurfaceVariant = darkOnSurfaceVariant
            )
        } else {
            WidgetColors(
                surface = lightSurface,
                background = lightBackground,
                primaryContainer = lightPrimaryContainer,
                onPrimaryContainer = lightOnPrimaryContainer,
                secondaryContainer = lightSecondaryContainer,
                onSecondaryContainer = lightOnSecondaryContainer,
                onSurface = lightOnSurface,
                surfaceVariant = lightSurfaceVariant,
                onSurfaceVariant = lightOnSurfaceVariant
            )
        }
    }

    data class WidgetColors(
        val surface: ColorProvider,
        val background: ColorProvider,
        val primaryContainer: ColorProvider,
        val onPrimaryContainer: ColorProvider,
        val secondaryContainer: ColorProvider,
        val onSecondaryContainer: ColorProvider,
        val onSurface: ColorProvider,
        val surfaceVariant: ColorProvider,
        val onSurfaceVariant: ColorProvider
    )

    @Composable
    private fun WidgetUi(
        playerInfo: PlayerInfo,
        size: DpSize,
        context: Context
    ) {
        val colors = widgetColors(context)

        val title = playerInfo.songTitle.ifEmpty { context.getString(R.string.app_name) }
        val artist = playerInfo.artistName.ifEmpty { context.getString(R.string.widget_tap_to_open) }
        val isPlaying = playerInfo.isPlaying
        val isFavorite = playerInfo.isFavorite
        val albumArtBitmapData = playerInfo.albumArtBitmapData
        val albumArtUri = playerInfo.albumArtUri

        val actualBackgroundColor = colors.surface
        val onBackgroundColor = colors.onSurface

        val baseModifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>())

        Box(GlanceModifier.fillMaxSize()) {
            val isOneColumn = size.width < SMALL_LAYOUT_SIZE.width
            val isSmallHeight = size.height < SMALL_LAYOUT_SIZE.height

            if (isOneColumn) {
                when {
                    size.height <= ONE_BY_ONE_LAYOUT_SIZE.height -> OneByOneWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp,
                        isPlaying = isPlaying,
                        colors = colors
                    )
                    size.height <= GABE_TWO_HEIGHT_LAYOUT_SIZE.height -> GabeTwoHeightWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        context = context,
                        colors = colors
                    )
                    else -> GabeWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 360.dp,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        context = context,
                        colors = colors
                    )
                }
            } else if (isSmallHeight) {
                when {
                    size.width < VERY_THIN_LAYOUT_SIZE.width -> SmallHorizontalWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        context = context,
                        colors = colors
                    )
                    size.width < THIN_LAYOUT_SIZE.width -> VeryThinWidgetLayout(
                        modifier = baseModifier,
                        title = title,
                        artist = artist,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        textColor = onBackgroundColor,
                        context = context,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp,
                        colors = colors
                    )
                    else -> ThinWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp,
                        title = title,
                        artist = artist,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        textColor = onBackgroundColor,
                        context = context,
                        colors = colors
                    )
                }
            } else {
                when {
                    size.width < MEDIUM_LAYOUT_SIZE.width || size.height < MEDIUM_LAYOUT_SIZE.height -> SmallWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 28.dp,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        context = context,
                        colors = colors
                    )
                    size.width < LARGE_LAYOUT_SIZE.width || size.height < LARGE_LAYOUT_SIZE.height -> MediumWidgetLayout(
                        modifier = baseModifier,
                        title = title,
                        artist = artist,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        textColor = onBackgroundColor,
                        context = context,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 28.dp,
                        colors = colors
                    )
                    size.width < EXTRA_LARGE_LAYOUT_SIZE.width || size.height < EXTRA_LARGE_LAYOUT_SIZE.height -> LargeWidgetLayout(
                        modifier = baseModifier,
                        title = title,
                        artist = artist,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 28.dp,
                        isPlaying = isPlaying,
                        isFavorite = isFavorite,
                        textColor = onBackgroundColor,
                        context = context,
                        colors = colors
                    )
                    else -> ExtraLargeWidgetLayout(
                        modifier = baseModifier,
                        title = title,
                        artist = artist,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 28.dp,
                        textColor = onBackgroundColor,
                        context = context,
                        queue = playerInfo.queue,
                        colors = colors
                    )
                }
            }
        }
    }

    @Composable
    fun VeryThinWidgetLayout(
        modifier: GlanceModifier,
        title: String,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        artist: String,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        textColor: ColorProvider,
        context: Context,
        colors: WidgetColors
    ) {
        val size = LocalSize.current
        val albumArtSize = size.height - 32.dp

        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize().cornerRadius(bgCornerRadius),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                AlbumArtImageGlance(
                    modifier = GlanceModifier.size(albumArtSize),
                    bitmapData = albumArtBitmapData,
                    albumArtUri = albumArtUri,
                    context = context,
                    cornerRadius = bgCornerRadius,
                    colors = colors
                )
                Spacer(GlanceModifier.width(10.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(text = title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor), maxLines = 1)
                    if (artist.isNotEmpty() && artist != context.getString(R.string.widget_tap_to_open)) {
                        Text(text = artist, style = TextStyle(fontSize = 14.sp, color = textColor), maxLines = 1)
                    }
                }
                Spacer(GlanceModifier.width(8.dp))
                PlayPauseButtonGlance(
                    modifier = GlanceModifier.defaultWeight().size(48.dp, 48.dp).fillMaxHeight(),
                    backgroundColor = colors.primaryContainer,
                    iconColor = colors.onPrimaryContainer,
                    isPlaying = isPlaying,
                    iconSize = 26.dp,
                    cornerRadius = 10.dp
                )
                Spacer(GlanceModifier.width(10.dp))
                NextButtonGlance(
                    modifier = GlanceModifier.defaultWeight().size(48.dp, 48.dp).fillMaxHeight(),
                    iconColor = colors.onSecondaryContainer,
                    iconSize = 26.dp,
                    backgroundColor = colors.secondaryContainer,
                    cornerRadius = 10.dp
                )
            }
        }
    }

    @Composable
    fun ThinWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        title: String,
        artist: String,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        textColor: ColorProvider,
        context: Context,
        colors: WidgetColors
    ) {
        val size = LocalSize.current
        val albumArtSize = size.height - 32.dp

        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize().cornerRadius(bgCornerRadius),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                AlbumArtImageGlance(
                    modifier = GlanceModifier.size(albumArtSize),
                    bitmapData = albumArtBitmapData,
                    albumArtUri = albumArtUri,
                    context = context,
                    cornerRadius = bgCornerRadius,
                    colors = colors
                )
                Spacer(GlanceModifier.width(14.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(text = title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor), maxLines = 1)
                    if (artist.isNotEmpty() && artist != context.getString(R.string.widget_tap_to_open)) {
                        Text(text = artist, style = TextStyle(fontSize = 14.sp, color = textColor), maxLines = 1)
                    }
                }
                Spacer(GlanceModifier.width(8.dp))
                PlayPauseButtonGlance(
                    modifier = GlanceModifier.defaultWeight().size(48.dp, 48.dp).fillMaxHeight(),
                    backgroundColor = colors.primaryContainer,
                    iconColor = colors.onPrimaryContainer,
                    isPlaying = isPlaying,
                    iconSize = 26.dp,
                    cornerRadius = 10.dp
                )
                Spacer(GlanceModifier.width(10.dp))
                NextButtonGlance(
                    modifier = GlanceModifier.defaultWeight().size(48.dp, 48.dp).fillMaxHeight(),
                    iconColor = colors.onSecondaryContainer,
                    iconSize = 26.dp,
                    backgroundColor = colors.secondaryContainer,
                    cornerRadius = 10.dp
                )
            }
        }
    }

    @Composable
    fun GabeTwoHeightWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        context: Context,
        colors: WidgetColors
    ) {
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                AlbumArtImageGlance(
                    modifier = GlanceModifier.defaultWeight().height(48.dp),
                    bitmapData = albumArtBitmapData,
                    albumArtUri = albumArtUri,
                    context = context,
                    cornerRadius = 64.dp,
                    colors = colors
                )
                Spacer(GlanceModifier.height(14.dp))
                Column(modifier = GlanceModifier.defaultWeight().cornerRadius(bgCornerRadius)) {
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                        backgroundColor = colors.primaryContainer,
                        iconColor = colors.onPrimaryContainer,
                        isPlaying = isPlaying,
                        iconSize = 26.dp,
                        cornerRadius = 10.dp
                    )
                    Spacer(GlanceModifier.height(10.dp))
                    NextButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                        iconColor = colors.onSecondaryContainer,
                        iconSize = 26.dp,
                        backgroundColor = colors.secondaryContainer,
                        cornerRadius = 10.dp
                    )
                }
            }
        }
    }

    @Composable
    fun GabeWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        context: Context,
        colors: WidgetColors
    ) {
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                AlbumArtImageGlance(
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth().height(48.dp),
                    bitmapData = albumArtBitmapData,
                    albumArtUri = albumArtUri,
                    context = context,
                    cornerRadius = 64.dp,
                    colors = colors
                )
                Spacer(GlanceModifier.height(14.dp))
                Column(modifier = GlanceModifier.defaultWeight().cornerRadius(bgCornerRadius)) {
                    PreviousButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                        iconColor = colors.onSecondaryContainer,
                        iconSize = 26.dp,
                        backgroundColor = colors.secondaryContainer,
                        cornerRadius = 10.dp
                    )
                    Spacer(GlanceModifier.height(10.dp))
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                        backgroundColor = colors.primaryContainer,
                        iconColor = colors.onPrimaryContainer,
                        isPlaying = isPlaying,
                        iconSize = 26.dp,
                        cornerRadius = 10.dp
                    )
                    Spacer(GlanceModifier.height(10.dp))
                    NextButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                        iconColor = colors.onSecondaryContainer,
                        iconSize = 26.dp,
                        backgroundColor = colors.secondaryContainer,
                        cornerRadius = 10.dp
                    )
                }
            }
        }
    }

    @Composable
    fun OneByOneWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        isPlaying: Boolean,
        colors: WidgetColors
    ) {
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            PlayPauseButtonGlance(
                modifier = GlanceModifier.fillMaxSize(),
                backgroundColor = colors.primaryContainer,
                iconColor = colors.onPrimaryContainer,
                isPlaying = isPlaying,
                iconSize = 36.dp,
                cornerRadius = 30.dp
            )
        }
    }

    @Composable
    fun SmallHorizontalWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        context: Context,
        colors: WidgetColors
    ) {
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize().cornerRadius(bgCornerRadius),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                AlbumArtImageGlance(
                    modifier = GlanceModifier.padding(vertical = 6.dp),
                    bitmapData = albumArtBitmapData,
                    albumArtUri = albumArtUri,
                    size = 58.dp,
                    context = context,
                    cornerRadius = 64.dp,
                    colors = colors
                )
                Spacer(GlanceModifier.width(14.dp))
                PlayPauseButtonGlance(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                    backgroundColor = colors.primaryContainer,
                    iconColor = colors.onPrimaryContainer,
                    isPlaying = isPlaying,
                    iconSize = 26.dp,
                    cornerRadius = 10.dp
                )
            }
        }
    }

    @Composable
    fun SmallWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        context: Context,
        colors: WidgetColors
    ) {
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(12.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    AlbumArtImageGlance(
                        modifier = GlanceModifier.defaultWeight(),
                        bitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        context = context,
                        cornerRadius = 64.dp,
                        colors = colors
                    )
                }
                Spacer(GlanceModifier.height(8.dp))
                PlayPauseButtonGlance(
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth().height(50.dp),
                    isPlaying = isPlaying,
                    cornerRadius = if (isPlaying) 12.dp else 60.dp,
                    iconSize = 26.dp,
                    backgroundColor = colors.primaryContainer,
                    iconColor = colors.onPrimaryContainer
                )
                Spacer(GlanceModifier.height(8.dp))
                Row(
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth().height(50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PreviousButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        iconSize = 26.dp,
                        cornerRadius = 16.dp,
                        backgroundColor = colors.secondaryContainer,
                        iconColor = colors.onSecondaryContainer
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    NextButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        iconSize = 26.dp,
                        cornerRadius = 16.dp,
                        backgroundColor = colors.secondaryContainer,
                        iconColor = colors.onSecondaryContainer
                    )
                }
            }
        }
    }

    @Composable
    fun MediumWidgetLayout(
        modifier: GlanceModifier,
        title: String,
        artist: String,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        textColor: ColorProvider,
        context: Context,
        colors: WidgetColors
    ) {
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArtImageGlance(
                        bitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        size = 80.dp,
                        context = context,
                        cornerRadius = 16.dp,
                        colors = colors
                    )
                    Spacer(GlanceModifier.width(12.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = title,
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor),
                            maxLines = 2
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(text = artist, style = TextStyle(fontSize = 13.sp, color = textColor), maxLines = 2)
                    }
                }
                Spacer(GlanceModifier.height(12.dp))
                Row(
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreviousButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        iconColor = colors.onSecondaryContainer,
                        backgroundColor = colors.secondaryContainer,
                        cornerRadius = 60.dp
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        isPlaying = isPlaying,
                        iconColor = colors.onPrimaryContainer,
                        backgroundColor = colors.primaryContainer,
                        cornerRadius = if (isPlaying) 14.dp else 60.dp
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    NextButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        iconColor = colors.onSecondaryContainer,
                        backgroundColor = colors.secondaryContainer,
                        cornerRadius = 60.dp
                    )
                }
            }
        }
    }

    @Composable
    fun LargeWidgetLayout(
        modifier: GlanceModifier,
        title: String,
        artist: String,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        isPlaying: Boolean,
        isFavorite: Boolean,
        textColor: ColorProvider,
        context: Context,
        colors: WidgetColors
    ) {
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    AlbumArtImageGlance(
                        bitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        size = 64.dp,
                        context = context,
                        cornerRadius = 18.dp,
                        colors = colors
                    )
                    Spacer(GlanceModifier.width(12.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(text = title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor), maxLines = 1)
                        Text(text = artist, style = TextStyle(fontSize = 13.sp, color = textColor), maxLines = 1)
                    }
                    Spacer(GlanceModifier.width(4.dp))
                    Image(
                        provider = ImageProvider(if (isFavorite) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24),
                        contentDescription = context.getString(R.string.cd_favorite),
                        modifier = GlanceModifier
                            .size(28.dp)
                            .clickable(actionRunCallback<PlayerControlActionCallback>(actionParametersOf(PlayerActions.key to PlayerActions.FAVORITE)))
                            .padding(2.dp),
                        colorFilter = ColorFilter.tint(textColor)
                    )
                    Spacer(GlanceModifier.width(8.dp))
                }
                Spacer(GlanceModifier.height(4.dp))
                Spacer(GlanceModifier.height(10.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreviousButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        iconColor = colors.onSecondaryContainer,
                        backgroundColor = colors.secondaryContainer,
                        cornerRadius = 60.dp
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        isPlaying = isPlaying,
                        iconColor = colors.onPrimaryContainer,
                        backgroundColor = colors.primaryContainer,
                        cornerRadius = if (isPlaying) 14.dp else 60.dp
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    NextButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        iconColor = colors.onSecondaryContainer,
                        backgroundColor = colors.secondaryContainer,
                        cornerRadius = 60.dp
                    )
                }
            }
        }
    }

    @Composable
    fun ExtraLargeWidgetLayout(
        modifier: GlanceModifier, title: String, artist: String, albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean, backgroundColor: ColorProvider, bgCornerRadius: Dp,
        textColor: ColorProvider,
        context: Context,
        queue: List<QueueItem>,
        colors: WidgetColors
    ) {
        val playButtonCornerRadius = if (isPlaying) 16.dp else 60.dp

        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    AlbumArtImageGlance(
                        bitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        size = 68.dp,
                        context = context,
                        cornerRadius = 16.dp,
                        colors = colors
                    )
                    Spacer(GlanceModifier.width(16.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = title,
                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor),
                            maxLines = 2
                        )
                        Text(
                            text = artist,
                            style = TextStyle(fontSize = 16.sp, color = textColor),
                            maxLines = 1
                        )
                    }
                }

                Spacer(GlanceModifier.height(14.dp))

                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val buttonCornerRadius = 60.dp

                    PreviousButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        iconColor = colors.onSecondaryContainer,
                        backgroundColor = colors.secondaryContainer,
                        iconSize = 28.dp,
                        cornerRadius = buttonCornerRadius
                    )
                    Spacer(GlanceModifier.width(10.dp))
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        isPlaying = isPlaying,
                        iconColor = colors.onPrimaryContainer,
                        backgroundColor = colors.primaryContainer,
                        iconSize = 30.dp,
                        cornerRadius = playButtonCornerRadius
                    )
                    Spacer(GlanceModifier.width(10.dp))
                    NextButtonGlance(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        iconColor = colors.onSecondaryContainer,
                        backgroundColor = colors.secondaryContainer,
                        iconSize = 28.dp,
                        cornerRadius = buttonCornerRadius
                    )
                }

                Spacer(GlanceModifier.defaultWeight())

                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp)
                        .background(colors.onSurface.getColor(context).copy(alpha = 0.15f))
                        .height(2.dp)
                        .cornerRadius(60.dp)
                )

                Spacer(GlanceModifier.height(12.dp))

                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(58.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val items = queue.take(4)
                    val itemSize = 58.dp
                    val cornerRadius = 14.dp

                    for (i in 0 until 4) {
                        Box(
                            modifier = GlanceModifier.defaultWeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (i < items.size) {
                                val queueItem = items[i]
                                AlbumArtImageGlance(
                                    modifier = GlanceModifier.clickable(
                                        actionRunCallback<PlayerControlActionCallback>(
                                            actionParametersOf(
                                                PlayerActions.key to PlayerActions.PLAY_FROM_QUEUE,
                                                PlayerActions.songIdKey to queueItem.id
                                            )
                                        )
                                    ),
                                    bitmapData = null,
                                    albumArtUri = queueItem.albumArtUri,
                                    size = itemSize,
                                    context = context,
                                    cornerRadius = cornerRadius,
                                    colors = colors
                                )
                            } else {
                                EndOfQueuePlaceholder(
                                    size = itemSize,
                                    cornerRadius = cornerRadius,
                                    colors = colors
                                )
                            }
                        }

                        if (i < 3) {
                            Spacer(GlanceModifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AlbumArtImageGlance(
        bitmapData: ByteArray?,
        albumArtUri: String? = null,
        size: Dp? = null,
        context: Context,
        modifier: GlanceModifier = GlanceModifier,
        cornerRadius: Dp = 16.dp,
        colors: WidgetColors
    ) {
        val TAG_AAIG = "AlbumArtImageGlance"
        Timber.tag(TAG_AAIG).d("Init. bitmapData is null: ${bitmapData == null}. Requested Dp size: $size")
        if (bitmapData != null) Timber.tag(TAG_AAIG).d("bitmapData size: ${bitmapData.size} bytes")

        val sizingModifier = if (size != null) modifier.size(size) else modifier
        val widgetDpSize = LocalSize.current

        val imageProvider = bitmapData?.let { data ->
            val cacheKey = AlbumArtBitmapCache.getKey(data)
            var bitmap = AlbumArtBitmapCache.getBitmap(cacheKey)
            if (bitmap == null) {
                try {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(data, 0, data.size, options)
                    val imageHeight = options.outHeight
                    val imageWidth = options.outWidth
                    var inSampleSize = 1
                    val targetWidthPx: Int
                    val targetHeightPx: Int
                    with(context.resources.displayMetrics) {
                        if (size != null) {
                            val targetSizePx = (size.value * density).toInt()
                            targetWidthPx = targetSizePx
                            targetHeightPx = targetSizePx
                        } else {
                            targetWidthPx = (widgetDpSize.width.value * density).toInt()
                            targetHeightPx = (widgetDpSize.height.value * density).toInt()
                        }
                    }
                    if (imageHeight > targetHeightPx || imageWidth > targetWidthPx) {
                        val halfHeight = imageHeight / 2
                        val halfWidth = imageWidth / 2
                        while (halfHeight / inSampleSize >= targetHeightPx && halfWidth / inSampleSize >= targetWidthPx) {
                            inSampleSize *= 2
                        }
                    }
                    options.inSampleSize = inSampleSize
                    options.inJustDecodeBounds = false
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
                    bitmap?.let { AlbumArtBitmapCache.putBitmap(cacheKey, it) }
                } catch (e: Exception) {
                    Timber.tag(TAG_AAIG).e(e, "Error decoding bitmap")
                }
            }
            bitmap?.let { ImageProvider(it) }
        } ?: albumArtUri?.let { rawUri ->
            val cacheKey = "uri:$rawUri"
            var bitmap = AlbumArtBitmapCache.getBitmap(cacheKey)
            if (bitmap == null) {
                bitmap = decodeAlbumArtFromUri(context, rawUri, size, widgetDpSize)
                bitmap?.let { AlbumArtBitmapCache.putBitmap(cacheKey, it) }
            }
            bitmap?.let { ImageProvider(it) }
        }

        Box(modifier = sizingModifier) {
            if (imageProvider != null) {
                Image(
                    provider = imageProvider,
                    contentDescription = context.getString(R.string.widget_album_art),
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(cornerRadius),
                    contentScale = ContentScale.Crop
                )
            } else {
                val placeholderBaseSize = size ?: minOf(widgetDpSize.width, widgetDpSize.height)
                val resolvedPlaceholderBaseSize = when {
                    placeholderBaseSize == Dp.Unspecified || placeholderBaseSize <= 0.dp -> 72.dp
                    else -> placeholderBaseSize
                }
                val placeholderSize = resolvedPlaceholderBaseSize
                    .times(0.65f)
                    .coerceIn(36.dp, 96.dp)

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(cornerRadius)
                        .background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_placeholder),
                        contentDescription = context.getString(R.string.widget_album_art_placeholder),
                        modifier = GlanceModifier.size(placeholderSize),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(colors.onSurfaceVariant)
                    )
                }
            }
        }
    }

    private fun decodeAlbumArtFromUri(context: Context, rawUri: String, requestedSize: Dp?, widgetDpSize: DpSize): Bitmap? {
        val (targetWidthPx, targetHeightPx) = with(context.resources.displayMetrics) {
            if (requestedSize != null) {
                val target = (requestedSize.value * density).toInt().coerceAtLeast(1)
                target to target
            } else {
                val width = (widgetDpSize.width.value * density).toInt().coerceAtLeast(1)
                val height = (widgetDpSize.height.value * density).toInt().coerceAtLeast(1)
                width to height
            }
        }
        return decodeWidgetAlbumArtBitmap(context, rawUri, targetWidthPx, targetHeightPx)
    }

    @Composable
    fun PlayPauseButtonGlance(
        modifier: GlanceModifier = GlanceModifier,
        isPlaying: Boolean,
        iconColor: ColorProvider = GlanceTheme.colors.onSurfaceVariant,
        backgroundColor: ColorProvider = GlanceTheme.colors.surfaceVariant,
        iconSize: Dp = 24.dp,
        cornerRadius: Dp = 0.dp
    ) {
        val context = LocalContext.current
        val params = actionParametersOf(PlayerActions.key to PlayerActions.PLAY_PAUSE)
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(cornerRadius)
                .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(if (isPlaying) R.drawable.rounded_pause_24 else R.drawable.rounded_play_arrow_24),
                contentDescription = context.getString(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                modifier = GlanceModifier.size(iconSize),
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
    }

    @Composable
    fun NextButtonGlance(
        modifier: GlanceModifier = GlanceModifier,
        iconColor: ColorProvider = GlanceTheme.colors.onSurfaceVariant,
        backgroundColor: ColorProvider = GlanceTheme.colors.surfaceVariant,
        iconSize: Dp = 24.dp,
        cornerRadius: Dp = 8.dp
    ) {
        val context = LocalContext.current
        val params = actionParametersOf(PlayerActions.key to PlayerActions.NEXT)
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(cornerRadius)
                .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.rounded_skip_next_24),
                contentDescription = context.getString(R.string.next_track),
                modifier = GlanceModifier.size(iconSize),
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
    }

    @Composable
    fun PreviousButtonGlance(
        modifier: GlanceModifier = GlanceModifier,
        iconColor: ColorProvider = GlanceTheme.colors.onSurfaceVariant,
        backgroundColor: ColorProvider = GlanceTheme.colors.surfaceVariant,
        iconSize: Dp = 24.dp,
        cornerRadius: Dp = 8.dp
    ) {
        val context = LocalContext.current
        val params = actionParametersOf(PlayerActions.key to PlayerActions.PREVIOUS)
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(cornerRadius)
                .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.rounded_skip_previous_24),
                contentDescription = context.getString(R.string.previous_track),
                modifier = GlanceModifier.size(iconSize),
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
    }

    @Composable
    fun EndOfQueuePlaceholder(
        modifier: GlanceModifier = GlanceModifier,
        size: Dp,
        cornerRadius: Dp,
        colors: WidgetColors
    ) {
        Box(
            modifier = modifier
                .size(size)
                .background(colors.surfaceVariant)
                .cornerRadius(cornerRadius)
        )
    }
}

// Função externa mantida
private fun formatDurationGlance(millis: Long): String {
    if (millis < 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}