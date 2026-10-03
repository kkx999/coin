package com.kkx999.coin.data

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

data class LiveQuote(
    val symbol: String,
    val price: Double,
    val changePercent24h: Double,
    val high24h: Double,
    val low24h: Double,
    val quoteVolume24h: Double
)

data class Candle(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class MarketTrade(
    val price: Double,
    val quantity: Double,
    val time: Long,
    val isSell: Boolean
)

object BinanceMarketClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun connectMarket(
        symbols: List<String>,
        onConnected: () -> Unit,
        onQuote: (LiveQuote) -> Unit,
        onDisconnected: () -> Unit
    ): WebSocket {
        val streams = symbols.joinToString("/") { "${it.lowercase(Locale.US)}@ticker" }
        val request = Request.Builder()
            .url("wss://stream.binance.com:9443/stream?streams=$streams")
            .build()

        return client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = onConnected()

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val data = JSONObject(text).getJSONObject("data")
                    LiveQuote(
                        symbol = data.getString("s"),
                        price = data.getString("c").toDouble(),
                        changePercent24h = data.getString("P").toDouble(),
                        high24h = data.getString("h").toDouble(),
                        low24h = data.getString("l").toDouble(),
                        quoteVolume24h = data.getString("q").toDouble()
                    )
                }.onSuccess(onQuote)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = onDisconnected()
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = onDisconnected()
        })
    }

    fun connectTrades(
        symbol: String,
        onTrade: (MarketTrade) -> Unit,
        onDisconnected: () -> Unit = {}
    ): WebSocket {
        val request = Request.Builder()
            .url("wss://stream.binance.com:9443/ws/${symbol.lowercase(Locale.US)}@aggTrade")
            .build()

        return client.newWebSocket(request, object : WebSocketListener() {
            private var lastEmitAt = 0L

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val data = JSONObject(text)
                    val tradeTime = data.getLong("T")
                    MarketTrade(
                        price = data.getString("p").toDouble(),
                        quantity = data.getString("q").toDouble(),
                        time = tradeTime,
                        isSell = data.getBoolean("m")
                    )
                }.onSuccess { trade ->
                    if (trade.time - lastEmitAt >= 120L) {
                        lastEmitAt = trade.time
                        onTrade(trade)
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = onDisconnected()
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = onDisconnected()
        })
    }

    fun fetchCandles(
        symbol: String,
        interval: String,
        limit: Int,
        onResult: (List<Candle>) -> Unit,
        onError: () -> Unit
    ) {
        val request = Request.Builder()
            .url("https://api.binance.com/api/v3/klines?symbol=$symbol&interval=$interval&limit=$limit")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError()

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        onError()
                        return
                    }
                    val body = it.body?.string().orEmpty()
                    runCatching {
                        val array = JSONArray(body)
                        buildList {
                            for (index in 0 until array.length()) {
                                val row = array.getJSONArray(index)
                                add(
                                    Candle(
                                        openTime = row.getLong(0),
                                        open = row.getString(1).toDouble(),
                                        high = row.getString(2).toDouble(),
                                        low = row.getString(3).toDouble(),
                                        close = row.getString(4).toDouble(),
                                        volume = row.getString(5).toDouble()
                                    )
                                )
                            }
                        }
                    }.onSuccess(onResult).onFailure { onError() }
                }
            }
        })
    }
}
