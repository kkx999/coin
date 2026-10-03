# Coin

A lightweight Android cryptocurrency market app focused on **live prices, watchlists and clean charts**.

## Version

Current development version: **0.0.1**

## 0.0.1

- Native Android app built with Kotlin + Jetpack Compose
- Three-tab layout: 行情 / 自选 / 设置
- Floating rounded Liquid Glass-inspired bottom navigation
- Public Binance WebSocket live tickers, no API key and no login
- BTC / ETH / SOL / BNB / XRP / DOGE market cards
- Live 24h price change, high, low and turnover
- Coin detail page with historical price chart
- 1H / 1D / 1W / 1M / 3M chart ranges
- Local watchlist persistence
- Dark / light mode, haptic feedback and red/green direction preference
- Press, spring, crossfade and page-transition animations
- GitHub Actions APK build

## Privacy

Coin 0.0.1 does not require an account. Watchlist and preferences are stored locally on the device.

## Build

```bash
gradle :app:assembleDebug
```

GitHub Actions also builds an installable debug APK automatically on every push to `main`.
