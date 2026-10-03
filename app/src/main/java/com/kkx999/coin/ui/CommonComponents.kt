package com.kkx999.coin.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkx999.coin.data.LiveQuote
import java.util.Locale

@Composable
internal fun GlassPanel(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color.Black.copy(.07f),
                spotColor = Color.Black.copy(.07f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(.88f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(.055f),
                RoundedCornerShape(22.dp)
            )
            .padding(padding)
    ) {
        content()
    }
}

@Composable
internal fun CoinBadge(meta: CoinMeta, size: Dp = 42.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(meta.badgeA, meta.badgeB)))
            .border(1.dp, Color.White.copy(.17f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            meta.ticker.take(1),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * .34f).sp
        )
    }
}

@Composable
internal fun LiveDot(connected: Boolean) {
    val color by animateColorAsState(
        if (connected) UpGreen else Color(0xFFFFB24A),
        label = "连接状态"
    )
    Box(Modifier.size(7.dp).clip(CircleShape).background(color))
}

@Composable
internal fun StatusCapsule(connected: Boolean) {
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(.76f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(.055f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiveDot(connected)
        Spacer(Modifier.width(6.dp))
        Text(
            if (connected) "实时" else "连接中",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(.62f)
        )
    }
}

@Composable
internal fun Sparkline(points: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val minV = points.minOrNull() ?: return@Canvas
        val maxV = points.maxOrNull() ?: return@Canvas
        val range = (maxV - minV).coerceAtLeast(.0001f)
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = size.width * index / points.lastIndex.coerceAtLeast(1)
            val y = size.height - ((value - minV) / range) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            color.copy(.88f),
            style = Stroke(1.55.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
internal fun CoinCard(
    meta: CoinMeta,
    quote: LiveQuote,
    points: List<Float>,
    favorite: Boolean,
    haptics: Boolean,
    invert: Boolean,
    onFavorite: () -> Unit,
    onOpen: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) .982f else 1f,
        spring(stiffness = Spring.StiffnessMediumLow),
        label = "行情卡片按压"
    )
    val haptic = LocalHapticFeedback.current
    val positive = quote.changePercent24h >= 0
    val accent = priceColor(positive, invert)

    Box(
        Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                7.dp,
                RoundedCornerShape(22.dp),
                ambientColor = Color.Black.copy(.075f),
                spotColor = Color.Black.copy(.075f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(.90f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(.055f),
                RoundedCornerShape(22.dp)
            )
            .clickable(interactionSource = interaction, indication = null) {
                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpen()
            }
            .padding(horizontal = 13.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinBadge(meta)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.width(72.dp)) {
                Text(meta.ticker, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    meta.chineseName,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(.40f)
                )
            }
            Sparkline(
                points = points,
                color = accent,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .padding(horizontal = 7.dp)
            )
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(92.dp)) {
                Text(
                    formatPrice(quote.price),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${if (positive) "+" else ""}${"%.2f".format(Locale.US, quote.changePercent24h)}%",
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.width(5.dp))
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFavorite()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (favorite) androidx.compose.material.icons.rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "自选",
                    tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.28f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(.15f) else MaterialTheme.colorScheme.surface.copy(.75f),
        label = "筛选背景"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.50f),
        label = "筛选文字"
    )
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary.copy(.22f) else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            color = fg,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
internal fun GlassBottomBar(
    selected: MainTab,
    haptics: Boolean,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier
            .padding(horizontal = 18.dp)
            .navigationBarsPadding()
            .padding(bottom = 9.dp)
            .shadow(
                24.dp,
                RoundedCornerShape(31.dp),
                ambientColor = Color.Black.copy(.20f),
                spotColor = Color.Black.copy(.20f)
            )
            .clip(RoundedCornerShape(31.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(.96f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(.86f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(.11f), RoundedCornerShape(31.dp))
            .padding(7.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MainTab.entries.forEach { tab ->
                BottomItem(
                    tab = tab,
                    selected = tab == selected,
                    modifier = Modifier.weight(1f)
                ) {
                    if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(tab)
                }
            }
        }
    }
}

@Composable
private fun BottomItem(
    tab: MainTab,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(.16f) else Color.Transparent,
        animationSpec = tween(210),
        label = "底栏选中背景"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.46f),
        animationSpec = tween(180),
        label = "底栏前景"
    )
    val scale by animateFloatAsState(
        if (selected) 1f else .96f,
        spring(stiffness = Spring.StiffnessMediumLow),
        label = "底栏缩放"
    )
    val icon = when (tab) {
        MainTab.Market -> Icons.Rounded.ShowChart
        MainTab.Watch -> Icons.Rounded.StarBorder
        MainTab.Settings -> Icons.Rounded.Settings
    }

    Column(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, tab.title, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            tab.title,
            color = fg,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
