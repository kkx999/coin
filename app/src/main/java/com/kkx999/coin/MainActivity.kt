package com.kkx999.coin

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.kkx999.coin.data.BinanceMarketClient
import com.kkx999.coin.data.LiveQuote
import java.util.Locale
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { CoinApp() }
    }
}

private val Dark = darkColorScheme(
    background = Color(0xFF070A10),
    surface = Color(0xFF0D121C),
    surfaceVariant = Color(0xFF151C29),
    primary = Color(0xFF72AFFF),
    onBackground = Color(0xFFF5F7FB),
    onSurface = Color(0xFFF5F7FB)
)
private val Light = lightColorScheme(
    background = Color(0xFFF4F7FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EEF7),
    primary = Color(0xFF236FDB),
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827)
)

private data class CoinMeta(val symbol: String, val ticker: String, val name: String, val fallback: Double)
private val coins = listOf(
    CoinMeta("BTCUSDT", "BTC", "Bitcoin", 84936.70),
    CoinMeta("ETHUSDT", "ETH", "Ethereum", 2685.21),
    CoinMeta("SOLUSDT", "SOL", "Solana", 119.76),
    CoinMeta("BNBUSDT", "BNB", "BNB", 789.26),
    CoinMeta("XRPUSDT", "XRP", "XRP", 1.49),
    CoinMeta("DOGEUSDT", "DOGE", "Dogecoin", 0.0931)
)
private enum class Tab(val title: String) { Market("行情"), Watch("自选"), Settings("设置") }

private object Prefs {
    private const val P = "coin"
    fun favorites(c: Context) = c.getSharedPreferences(P, 0)
        .getStringSet("favorites", setOf("BTCUSDT", "ETHUSDT", "SOLUSDT"))?.toSet() ?: emptySet()
    fun favorites(c: Context, value: Set<String>) =
        c.getSharedPreferences(P, 0).edit().putStringSet("favorites", value).apply()
    fun bool(c: Context, key: String, default: Boolean) =
        c.getSharedPreferences(P, 0).getBoolean(key, default)
    fun bool(c: Context, key: String, value: Boolean) =
        c.getSharedPreferences(P, 0).edit().putBoolean(key, value).apply()
}

