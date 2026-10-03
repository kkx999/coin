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
import java.util.concurrent.TimeUnit

data class LiveQuote(
    val symbol: String,
    val price: Double,
    val changePercent24h: Double,
    val high24h: Double,
    val low24h: Double,
    val quoteVolume24h: Double
)

object BinanceMarketClient {
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val streams = listOf(
        "btcusdt@ticker",
        "ethusdt@ticker",
        "solusdt@ticker",
        "bnbusdt@ticker",
        "xrpusdt@ticker",
        "dogeusdt@ticker"
    ).joinToString("/")

    fun connect(
        onConnected: () -> Unit,
        onQuote: (LiveQuote) -> Unit,
        onDisconnected: () -> Unit
    ): WebSocket {
        val request = Request.Builder()
            .url("wss://stream.binance.com:9443/stream?streams=$streams")
            .build()

        return client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val data = JSONObject(text).getJSONObject("data")
                    val symbol = data.getString("s")
                    onQuote(
                        LiveQuote(
                            symbol = symbol,
                            price = data.getString("c").toDouble(),
                            changePercent24h = data.getString("P").toDouble(),
                            high24h = data.getString("h").toDouble(),
                            low24h = data.getString("l").toDouble(),
                            quoteVolume24h = data.getString("q").toDouble()
                        )
                    )
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onDisconnected()
            }
        })
    }

    fun fetchKlines(
        symbol: String,
        interval: String,
        limit: Int,
        onResult: (List<Float>) -> Unit,
        onError: () -> Unit
    ) {
        val request = Request.Builder()
            .url("https://api.binance.com/api/v3/klines?symbol=$symbol&interval=$interval&limit=$limit")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError()
            }

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
                                val candle = array.getJSONArray(index)
                                add(candle.getString(4).toFloat())
                            }
                        }
                    }.onSuccess(onResult).onFailure { onError() }
                }
            }
        })
    }
}
