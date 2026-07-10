package com.example.ui.screens

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.HotPlatform
import com.example.data.model.PlatformCategory
import com.example.ui.viewmodel.GoldPriceViewModel
import com.example.ui.viewmodel.HotSearchViewModel
import com.example.ui.viewmodel.OilPriceViewModel
import com.example.ui.viewmodel.News60sViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.WeatherAlertViewModel

enum class DashboardMode(
    val shortLabel: String,
    val title: String,
    val emoji: String,
    val color: Color
) {
    NEWS_60S("60S", "60秒读世界", "📰", Color(0xFF2196F3)),
    HOT_SEARCH("热搜", "多平台热搜", "", Color(0xFFFF6B35)),
    WEATHER_ALERT("预警", "气象预警", "⚠️", Color(0xFF00ACC1)),
    OIL_PRICE("油价", "油价查询", "⛽", Color(0xFFFF6B35)),
    GOLD_PRICE("金价", "金价查询", "🪙", Color(0xFFD4A017));

    fun accentColor(activePlatform: HotPlatform): Color =
        if (this == HOT_SEARCH) activePlatform.brandColor else color

    fun badge(activePlatform: HotPlatform): String =
        if (this == HOT_SEARCH) activePlatform.infoEmoji else emoji
}

@Composable
fun DailyHotDashboard(
    hotViewModel: HotSearchViewModel,
    oilViewModel: OilPriceViewModel,
    goldViewModel: GoldPriceViewModel,
    news60sViewModel: News60sViewModel,
    weatherAlertViewModel: WeatherAlertViewModel,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activePlatform by hotViewModel.activePlatform.collectAsState()
    val searchQuery by hotViewModel.searchQuery.collectAsState()
    val uiState by hotViewModel.uiState.collectAsState()
    val oilState by oilViewModel.uiState.collectAsState()
    val goldState by goldViewModel.uiState.collectAsState()
    val selectedProvince by oilViewModel.selectedProvince.collectAsState()
    val news60sState by news60sViewModel.uiState.collectAsState()
    val weatherAlertState by weatherAlertViewModel.uiState.collectAsState()
    var mode by rememberSaveable { mutableStateOf(DashboardMode.NEWS_60S) }
    var selectedPlatformCategory by rememberSaveable { mutableStateOf(PlatformCategory.ALL) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Web view preview state
    var previewUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var previewTitle by rememberSaveable { mutableStateOf<String?>(null) }

    // Floating action rotation animation state
    var isRotating by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRotating) 360f else 0f,
        animationSpec = tween(durationMillis = 650),
        finishedListener = { isRotating = false }
    )

    // Back handler to close native WebView preview
    if (previewUrl != null) {
        BackHandler {
            previewUrl = null
            previewTitle = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                activePlatform.brandColor.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, 200f),
                            radius = size.width * 0.8f
                        )
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                HeaderSection(
                    activePlatform = activePlatform,
                    mode = mode,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onModeChange = { mode = it },
                    onRefresh = {
                        isRotating = true
                        when (mode) {
                            DashboardMode.HOT_SEARCH -> hotViewModel.refreshActivePlatform()
                            DashboardMode.WEATHER_ALERT -> weatherAlertViewModel.refresh(force = true)
                            DashboardMode.OIL_PRICE -> oilViewModel.refresh()
                            DashboardMode.GOLD_PRICE -> goldViewModel.refresh()
                            DashboardMode.NEWS_60S -> news60sViewModel.refresh()
                        }
                    },
                    rotationAngle = rotationAngle
                )

                when (mode) {
                    DashboardMode.NEWS_60S -> {
                        News60sContent(news60sState = news60sState, onRefresh = { news60sViewModel.refresh() })
                    }
                    DashboardMode.WEATHER_ALERT -> {
                        WeatherAlertContent(
                            state = weatherAlertState,
                            onRefresh = { weatherAlertViewModel.refresh(force = true) },
                            onQuery = { weatherAlertViewModel.queryCity(it) }
                        )
                    }
                    DashboardMode.HOT_SEARCH -> {
                        PlatformCategoryBar(
                            selectedCategory = selectedPlatformCategory,
                            onSelected = { selectedPlatformCategory = it }
                        )

                        PlatformsBar(
                            activePlatform = activePlatform,
                            platforms = HotPlatform.platformsByCategory(selectedPlatformCategory),
                            onSelected = { hotViewModel.selectPlatform(it) }
                        )

                        SearchSection(
                            query = searchQuery,
                            onQueryChanged = { hotViewModel.updateSearchQuery(it) },
                            platform = activePlatform
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            when (val state = uiState) {
                                is UiState.Loading -> {
                                    LoadingStateView(platform = activePlatform)
                                }
                                is UiState.Success -> {
                                    SuccessStateView(
                                        platform = activePlatform,
                                        items = state.items,
                                        updateTime = state.updateTime,
                                        onItemClicked = { item ->
                                            if (item.url != null) {
                                                previewUrl = item.url
                                                previewTitle = item.title
                                            }
                                        },
                                        onCopyItem = { item ->
                                            copyToClipboard(context, item.title + " " + (item.url ?: ""))
                                        },
                                        onShareItem = { item ->
                                            shareText(context, "\u3010\u805A\u5408\u70ED\u641C\u00B7${activePlatform.displayName}\u3011${item.title}\uFF1A${item.url ?: ""}")
                                        }
                                    )
                                }
                                is UiState.Error -> {
                                    ErrorStateView(
                                        message = state.message,
                                        onRetry = { hotViewModel.refreshActivePlatform() },
                                        platform = activePlatform
                                    )
                                }
                            }
                        }
                    }
                    DashboardMode.OIL_PRICE -> {
                        OilPriceContent(
                            oilState = oilState,
                            selectedProvince = selectedProvince,
                            onProvinceSelected = { oilViewModel.selectProvince(it) },
                            onRefresh = { oilViewModel.refresh() }
                        )
                    }
                    DashboardMode.GOLD_PRICE -> {
                        GoldPriceContent(
                            goldState = goldState,
                            onRefresh = { goldViewModel.refresh() }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = previewUrl != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 350)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut()
            ) {
                if (previewUrl != null) {
                    InAppBrowserPreview(
                        url = previewUrl!!,
                        title = previewTitle ?: "\u70ED\u641C\u8BE6\u60C5",
                        brandColor = activePlatform.brandColor,
                        onClose = {
                            previewUrl = null
                            previewTitle = null
                        }
                    )
                }
            }

        }
    }
}