@Composable
private fun CoinApp() {
    val context = LocalContext.current
    val main = remember { Handler(Looper.getMainLooper()) }
    var tab by rememberSaveable { mutableStateOf(Tab.Market) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var connected by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(Prefs.favorites(context)) }
    var dark by rememberSaveable { mutableStateOf(Prefs.bool(context, "dark", true)) }
    var haptics by rememberSaveable { mutableStateOf(Prefs.bool(context, "haptics", true)) }
    var invert by rememberSaveable { mutableStateOf(Prefs.bool(context, "invert", false)) }

    val quotes = remember {
        mutableStateMapOf<String, LiveQuote>().apply {
            val change = listOf(0.18, 0.06, 0.20, 2.10, -0.17, -1.28)
            coins.forEachIndexed { i, c ->
                this[c.symbol] = LiveQuote(c.symbol, c.fallback, change[i], c.fallback * 1.02, c.fallback * .98, c.fallback * 1_000_000)
            }
        }
    }
    val history = remember {
        mutableStateMapOf<String, List<Float>>().apply { coins.forEach { this[it.symbol] = listOf(it.fallback.toFloat()) } }
    }

    DisposableEffect(Unit) {
        val socket = BinanceMarketClient.connect(
            onConnected = { main.post { connected = true } },
            onQuote = { q -> main.post {
                quotes[q.symbol] = q
                history[q.symbol] = (history[q.symbol].orEmpty() + q.price.toFloat()).takeLast(48)
            }},
            onDisconnected = { main.post { connected = false } }
        )
        onDispose { socket.close(1000, "Coin closed") }
    }

    MaterialTheme(if (dark) Dark else Light) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AnimatedContent(
                targetState = detail,
                transitionSpec = {
                    if (targetState != null)
                        (slideInHorizontally(tween(320)) { it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally(tween(220)) { -it / 5 } + fadeOut())
                    else
                        (slideInHorizontally(tween(280)) { -it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally(tween(220)) { it / 4 } + fadeOut())
                },
                label = "detail"
            ) { symbol ->
                if (symbol == null) {
                    Box(Modifier.fillMaxSize()) {
                        Crossfade(tab, animationSpec = tween(180), label = "tabs") { selected ->
                            when (selected) {
                                Tab.Market -> MarketScreen(quotes, history, connected, favorites, haptics, invert,
                                    onFavorite = { s ->
                                        favorites = if (s in favorites) favorites - s else favorites + s
                                        Prefs.favorites(context, favorites)
                                    },
                                    onOpen = { detail = it })
                                Tab.Watch -> WatchScreen(quotes, history, favorites, haptics, invert,
                                    onFavorite = { s ->
                                        favorites = favorites - s
                                        Prefs.favorites(context, favorites)
                                    },
                                    onOpen = { detail = it })
                                Tab.Settings -> SettingsScreen(
                                    connected, dark, haptics, invert,
                                    onDark = { dark = it; Prefs.bool(context, "dark", it) },
                                    onHaptics = { haptics = it; Prefs.bool(context, "haptics", it) },
                                    onInvert = { invert = it; Prefs.bool(context, "invert", it) }
                                )
                            }
                        }
                        GlassBar(tab, haptics, { tab = it }, Modifier.align(Alignment.BottomCenter))
                    }
                } else {
                    val q = quotes[symbol] ?: return@AnimatedContent
                    val meta = coins.first { it.symbol == symbol }
                    DetailScreen(meta, q, history[symbol].orEmpty(), symbol in favorites, haptics, invert,
                        onBack = { detail = null },
                        onFavorite = {
                            favorites = if (symbol in favorites) favorites - symbol else favorites + symbol
                            Prefs.favorites(context, favorites)
                        })
                }
            }
        }
    }
}

@Composable
private fun MarketScreen(
    quotes: Map<String, LiveQuote>,
    history: Map<String, List<Float>>,
    connected: Boolean,
    favorites: Set<String>,
    haptics: Boolean,
    invert: Boolean,
    onFavorite: (String) -> Unit,
    onOpen: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 118.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Coin", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveDot(connected); Spacer(Modifier.width(7.dp))
                        Text(if (connected) "实时行情" else "连接行情中", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(.52f))
                    }
                }
                Icon(Icons.Rounded.ShowChart, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text("搜索 BTC / ETH / SOL") }
            )
        }
        item { Hero(quotes["BTCUSDT"], quotes["ETHUSDT"], connected, invert) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("市场", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("24H", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.42f))
            }
        }
        items(coins.filter { query.isBlank() || it.ticker.contains(query, true) || it.name.contains(query, true) }, key = { it.symbol }) { meta ->
            val q = quotes[meta.symbol] ?: return@items
            CoinCard(meta, q, history[meta.symbol].orEmpty(), meta.symbol in favorites, haptics, invert,
                { onFavorite(meta.symbol) }, { onOpen(meta.symbol) })
        }
    }
}

@Composable
private fun WatchScreen(
    quotes: Map<String, LiveQuote>,
    history: Map<String, List<Float>>,
    favorites: Set<String>,
    haptics: Boolean,
    invert: Boolean,
    onFavorite: (String) -> Unit,
    onOpen: (String) -> Unit
) {
    val list = coins.filter { it.symbol in favorites }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(18.dp, 20.dp, 18.dp, 118.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("自选", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Text("${list.size} 个币种 · 本地保存", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(.5f))
        }
        if (list.isEmpty()) item {
            Glass {
                Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.StarBorder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(10.dp)); Text("还没有自选", fontWeight = FontWeight.SemiBold)
                    Text("在行情或详情页点星标即可加入", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.46f))
                }
            }
        }
        items(list, key = { it.symbol }) { meta ->
            val q = quotes[meta.symbol] ?: return@items
            CoinCard(meta, q, history[meta.symbol].orEmpty(), true, haptics, invert,
                { onFavorite(meta.symbol) }, { onOpen(meta.symbol) })
        }
    }
}

