package com.kkx999.coin

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.kkx999.coin.data.BinanceMarketClient
import com.kkx999.coin.data.Candle
import com.kkx999.coin.data.LiveQuote
import com.kkx999.coin.data.MarketTrade
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { CoinApp() }
    }
}

private val Dark = darkColorScheme(
    background = Color(0xFF05070B),
    surface = Color(0xFF0B1018),
    surfaceVariant = Color(0xFF111925),
    primary = Color(0xFF68A8FF),
    secondary = Color(0xFF9CC6FF),
    onBackground = Color(0xFFF4F7FB),
    onSurface = Color(0xFFF4F7FB)
)

private val Light = lightColorScheme(
    background = Color(0xFFF2F5FA),
    surface = Color(0xFFFBFCFE),
    surfaceVariant = Color(0xFFE8EEF7),
    primary = Color(0xFF236FDA),
    secondary = Color(0xFF4E8EE8),
    onBackground = Color(0xFF121723),
    onSurface = Color(0xFF121723)
)

private val Up = Color(0xFF2CCB8C)
private val Down = Color(0xFFFF5E75)
private val Gold = Color(0xFFF3C65A)

private data class CoinMeta(val symbol: String, val ticker: String, val name: String)
private data class ChartPeriod(val title: String, val interval: String, val limit: Int)

private val coins = listOf(
    CoinMeta("BTCUSDT", "BTC", "比特币"),
    CoinMeta("ETHUSDT", "ETH", "以太坊"),
    CoinMeta("SOLUSDT", "SOL", "索拉纳"),
    CoinMeta("BNBUSDT", "BNB", "币安币"),
    CoinMeta("XRPUSDT", "XRP", "瑞波币"),
    CoinMeta("DOGEUSDT", "DOGE", "狗狗币"),
    CoinMeta("ADAUSDT", "ADA", "艾达币"),
    CoinMeta("AVAXUSDT", "AVAX", "雪崩"),
    CoinMeta("LINKUSDT", "LINK", "Chainlink"),
    CoinMeta("DOTUSDT", "DOT", "波卡"),
    CoinMeta("LTCUSDT", "LTC", "莱特币"),
    CoinMeta("TRXUSDT", "TRX", "波场")
)

private enum class Tab(val title: String) { Market("行情"), Watch("自选"), Settings("设置") }
private enum class MarketFilter(val title: String) { All("全部"), Gainers("涨幅"), Losers("跌幅") }

private object Prefs {
    private const val P = "coin"
    fun favorites(c: Context) = c.getSharedPreferences(P, 0)
        .getStringSet("favorites", setOf("BTCUSDT", "ETHUSDT", "SOLUSDT"))?.toSet() ?: emptySet()
    fun favorites(c: Context, value: Set<String>) =
        c.getSharedPreferences(P, 0).edit().putStringSet("favorites", value).apply()
    fun getBool(c: Context, key: String, default: Boolean) =
        c.getSharedPreferences(P, 0).getBoolean(key, default)
    fun setBool(c: Context, key: String, value: Boolean) =
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
    var dark by rememberSaveable { mutableStateOf(Prefs.getBool(context, "dark", true)) }
    var haptics by rememberSaveable { mutableStateOf(Prefs.getBool(context, "haptics", true)) }
    var invert by rememberSaveable { mutableStateOf(Prefs.getBool(context, "invert", false)) }

    val quotes = remember {
        mutableStateMapOf<String, LiveQuote>().apply {
            coins.forEach { c -> this[c.symbol] = LiveQuote(c.symbol, 0.0, 0.0, 0.0, 0.0, 0.0) }
        }
    }
    val history = remember {
        mutableStateMapOf<String, List<Float>>().apply { coins.forEach { this[it.symbol] = emptyList() } }
    }

    DisposableEffect(Unit) {
        val socket = BinanceMarketClient.connectMarket(
            symbols = coins.map { it.symbol },
            onConnected = { main.post { connected = true } },
            onQuote = { q ->
                main.post {
                    quotes[q.symbol] = q
                    history[q.symbol] = (history[q.symbol].orEmpty() + q.price.toFloat()).takeLast(60)
                }
            },
            onDisconnected = { main.post { connected = false } }
        )
        onDispose { socket.close(1000, "关闭行情") }
    }