@Composable
fun HeaderSection(
    activePlatform: HotPlatform,
    mode: DashboardMode,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onModeChange: (DashboardMode) -> Unit,
    onRefresh: () -> Unit,
    rotationAngle: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u805A\u5408\u667A\u8BAF",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(mode.accentColor(activePlatform).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = mode.badge(activePlatform),
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = mode.title,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        painter = painterResource(if (isDarkTheme) R.drawable.ic_light_mode else R.drawable.ic_dark_mode),
                        contentDescription = if (isDarkTheme) "切换浅色主题" else "切换深色主题",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "\u5237\u65B0",
                        tint = mode.accentColor(activePlatform),
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeToggle(
                mode = mode,
                onModeChange = onModeChange
            )
        }
    }
}

@Composable
fun ModeToggle(
    mode: DashboardMode,
    onModeChange: (DashboardMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup()
                .padding(horizontal = 3.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardMode.entries.forEach { option ->
                ModeToggleItem(
                    option = option,
                    selected = mode == option,
                    onSelected = { onModeChange(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModeToggleItem(
    option: DashboardMode,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) option.color else Color.Transparent,
        animationSpec = tween(durationMillis = 200)
    )
    val content by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200)
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .selectable(
                selected = selected,
                onClick = onSelected,
                role = Role.Tab
            )
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = option.shortLabel,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