@Composable
private fun SettingsScreen(
    connected: Boolean, dark: Boolean, haptics: Boolean, invert: Boolean,
    onDark: (Boolean) -> Unit, onHaptics: (Boolean) -> Unit, onInvert: (Boolean) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(18.dp, 20.dp, 18.dp, 118.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("设置", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Text("简单、轻量、不需要登录", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(.5f))
        }
        item { Glass { Column {
            SwitchRow("深色模式", "默认使用深色行情界面", dark, onDark)
            DividerLine()
            SwitchRow("触感反馈", "切换、收藏和进入详情时反馈", haptics, onHaptics)
            DividerLine()
            SwitchRow("涨红跌绿", "关闭时为涨绿跌红", invert, onInvert)
        }}}
        item { Glass { Column {
            Info("行情源", "Binance Public API"); DividerLine()
            Info("实时连接", if (connected) "已连接" else "连接中"); DividerLine()
            Info("账户", "无需登录")
        }}}
        item { Glass { Column {
            Info("版本", "0.0.1"); DividerLine(); Info("阶段", "Early Preview")
        }}}
    }
}

@Composable
private fun DetailScreen(
    meta: CoinMeta, q: LiveQuote, fallback: List<Float>, favorite: Boolean,
    haptics: Boolean, invert: Boolean, onBack: () -> Unit, onFavorite: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val main = remember { Handler(Looper.getMainLooper()) }
    var period by rememberSaveable(meta.symbol) { mutableStateOf("1D") }
    var data by remember(meta.symbol) { mutableStateOf(fallback) }
    var loading by remember(meta.symbol) { mutableStateOf(true) }
    val periods = linkedMapOf("1H" to ("1m" to 60), "1D" to ("15m" to 96), "1W" to ("1h" to 168), "1M" to ("4h" to 180), "3M" to ("1d" to 90))

    LaunchedEffect(meta.symbol, period) {
        loading = true
        val c = periods.getValue(period)
        BinanceMarketClient.fetchKlines(meta.symbol, c.first, c.second,
            onResult = { main.post { data = it; loading = false } },
            onError = { main.post { data = fallback; loading = false } })
    }
    val positive = q.changePercent24h >= 0
    val accent = changeColor(positive, invert)

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onBack() }) {
                    Icon(Icons.Rounded.ArrowBack, "返回")
                }
                Column(Modifier.weight(1f)) {
                    Text(meta.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("${meta.ticker} / USDT", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.46f))
                }
                IconButton({ if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); onFavorite() }) {
                    Icon(if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, "自选",
                        tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.55f))
                }
            }
        }
        item {
            Column(Modifier.padding(vertical = 4.dp)) {
                Crossfade(q.price, animationSpec = tween(160), label = "price") { Text("$${price(it)}", fontSize = 36.sp, fontWeight = FontWeight.SemiBold) }
                Text(percent(q.changePercent24h), color = accent, fontWeight = FontWeight.Medium)
            }
        }
        item {
            Glass {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        Text("价格走势", fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f))
                        Text(if (loading) "加载中" else "实时数据", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.42f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Spark(if (data.size > 1) data else fallback, positive, invert, Modifier.fillMaxWidth().height(210.dp), 3.dp)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        periods.keys.forEach { p -> PeriodChip(p, period == p) {
                            if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); period = p
                        }}
                    }
                }
            }
        }
        item { Text("24H 数据", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("最高", "$${price(q.high24h)}", Modifier.weight(1f))
            Stat("最低", "$${price(q.low24h)}", Modifier.weight(1f))
        }}
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("成交额", money(q.quoteVolume24h), Modifier.weight(1f))
            Stat("涨跌幅", percent(q.changePercent24h), Modifier.weight(1f), accent)
        }}
        item { Glass {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column { Text("公开行情", fontWeight = FontWeight.Medium)
                    Text("无需 API Key，不涉及交易和账户", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.46f)) }
            }
        }}
    }
}

