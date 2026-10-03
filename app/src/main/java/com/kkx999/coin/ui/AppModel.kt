package com.kkx999.coin.ui

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import java.util.Locale
import kotlin.math.absoluteValue

internal val DarkColors = darkColorScheme(
    background = Color(0xFF05070B),
    surface = Color(0xFF0B1018),
    surfaceVariant = Color(0xFF111824),
    primary = Color(0xFF6EA8FF),
    secondary = Color(0xFF9CC5FF),
    onBackground = Color(0xFFF5F7FA),
    onSurface = Color(0xFFF5F7FA)
)

internal val LightColors = lightColorScheme(
    background = Color(0xFFF3F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEAF0F8),
    primary = Color(0xFF276DD5),
    secondary = Color(0xFF5D8FD4),
    onBackground = Color(0xFF10141B),
    onSurface = Color(0xFF10141B)
)

internal val UpGreen = Color(0xFF3FD49B)
internal val DownRed = Color(0xFFFF6677)
internal val Ma5Color = Color(0xFFF4C560)
internal val Ma10Color = Color(0xFF9B8CFF)

internal data class CoinMeta(
    val symbol: String,
    val ticker: String,
    val chineseName: String,
    val fallback: Double,
    val badgeA: Color,
    val badgeB: Color
)

internal val CoinList = listOf(
    CoinMeta("BTCUSDT", "BTC", "比特币", 84936.70, Color(0xFFF6B84A), Color(0xFFF7931A)),
    CoinMeta("ETHUSDT", "ETH", "以太坊", 2685.21, Color(0xFF9BB7E9), Color(0xFF5C7FC7)),
    CoinMeta("SOLUSDT", "SOL", "Solana", 119.76, Color(0xFF9F7AEA), Color(0xFF5B58D6)),
    CoinMeta("BNBUSDT", "BNB", "币安币", 789.26, Color(0xFFFFD45C), Color(0xFFF0B90B)),
    CoinMeta("XRPUSDT", "XRP", "瑞波币", 1.49, Color(0xFF9CA7B7), Color(0xFF566375)),
    CoinMeta("DOGEUSDT", "DOGE", "狗狗币", 0.0931, Color(0xFFC7A85B), Color(0xFF8F772C))
)

internal enum class MainTab(val title: String) {
    Market("行情"), Watch("自选"), Settings("设置")
}

internal enum class MarketFilter(val title: String) {
    All("全部"), Gainers("涨幅"), Losers("跌幅")
}

internal object AppPrefs {
    private const val FILE = "币行情"

    fun favorites(context: Context): Set<String> = context.getSharedPreferences(FILE, 0)
        .getStringSet("favorites", setOf("BTCUSDT", "ETHUSDT", "SOLUSDT"))
        ?.toSet().orEmpty()

    fun favorites(context: Context, value: Set<String>) {
        context.getSharedPreferences(FILE, 0).edit().putStringSet("favorites", value).apply()
    }

    fun getBoolean(context: Context, key: String, default: Boolean): Boolean =
        context.getSharedPreferences(FILE, 0).getBoolean(key, default)

    fun setBoolean(context: Context, key: String, value: Boolean) {
        context.getSharedPreferences(FILE, 0).edit().putBoolean(key, value).apply()
    }
}

internal fun priceColor(positive: Boolean, invert: Boolean): Color = if (invert) {
    if (positive) DownRed else UpGreen
} else {
    if (positive) UpGreen else DownRed
}

internal fun formatPrice(value: Double): String = when {
    value >= 1000 -> "$" + String.format(Locale.US, "%,.2f", value)
    value >= 1 -> "$" + String.format(Locale.US, "%.2f", value)
    value >= .01 -> "$" + String.format(Locale.US, "%.4f", value)
    else -> "$" + String.format(Locale.US, "%.6f", value)
}

internal fun shortPrice(value: Double): String = when {
    value >= 10000 -> String.format(Locale.US, "%.0f", value)
    value >= 1000 -> String.format(Locale.US, "%.1f", value)
    value >= 1 -> String.format(Locale.US, "%.2f", value)
    else -> String.format(Locale.US, "%.4f", value)
}

internal fun compactMoney(value: Double): String {
    val abs = value.absoluteValue
    return when {
        abs >= 1_000_000_000 -> "$" + String.format(Locale.US, "%.2fB", value / 1_000_000_000)
        abs >= 1_000_000 -> "$" + String.format(Locale.US, "%.2fM", value / 1_000_000)
        abs >= 1_000 -> "$" + String.format(Locale.US, "%.2fK", value / 1_000)
        else -> "$" + String.format(Locale.US, "%.2f", value)
    }
}

internal fun formatQuantity(value: Double): String = when {
    value >= 1000 -> String.format(Locale.US, "%.2fK", value / 1000)
    value >= 1 -> String.format(Locale.US, "%.4f", value)
    else -> String.format(Locale.US, "%.6f", value)
}
