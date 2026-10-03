# 币行情

一个轻量、无登录的 Android 加密货币实时行情应用，专注于 **实时价格、自选、专业 K 线和逐笔成交监控**。

## 当前版本

开发版本：**0.0.2**

## 0.0.2 主要变化

- App 与项目说明全面中文化
- 全新行情首页与卡片视觉，提升信息层级和质感
- iOS Liquid Glass 风格悬浮底部导航
- 系统返回键正确返回上一层页面
- 详情页升级为交易所风格 K 线
- 支持 1 分 / 5 分 / 15 分 / 1 小时 / 4 小时 / 日线
- K 线显示 MA5 / MA10 / MA20
- 支持拖动查看单根 K 线 OHLC 数据
- 详情页新增 Binance 实时逐笔成交监控
- 扩展 BTC / ETH / SOL / BNB / XRP / DOGE / ADA / AVAX / LINK / DOT / LTC / TRX
- 自选与设置继续保存在本机
- 无需账号、无需 API Key，不提供交易功能

## 技术栈

- Kotlin
- Jetpack Compose
- OkHttp WebSocket
- Binance Public API
- GitHub Actions

## 隐私

币行情不要求注册或登录。自选列表与界面偏好仅保存在本机。

## 构建

```bash
gradle :app:assembleDebug
```

GitHub Actions 会自动构建可安装 APK。