@Composable
private fun CoinCard(
    meta: CoinMeta, q: LiveQuote, points: List<Float>, favorite: Boolean,
    haptics: Boolean, invert: Boolean, onFavorite: () -> Unit, onOpen: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .985f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "press")
    val positive = q.changePercent24h >= 0
    Surface(
        modifier = Modifier.fillMaxWidth().scale(scale).clip(RoundedCornerShape(24.dp))
            .clickable(source, indication = null) {
                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onOpen()
            },
        color = MaterialTheme.colorScheme.surface.copy(.92f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Badge(meta.ticker); Spacer(Modifier.width(11.dp))
            Column(Modifier.width(78.dp)) {
                Text(meta.ticker, fontWeight = FontWeight.SemiBold)
                Text(meta.name, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(.4f))
            }
            Spark(points, positive, invert, Modifier.weight(1f).height(38.dp).padding(horizontal = 8.dp), 2.dp)
            Column(horizontalAlignment = Alignment.End) {
                Crossfade(q.price, animationSpec = tween(150), label = "row-price") { Text("$${price(it)}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                Text(percent(q.changePercent24h), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = changeColor(positive, invert))
            }
            IconButton({
                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); onFavorite()
            }, Modifier.size(34.dp)) {
                Icon(if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, "自选",
                    modifier = Modifier.size(19.dp),
                    tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.3f))
            }
        }
    }
}

@Composable
private fun Hero(btc: LiveQuote?, eth: LiveQuote?, connected: Boolean, invert: Boolean) {
    Glass {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("市场概览", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.48f))
                Spacer(Modifier.weight(1f))
                Text(if (connected) "LIVE" else "SYNC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroQuote("BTC", btc, invert, Modifier.weight(1f)); HeroQuote("ETH", eth, invert, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroQuote(name: String, q: LiveQuote?, invert: Boolean, modifier: Modifier) {
    Column(modifier) {
        Text(name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.45f))
        Text("$${price(q?.price ?: 0.0)}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(percent(q?.changePercent24h ?: 0.0), fontSize = 12.sp, color = changeColor((q?.changePercent24h ?: 0.0) >= 0, invert))
    }
}

@Composable
private fun GlassBar(selected: Tab, haptics: Boolean, onSelected: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val tabs = Tab.entries
    BoxWithConstraints(
        modifier.navigationBarsPadding().padding(start = 18.dp, end = 18.dp, bottom = 14.dp)
            .fillMaxWidth().height(68.dp).shadow(20.dp, RoundedCornerShape(34.dp))
            .clip(RoundedCornerShape(34.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(.15f), MaterialTheme.colorScheme.surface.copy(.94f))))
            .border(1.dp, Color.White.copy(.11f), RoundedCornerShape(34.dp))
    ) {
        val width = maxWidth / tabs.size.toFloat()
        val x by animateDpAsState(width * tabs.indexOf(selected), spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "indicator")
        Box(Modifier.offset(x = x).width(width).fillMaxHeight().padding(7.dp).clip(RoundedCornerShape(27.dp))
            .background(MaterialTheme.colorScheme.primary.copy(.12f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(.14f), RoundedCornerShape(27.dp)))
        Row(Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                val source = remember { MutableInteractionSource() }
                val pressed by source.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) .92f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "nav")
                val active = tab == selected
                Column(Modifier.weight(1f).fillMaxHeight().scale(scale).clickable(source, indication = null) {
                    if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelected(tab)
                }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(when (tab) {
                        Tab.Market -> Icons.Rounded.ShowChart
                        Tab.Watch -> if (active) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
                        Tab.Settings -> Icons.Rounded.Settings
                    }, tab.title, Modifier.size(20.dp),
                        tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.45f))
                    Spacer(Modifier.height(3.dp))
                    Text(tab.title, fontSize = 11.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(.45f))
                }
            }
        }
    }
}