    BackHandler(enabled = detail != null || tab != Tab.Market) {
        if (detail != null) detail = null else tab = Tab.Market
    }

    MaterialTheme(if (dark) Dark else Light) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AnimatedContent(
                targetState = detail,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(220)) { -it / 6 } + fadeOut(tween(160)))
                    } else {
                        (slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(220)) { it / 5 } + fadeOut(tween(160)))
                    }
                },
                label = "详情切换"
            ) { symbol ->
                if (symbol == null) {
                    Box(Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = {
                                val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                                (slideInHorizontally(tween(260)) { it / 7 * direction } + fadeIn(tween(180))) togetherWith
                                    (slideOutHorizontally(tween(200)) { -it / 10 * direction } + fadeOut(tween(150)))
                            },
                            label = "底部页面"
                        ) { selected ->
                            when (selected) {
                                Tab.Market -> MarketScreen(
                                    quotes = quotes,
                                    history = history,
                                    connected = connected,
                                    favorites = favorites,
                                    haptics = haptics,
                                    invert = invert,
                                    onFavorite = { s ->
                                        favorites = if (s in favorites) favorites - s else favorites + s
                                        Prefs.favorites(context, favorites)
                                    },
                                    onOpen = { detail = it }
                                )
                                Tab.Watch -> WatchScreen(
                                    quotes = quotes,
                                    history = history,
                                    favorites = favorites,
                                    haptics = haptics,
                                    invert = invert,
                                    onFavorite = { s ->
                                        favorites = favorites - s
                                        Prefs.favorites(context, favorites)
                                    },
                                    onOpen = { detail = it }
                                )
                                Tab.Settings -> SettingsScreen(
                                    connected = connected,
                                    dark = dark,
                                    haptics = haptics,
                                    invert = invert,
                                    onDark = { dark = it; Prefs.setBool(context, "dark", it) },
                                    onHaptics = { haptics = it; Prefs.setBool(context, "haptics", it) },
                                    onInvert = { invert = it; Prefs.setBool(context, "invert", it) }
                                )
                            }
                        }
                        LiquidGlassBar(tab, haptics, { tab = it }, Modifier.align(Alignment.BottomCenter))
                    }
                } else {
                    val q = quotes[symbol] ?: LiveQuote(symbol, 0.0, 0.0, 0.0, 0.0, 0.0)
                    val meta = coins.firstOrNull { it.symbol == symbol } ?: return@AnimatedContent
                    DetailScreen(
                        meta = meta,
                        quote = q,
                        favorite = symbol in favorites,
                        haptics = haptics,
                        invert = invert,
                        onBack = { detail = null },
                        onFavorite = {
                            favorites = if (symbol in favorites) favorites - symbol else favorites + symbol
                            Prefs.favorites(context, favorites)
                        }
                    )
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
    var searching by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(MarketFilter.All) }

    val filtered = coins
        .filter { query.isBlank() || it.ticker.contains(query, true) || it.name.contains(query, true) }
        .let { list ->
            when (filter) {
                MarketFilter.All -> list
                MarketFilter.Gainers -> list.sortedByDescending { quotes[it.symbol]?.changePercent24h ?: 0.0 }
                MarketFilter.Losers -> list.sortedBy { quotes[it.symbol]?.changePercent24h ?: 0.0 }
            }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("币行情", fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveDot(connected)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (connected) "实时行情已连接" else "正在连接实时行情",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(.52f)
                        )
                    }
                }
                SoftIconButton(if (searching) Icons.Rounded.Close else Icons.Rounded.Search) {
                    searching = !searching
                    if (!searching) query = ""
                }
            }
        }

        item {
            AnimatedVisibility(searching) {
                PremiumSearchField(query = query, onQuery = { query = it })
            }
        }

        item { MarketOverview(quotes, connected, invert) }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("市场", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("24 小时", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.42f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MarketFilter.entries.forEach { item ->
                    FilterChip(item.title, filter == item) { filter = item }
                }
            }
        }

        items(filtered, key = { it.symbol }) { meta ->
            val q = quotes[meta.symbol] ?: return@items
            MarketRow(
                meta = meta,
                quote = q,
                points = history[meta.symbol].orEmpty(),
                favorite = meta.symbol in favorites,
                haptics = haptics,
                invert = invert,
                onFavorite = { onFavorite(meta.symbol) },
                onOpen = { onOpen(meta.symbol) }
            )
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
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 118.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("自选", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("${list.size} 个币种 · 仅保存在本机", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.48f))
        }
        if (list.isEmpty()) {
            item {
                PremiumCard {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.StarBorder, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        Text("还没有自选币种", fontWeight = FontWeight.SemiBold)
                        Text("在行情或详情页点击星标即可加入", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.46f))
                    }
                }
            }
        }
        items(list, key = { it.symbol }) { meta ->
            val q = quotes[meta.symbol] ?: return@items
            MarketRow(
                meta = meta,
                quote = q,
                points = history[meta.symbol].orEmpty(),
                favorite = true,
                haptics = haptics,
                invert = invert,
                onFavorite = { onFavorite(meta.symbol) },
                onOpen = { onOpen(meta.symbol) }
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    connected: Boolean,
    dark: Boolean,
    haptics: Boolean,
    invert: Boolean,
    onDark: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onInvert: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 118.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("设置", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("无账号、无登录、只看行情", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.48f))
        }
        item {
            SectionTitle("外观与交互")
            PremiumCard {
                Column {
                    SwitchRow("深色模式", "更适合长时间查看行情", dark, onDark)
                    DividerLine()
                    SwitchRow("触感反馈", "页面切换、收藏和按钮反馈", haptics, onHaptics)
                    DividerLine()
                    SwitchRow("涨红跌绿", "关闭时为涨绿跌红", invert, onInvert)
                }
            }
        }
        item {
            SectionTitle("行情数据")
            PremiumCard {
                Column {
                    InfoRow("行情源", "币安公开 API")
                    DividerLine()
                    InfoRow("实时连接", if (connected) "已连接" else "连接中")
                    DividerLine()
                    InfoRow("账户", "无需登录")
                }
            }
        }
        item {
            SectionTitle("关于")
            PremiumCard {
                Column {
                    InfoRow("应用", "币行情")
                    DividerLine()
                    InfoRow("版本", "0.0.2")
                    DividerLine()
                    InfoRow("数据用途", "仅用于公开行情展示")
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(
    meta: CoinMeta,
    quote: LiveQuote,
    favorite: Boolean,
    haptics: Boolean,
    invert: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit
) {
    val main = remember { Handler(Looper.getMainLooper()) }
    val haptic = LocalHapticFeedback.current
    val periods = remember {
        listOf(
            ChartPeriod("1分", "1m", 120),
            ChartPeriod("5分", "5m", 120),
            ChartPeriod("15分", "15m", 120),
            ChartPeriod("1时", "1h", 120),
            ChartPeriod("4时", "4h", 120),
            ChartPeriod("日线", "1d", 120)
        )
    }
    var selectedPeriod by rememberSaveable(meta.symbol) { mutableStateOf("15分") }
    var candles by remember(meta.symbol) { mutableStateOf<List<Candle>>(emptyList()) }
    var loading by remember(meta.symbol) { mutableStateOf(true) }
    var selectedIndex by remember(candles) { mutableStateOf<Int?>(null) }
    val trades = remember(meta.symbol) { mutableStateListOf<MarketTrade>() }

    LaunchedEffect(meta.symbol, selectedPeriod) {
        loading = true
        val p = periods.first { it.title == selectedPeriod }
        BinanceMarketClient.fetchCandles(
            symbol = meta.symbol,
            interval = p.interval,
            limit = p.limit,
            onResult = { data -> main.post { candles = data; selectedIndex = null; loading = false } },
            onError = { main.post { loading = false } }
        )
    }

    DisposableEffect(meta.symbol) {
        trades.clear()
        val socket = BinanceMarketClient.connectTrades(
            symbol = meta.symbol,
            onTrade = { trade ->
                main.post {
                    trades.add(0, trade)
                    if (trades.size > 28) trades.removeAt(trades.lastIndex)
                }
            }
        )
        onDispose { socket.close(1000, "关闭成交监控") }
    }

    val positive = quote.changePercent24h >= 0
    val accent = changeColor(positive, invert)
    val selectedCandle = candles.getOrNull(selectedIndex ?: candles.lastIndex.coerceAtLeast(0))

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 14.dp, top = 6.dp, end = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoftIconButton(Icons.Rounded.ArrowBack) {
                    if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBack()
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${meta.ticker} / USDT", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(meta.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.44f))
                }
                SoftIconButton(if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder) {
                    if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFavorite()
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Crossfade(quote.price, animationSpec = tween(150), label = "详情价格") { value ->
                    Text(priceText(value), fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(.12f)) {
                        Text(percent(quote.changePercent24h), color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("24 小时", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.42f))
                }
            }
        }

        item {
            StatsStrip(quote, invert)
        }

        item {
            PremiumCard(corner = 20.dp) {
                Column(Modifier.padding(top = 14.dp, bottom = 12.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("K 线", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        LiveDot(!loading)
                        Spacer(Modifier.width(6.dp))
                        Text(if (loading) "加载中" else "实时", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.42f))
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        periods.forEach { p ->
                            PeriodTab(p.title, selectedPeriod == p.title) {
                                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedPeriod = p.title
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    CandleInfo(selectedCandle, invert)
                    Spacer(Modifier.height(8.dp))

                    if (candles.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                            Text(if (loading) "正在加载 K 线…" else "暂时无法获取 K 线", color = MaterialTheme.colorScheme.onSurface.copy(.45f), fontSize = 13.sp)
                        }
                    } else {
                        CandleChart(
                            candles = candles,
                            selectedIndex = selectedIndex,
                            invert = invert,
                            onSelect = { selectedIndex = it },
                            modifier = Modifier.fillMaxWidth().height(300.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(Color(0xFFF2C94C), "MA5")
                        LegendDot(Color(0xFF8A7CFF), "MA10")
                        LegendDot(Color(0xFF4CB7FF), "MA20")
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("实时成交", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                LiveDot(trades.isNotEmpty())
                Spacer(Modifier.width(6.dp))
                Text("逐笔监控", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.42f))
            }
        }

        item {
            PremiumCard(corner = 20.dp) {
                Column(Modifier.padding(vertical = 10.dp)) {
                    TradeHeader()
                    DividerLine(horizontal = 12.dp)
                    if (trades.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                            Text("等待实时成交数据…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.44f))
                        }
                    } else {
                        trades.take(16).forEach { trade -> TradeRow(trade, invert) }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("公开行情 · 无需 API Key · 不涉及交易账户", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.42f))
            }
        }
    }
}

@Composable
private fun MarketOverview(quotes: Map<String, LiveQuote>, connected: Boolean, invert: Boolean) {
    val btc = quotes["BTCUSDT"]
    val eth = quotes["ETHUSDT"]
    val mover = quotes.values.filter { it.price > 0.0 }.maxByOrNull { it.changePercent24h.absoluteValue }
    val moverMeta = coins.firstOrNull { it.symbol == mover?.symbol }

    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0C2347),
                        Color(0xFF0A1730),
                        Color(0xFF10141D)
                    )
                )
            )
            .border(1.dp, Color(0xFF6CAFFF).copy(.18f), RoundedCornerShape(26.dp))
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color(0xFF4C9CFF).copy(.08f), radius = size.width * .32f, center = Offset(size.width * .88f, size.height * .05f))
            drawCircle(Color.White.copy(.025f), radius = size.width * .22f, center = Offset(size.width * .18f, size.height * 1.08f))
        }
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("市场概览", color = Color.White.copy(.68f), fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(.08f)) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        LiveDot(connected)
                        Spacer(Modifier.width(6.dp))
                        Text(if (connected) "实时" else "同步中", color = Color.White.copy(.8f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                OverviewQuote("BTC", btc, invert, Modifier.weight(1f))
                OverviewQuote("ETH", eth, invert, Modifier.weight(1f))
                Column(Modifier.weight(1f)) {
                    Text("波动", color = Color.White.copy(.46f), fontSize = 10.sp)
                    Text(moverMeta?.ticker ?: "--", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        mover?.let { percent(it.changePercent24h) } ?: "--",
                        color = mover?.let { changeColor(it.changePercent24h >= 0, invert) } ?: Color.White.copy(.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewQuote(title: String, quote: LiveQuote?, invert: Boolean, modifier: Modifier) {
    Column(modifier) {
        Text(title, color = Color.White.copy(.46f), fontSize = 10.sp)
        Text(priceText(quote?.price ?: 0.0), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(
            if ((quote?.price ?: 0.0) > 0) percent(quote?.changePercent24h ?: 0.0) else "--",
            color = changeColor((quote?.changePercent24h ?: 0.0) >= 0, invert),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MarketRow(
    meta: CoinMeta,
    quote: LiveQuote,
    points: List<Float>,
    favorite: Boolean,
    haptics: Boolean,
    invert: Boolean,
    onFavorite: () -> Unit,
    onOpen: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .985f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "行情卡片")
    val positive = quote.changePercent24h >= 0

    Box(
        Modifier.fillMaxWidth().scale(scale).clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(.88f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(.055f), RoundedCornerShape(22.dp))
            .clickable(source, indication = null) {
                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpen()
            }
            .padding(horizontal = 13.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinBadge(meta.ticker)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.width(74.dp)) {
                Text(meta.ticker, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(meta.name, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(.40f))
            }
            MiniSpark(points, positive, invert, Modifier.weight(1f).height(42.dp).padding(horizontal = 8.dp))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(86.dp)) {
                Crossfade(quote.price, animationSpec = tween(140), label = "行情价格") { value ->
                    Text(priceText(value), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = changeColor(positive, invert).copy(.10f)) {
                    Text(
                        if (quote.price > 0) percent(quote.changePercent24h) else "--",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = changeColor(positive, invert)
                    )
                }
            }
            IconButton(
                onClick = {
                    if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFavorite()
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    "自选",
                    modifier = Modifier.size(18.dp),
                    tint = if (favorite) Gold else MaterialTheme.colorScheme.onSurface.copy(.26f)
                )
            }
        }
    }
}

@Composable
private fun CandleChart(
    candles: List<Candle>,
    selectedIndex: Int?,
    invert: Boolean,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val grid = MaterialTheme.colorScheme.onSurface.copy(.075f)
    val up = changeColor(true, invert)
    val down = changeColor(false, invert)
    val ma5 = Color(0xFFF2C94C)
    val ma10 = Color(0xFF8A7CFF)
    val ma20 = Color(0xFF4CB7FF)
    val maxPrice = candles.maxOfOrNull { it.high } ?: 1.0
    val minPrice = candles.minOfOrNull { it.low } ?: 0.0
    val maxVolume = candles.maxOfOrNull { it.volume }?.coerceAtLeast(1e-9) ?: 1.0

    Canvas(
        modifier.pointerInput(candles) {
            fun update(x: Float) {
                if (candles.isEmpty()) return
                val index = ((x / size.width) * candles.size).toInt().coerceIn(0, candles.lastIndex)
                onSelect(index)
            }
            detectDragGestures(
                onDragStart = { update(it.x) },
                onDrag = { change, _ -> update(change.position.x) },
                onDragEnd = { },
                onDragCancel = { }
            )
        }
    ) {
        if (candles.isEmpty()) return@Canvas
        val chartBottom = size.height * .77f
        val volumeTop = size.height * .82f
        val volumeBottom = size.height * .98f
        val range = (maxPrice - minPrice).coerceAtLeast(1e-9)
        val step = size.width / candles.size.toFloat()
        val bodyWidth = (step * .56f).coerceAtLeast(1.2f)

        fun yFor(value: Double): Float = ((maxPrice - value) / range).toFloat() * chartBottom

        repeat(5) { i ->
            val y = chartBottom * i / 4f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f)
        }
        repeat(5) { i ->
            val x = size.width * i / 4f
            drawLine(grid, Offset(x, 0f), Offset(x, chartBottom), 1f)
        }
        drawLine(grid, Offset(0f, volumeTop - 5f), Offset(size.width, volumeTop - 5f), 1f)

        candles.forEachIndexed { index, c ->
            val x = step * (index + .5f)
            val color = if (c.close >= c.open) up else down
            val highY = yFor(c.high)
            val lowY = yFor(c.low)
            val openY = yFor(c.open)
            val closeY = yFor(c.close)
            drawLine(color, Offset(x, highY), Offset(x, lowY), max(1f, bodyWidth * .13f), StrokeCap.Round)
            val top = min(openY, closeY)
            val bottom = max(openY, closeY)
            val height = max(1.7f, bottom - top)
            drawRoundRect(color, Offset(x - bodyWidth / 2f, top), Size(bodyWidth, height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.2f, 1.2f))

            val vh = ((c.volume / maxVolume).toFloat() * (volumeBottom - volumeTop)).coerceAtLeast(1f)
            drawRect(color.copy(.40f), Offset(x - bodyWidth / 2f, volumeBottom - vh), Size(bodyWidth, vh))
        }

        fun drawMa(period: Int, color: Color) {
            if (candles.size < period) return
            val path = Path()
            var started = false
            candles.indices.forEach { index ->
                if (index >= period - 1) {
                    var sum = 0.0
                    for (i in index - period + 1..index) sum += candles[i].close
                    val value = sum / period
                    val x = step * (index + .5f)
                    val y = yFor(value)
                    if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
                }
            }
            drawPath(path, color, style = Stroke(width = 1.45f))
        }
        drawMa(5, ma5)
        drawMa(10, ma10)
        drawMa(20, ma20)

        selectedIndex?.takeIf { it in candles.indices }?.let { idx ->
            val candle = candles[idx]
            val x = step * (idx + .5f)
            val y = yFor(candle.close)
            val cross = Color.White.copy(.38f)
            drawLine(cross, Offset(x, 0f), Offset(x, chartBottom), 1f)
            drawLine(cross, Offset(0f, y), Offset(size.width, y), 1f)
            drawCircle(Color.White, radius = 3.2f, center = Offset(x, y))
        }
    }
}

@Composable
private fun CandleInfo(candle: Candle?, invert: Boolean) {
    val positive = candle?.let { it.close >= it.open } ?: true
    val color = changeColor(positive, invert)
    Column(Modifier.padding(horizontal = 14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            InfoTiny("开", candle?.open)
            InfoTiny("高", candle?.high)
            InfoTiny("低", candle?.low)
            InfoTiny("收", candle?.close, color)
        }
    }
}

@Composable
private fun InfoTiny(label: String, value: Double?, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(.36f))
        Spacer(Modifier.width(3.dp))
        Text(value?.let { rawPrice(it) } ?: "--", fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatsStrip(quote: LiveQuote, invert: Boolean) {
    PremiumCard(corner = 18.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp)) {
            StatInline("24H 高", priceText(quote.high24h), Modifier.weight(1f))
            StatInline("24H 低", priceText(quote.low24h), Modifier.weight(1f))
            StatInline("成交额", money(quote.quoteVolume24h), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatInline(title: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(title, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(.38f))
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun TradeHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 6.dp)) {
        Text("价格(USDT)", Modifier.weight(1.2f), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(.38f))
        Text("数量", Modifier.weight(1f), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(.38f))
        Text("时间", Modifier.weight(.8f), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(.38f))
    }
}

@Composable
private fun TradeRow(trade: MarketTrade, invert: Boolean) {
    val color = changeColor(!trade.isSell, invert)
    Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(rawPrice(trade.price), Modifier.weight(1.2f), fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
        Text(quantity(trade.quantity), Modifier.weight(1f), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.72f))
        Text(timeText(trade.time), Modifier.weight(.8f), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(.42f))
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(.48f))
    }
}

@Composable
private fun PeriodTab(title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(.14f) else Color.Transparent,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.48f)
        )
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(.13f) else MaterialTheme.colorScheme.surface.copy(.65f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.48f)
        )
    }
}

@Composable
private fun PremiumSearchField(query: String, onQuery: (String) -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(.82f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(.07f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(.38f))
                Spacer(Modifier.width(9.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isBlank()) Text("搜索 BTC / ETH / SOL", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(.35f))
                    inner()
                }
            }
        }
    )
}

@Composable
private fun SoftIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(.78f)) {
        IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(.74f))
        }
    }
}

@Composable
private fun LiquidGlassBar(selected: Tab, haptics: Boolean, onSelected: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val tabs = Tab.entries
    BoxWithConstraints(
        modifier = modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .fillMaxWidth().height(70.dp).shadow(24.dp, RoundedCornerShape(35.dp))
            .clip(RoundedCornerShape(35.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(.16f),
                        MaterialTheme.colorScheme.surface.copy(.92f),
                        MaterialTheme.colorScheme.surface.copy(.86f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(.11f), RoundedCornerShape(35.dp))
    ) {
        val itemWidth = maxWidth / tabs.size.toFloat()
        val x by animateDpAsState(
            targetValue = itemWidth * tabs.indexOf(selected),
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "玻璃选中块"
        )
        Box(
            Modifier.offset(x = x).width(itemWidth).fillMaxHeight().padding(7.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(.20f), MaterialTheme.colorScheme.primary.copy(.09f))
                    )
                )
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(.18f), RoundedCornerShape(28.dp))
        )
        Row(Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                val source = remember { MutableInteractionSource() }
                val pressed by source.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) .91f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "底栏按压")
                val active = tab == selected
                Column(
                    Modifier.weight(1f).fillMaxHeight().scale(scale).clickable(source, indication = null) {
                        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelected(tab)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        when (tab) {
                            Tab.Market -> Icons.Rounded.ShowChart
                            Tab.Watch -> if (active) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
                            Tab.Settings -> Icons.Rounded.Settings
                        },
                        tab.title,
                        Modifier.size(20.dp),
                        tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.42f)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tab.title,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.42f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumCard(corner: androidx.compose.ui.unit.Dp = 22.dp, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surface.copy(.88f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(.055f), RoundedCornerShape(corner))
            .animateContentSize()
    ) { content() }
}

@Composable
private fun MiniSpark(points: List<Float>, positive: Boolean, invert: Boolean, modifier: Modifier) {
    val color = changeColor(positive, invert)
    Canvas(modifier) {
        if (points.size < 2) {
            drawLine(MaterialTheme.colorScheme.onSurface.copy(.08f), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1f)
            return@Canvas
        }
        val minV = points.minOrNull() ?: 0f
        val maxV = points.maxOrNull() ?: minV + 1f
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = size.width * index / (points.size - 1f)
            val y = size.height - ((value - minV) / range) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
    }
}

@Composable
private fun CoinBadge(ticker: String) {
    val color = when (ticker) {
        "BTC" -> Color(0xFFF5A623)
        "ETH" -> Color(0xFF7D90C7)
        "SOL" -> Color(0xFF8A67E8)
        "BNB" -> Color(0xFFF3BA2F)
        "XRP" -> Color(0xFF69778A)
        "DOGE" -> Color(0xFFC7A544)
        "ADA" -> Color(0xFF3B82F6)
        "AVAX" -> Color(0xFFE84142)
        "LINK" -> Color(0xFF375BD2)
        "DOT" -> Color(0xFFE6007A)
        "LTC" -> Color(0xFF7891B5)
        else -> Color(0xFF4E8EE8)
    }
    Box(
        Modifier.size(42.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(color.copy(.98f), color.copy(.68f))))
            .border(1.dp, Color.White.copy(.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(ticker.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun LiveDot(active: Boolean) {
    val color = if (active) Up else MaterialTheme.colorScheme.onSurface.copy(.25f)
    Box(Modifier.size(7.dp).clip(CircleShape).background(color))
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(.58f))
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.42f))
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(.58f))
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DividerLine(horizontal: androidx.compose.ui.unit.Dp = 16.dp) {
    Divider(Modifier.padding(horizontal = horizontal), color = MaterialTheme.colorScheme.onSurface.copy(.055f))
}

@Composable
private fun changeColor(positive: Boolean, invert: Boolean): Color {
    return if (invert) {
        if (positive) Down else Up
    } else {
        if (positive) Up else Down
    }
}

private fun rawPrice(value: Double): String {
    if (value <= 0.0) return "--"
    return when {
        value >= 1000 -> String.format(Locale.US, "%,.2f", value)
        value >= 1 -> String.format(Locale.US, "%.2f", value)
        value >= .01 -> String.format(Locale.US, "%.4f", value)
        else -> String.format(Locale.US, "%.6f", value)
    }
}

private fun priceText(value: Double): String = if (value <= 0.0) "--" else "$${rawPrice(value)}"
private fun percent(value: Double): String = String.format(Locale.US, "%+.2f%%", value)
private fun quantity(value: Double): String = when {
    value >= 1000 -> String.format(Locale.US, "%,.2f", value)
    value >= 1 -> String.format(Locale.US, "%.4f", value)
    else -> String.format(Locale.US, "%.6f", value)
}
private fun money(value: Double): String = when {
    value <= 0 -> "--"
    value >= 1_000_000_000 -> String.format(Locale.US, "$%.2fB", value / 1_000_000_000)
    value >= 1_000_000 -> String.format(Locale.US, "$%.2fM", value / 1_000_000)
    value >= 1_000 -> String.format(Locale.US, "$%.2fK", value / 1_000)
    else -> String.format(Locale.US, "$%.2f", value)
}
private fun timeText(time: Long): String = SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(Date(time))