@Composable
private fun Glass(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
        .background(Brush.verticalGradient(listOf(Color.White.copy(.075f), MaterialTheme.colorScheme.surface.copy(.9f))))
        .border(1.dp, Color.White.copy(.075f), RoundedCornerShape(26.dp))) { content() }
}

@Composable
private fun Badge(ticker: String) {
    val c = when (ticker) {
        "BTC" -> Color(0xFFF1B44C); "ETH" -> Color(0xFF8798C8); "SOL" -> Color(0xFF7D72E8)
        "BNB" -> Color(0xFFF0BF42); "XRP" -> Color(0xFF68758B); else -> Color(0xFF72AFFF)
    }
    Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c, c.copy(.55f)))), contentAlignment = Alignment.Center) {
        Text(ticker.take(1), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Spark(points: List<Float>, positive: Boolean, invert: Boolean, modifier: Modifier, width: Dp) {
    val c = changeColor(positive, invert)
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val min = points.minOrNull() ?: return@Canvas
        val max = points.maxOrNull() ?: return@Canvas
        val spread = (max - min).takeIf { it > 0f } ?: 1f
        val step = size.width / (points.size - 1)
        val p = Path()
        points.forEachIndexed { i, v ->
            val x = step * i
            val y = size.height - ((v - min) / spread) * size.height
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        drawPath(p, c, style = Stroke(width.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun PeriodChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .9f else 1f, label = "period")
    Box(Modifier.scale(scale).clip(RoundedCornerShape(14.dp))
        .background(if (selected) MaterialTheme.colorScheme.primary.copy(.14f) else Color.Transparent)
        .clickable(source, indication = null, onClick = onClick).padding(horizontal = 9.dp, vertical = 7.dp)) {
        Text(text, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.46f))
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface.copy(.92f))
        .border(1.dp, Color.White.copy(.06f), RoundedCornerShape(22.dp)).padding(16.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.44f))
        Spacer(Modifier.height(7.dp)); Text(value, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp, 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.44f)) }
        Switch(checked, onChecked)
    }
}

@Composable
private fun Info(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp, 15.dp)) {
        Text(label, fontWeight = FontWeight.Medium); Spacer(Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(.48f))
    }
}

@Composable private fun DividerLine() =
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(.055f)))

@Composable
private fun LiveDot(connected: Boolean) {
    val c by animateColorAsState(if (connected) Color(0xFF6DD5A8) else Color(0xFF8690A2), tween(250), label = "live")
    Box(Modifier.size(7.dp).clip(CircleShape).background(c))
}

@Composable
private fun changeColor(positive: Boolean, invert: Boolean): Color {
    val up = if (invert) Color(0xFFFF6178) else Color(0xFF57CF9A)
    val down = if (invert) Color(0xFF57CF9A) else Color(0xFFFF6178)
    return if (positive) up else down
}

private fun price(v: Double) = when {
    v >= 1000 -> String.format(Locale.US, "%,.2f", v)
    v >= 1 -> String.format(Locale.US, "%.2f", v)
    v >= .01 -> String.format(Locale.US, "%.4f", v)
    else -> String.format(Locale.US, "%.6f", v)
}
private fun percent(v: Double) = String.format(Locale.US, "%+.2f%%", v)
private fun money(v: Double): String {
    val a = v.absoluteValue
    return when {
        a >= 1e9 -> String.format(Locale.US, "$%.2fB", v / 1e9)
        a >= 1e6 -> String.format(Locale.US, "$%.2fM", v / 1e6)
        a >= 1e3 -> String.format(Locale.US, "$%.2fK", v / 1e3)
        else -> String.format(Locale.US, "$%.2f", v)
    }
}
