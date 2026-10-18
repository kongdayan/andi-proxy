<div align="center">
<img src="docs/images/icon.png" width="96" alt="Andi Proxy" />

# Andi Proxy

**你的热点，你做主。**

把同一个 Hysteria2 或 SOCKS5 代理分享给 Android 热点上的每一台设备 —— 无需 root。

[![CI](https://github.com/kongdayan/andi-proxy/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/kongdayan/andi-proxy/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/kongdayan/andi-proxy?display_name=tag)](https://github.com/kongdayan/andi-proxy/releases/latest)
[![GPL-3.0-or-later](https://img.shields.io/badge/license-GPL--3.0--or--later-3979f6)](LICENSE)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3ddc84?logo=android)](https://github.com/kongdayan/andi-proxy/releases/latest)
[![语言](https://img.shields.io/badge/UI-EN%20%7C%20ZH%20%7C%20JA%20%7C%20KO%20%7C%20RU-8e7cc3)](#语言)

[English](README.md) · [简体中文](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Русский](README.ru.md)

</div>

Andi Proxy 能把一部普通的 Android 手机变成一台小巧、自成一体的代理网关。接入 Hysteria2 或带认证的 SOCKS5 上游后，你可以让手机本身通过本地 VPN 走代理，也可以通过手机热点或现有的 Wi-Fi 局域网，把代理分享给笔记本电脑、平板和其他手机。

不需要 root，不需要 `iptables`，没有常驻的远程控制，也没有隐藏的云端。热点客户端只要把 Wi-Fi 代理指向 `phone-address:2080`；手机上的应用则可以通过带分应用排除的本地 VPN，使用同一套规则引擎。

> **热点客户端必须自行设置代理。** 手机 VPN 只覆盖手机上的应用，不覆盖来自其他设备的流量。这是一个务实、无需 root 的设计取舍：普通应用无法透明地截获热点的内核转发路径。支持 SOCKS5 的客户端可以使用相同的地址和端口。

![Andi Proxy 主界面](docs/images/home-en.png)

## 为什么选择 Andi Proxy

- **一个上游，两种分享方式。** 可以只在本机使用代理，也可以分享给其他设备，或者两者同时进行。停止分享后，手机 VPN 会继续运行。
- **真正的规则路由。** 可导入 Shadowrocket 规则列表和配置文件，也可以手写域名、CIDR 和默认路由规则。`DIRECT`、`PROXY` 和 `REJECT` 均可生效。
- **认证放在该放的地方。** 支持 Hysteria2 密码、SOCKS5 用户名/密码、可选的本地代理认证、TLS SNI 以及 Salamander 混淆。
- **凭据只留在手机上。** 配置文件使用 Android Keystore 加密，并被排除在备份之外。没有分析、广告或遥测。
- **为普通手机而做。** 无需 root，无需内核模块，也不需要额外的 VPN 应用。已在零售版 Android 16 arm64 手机上测试。
- **五种界面语言。** 英语、简体中文、日语、韩语和俄语，可在应用内切换，也可通过 Android 的分应用语言设置切换。
- **开源、可复现构建。** GitHub Actions 会为每种 ABI 构建并签名一个 APK，并在每次打标签时连同 SHA-256 校验和一起发布。

## 截图

| Settings | About | Languages |
| --- | --- | --- |
| ![Settings](docs/images/settings-en.png) | ![About](docs/images/about-en.png) | ![Languages](docs/images/languages-en.png) |

截图使用英文界面。应用还提供简体中文、日语、韩语和俄语。可在「设置 → 语言」或 Android 的分应用语言设置中切换。

## 功能

| 类别 | 你能得到什么 |
| --- | --- |
| 上游 | 支持带 TLS SNI 的 Hysteria2，可切换证书校验、启用 Salamander 混淆；支持强制用户名/密码的 SOCKS5，并可选择启用 UDP |
| 分享 | 应用自建的仅本地热点、系统热点入口、复用现有 Wi-Fi/局域网接口进行分享；在 `2080` 端口提供 HTTP CONNECT 和 SOCKS5 |
| 手机 VPN | 通过 hev-socks5-tunnel 复用同一个 sing-box 内核；可分应用配置直连名单（含系统应用）并支持搜索；Andi 始终排除自身 |
| 规则 | `DOMAIN`、`DOMAIN-SUFFIX`、`DOMAIN-KEYWORD`、IPv4/IPv6 CIDR、`GEOIP`、`FINAL`/`MATCH`；支持手动规则、私有网络直连，并保持订阅规则原有顺序 |
| DNS | 走代理的域名使用远程 Cloudflare DoH，直连域名使用 `223.5.5.5`，服务器域名通过直连方式引导解析；不会形成 DNS 环路 |
| 订阅 | 支持 Shadowrocket `.conf` 与规则列表；仅允许 HTTPS、限制大小、原子化缓存、启动时每 24 小时刷新一次，校验通过后才替换缓存 |
| 界面 | One UI 风格卡片、浅色/深色/跟随系统主题、大号与紧凑布局、桌面小组件、快捷设置磁贴、长按快捷方式 |
| 隐私 | 通过 Android Keystore 以 AES-GCM 加密配置，禁用备份，无遥测，无远程控制服务 |

## 安装

从[最新发布](https://github.com/kongdayan/andi-proxy/releases/latest)下载与你的设备匹配的 APK：

| 设备 | APK |
| --- | --- |
| 大多数现代手机（64 位 ARM） | [AndiProxy-arm64-v8a.apk](https://github.com/kongdayan/andi-proxy/releases/latest/download/AndiProxy-arm64-v8a.apk) |
| 较旧的 32 位 ARM 手机 | [AndiProxy-armeabi-v7a.apk](https://github.com/kongdayan/andi-proxy/releases/latest/download/AndiProxy-armeabi-v7a.apk) |
| x86_64 模拟器与平板 | [AndiProxy-x86_64.apk](https://github.com/kongdayan/andi-proxy/releases/latest/download/AndiProxy-x86_64.apk) |
| 旧版 x86 模拟器 | [AndiProxy-x86.apk](https://github.com/kongdayan/andi-proxy/releases/latest/download/AndiProxy-x86.apk) |
| 不确定选哪个 / 想一个文件搞定所有设备 | [AndiProxy-universal.apk](https://github.com/kongdayan/andi-proxy/releases/latest/download/AndiProxy-universal.apk) |

需要 **Android 8.0（API 26）** 或更高版本。通用 APK 的体积大得多，建议优先选择对应 ABI 的文件。每个版本都附带 `SHA256SUMS` 以及一份 `SIGNING.txt` 说明，描述 APK 的签名方式。如果你安装过 1.0.0 之前的开发版，请先卸载：公开版本使用了新的签名密钥。

Android 可能会提示该应用并非来自应用商店。对于侧载安装的开源 APK，这是正常现象。本项目未上架 Google Play。

## 快速上手

1. **添加服务器。** 打开 **服务器**，导入 HY2/SOCKS5 JSON 配置，或直接填写地址、端口和凭据，然后保存。
2. **让本机走代理。** 在首页点击 **开启本机代理** 并允许 VPN 请求。打开 **不代理的应用**，搜索并勾选需要直连的应用。
3. **分享给其他设备。** 点击 **开启 Wi-Fi 共享** → **连接其他设备**，加入界面上显示的 Wi-Fi，然后把客户端的 Wi-Fi 代理设为 **手动**，填入显示的服务器地址和端口 `2080`。支持 SOCKS5 的客户端使用相同的地址和端口。
4. **调整路由。** 打开 **规则**，粘贴 Shadowrocket 订阅并立即更新，或手写域名/CIDR 规则。规则、服务器和排除项会自动应用到正在运行的代理上。

如果客户端提示该 Wi-Fi 无法上网，请选择 **保持连接**。热点只负责提供本地链路，上网流量会经由代理。

## 工作原理

```text
            ┌──────────────────────────── Android phone ────────────────────────────┐
            │                                                                       │
  Wi-Fi/LAN │  other devices ── HTTP CONNECT / SOCKS5 :2080 ──┐                    │
            │                                                  │                   │
            │  phone apps ── VpnService ── hev-socks5-tunnel ──┤                   │
            │                    (per-app exclusions)          ▼                   │
            │                                            sing-box 1.14.2            │
            │                                         rules / DNS / routing         │
            └──────────────────────────────────────────────────┬───────────────────┘
                                                               │
                                              Hysteria2 or authenticated SOCKS5
                                                               ▼
                                                          upstream
```

- 前台服务使用生成的配置启动固定版本的 sing-box 内核。配置文件位于应用私有目录，权限为 `0600`，会话结束后即被删除。
- 本地混合入站监听 `2080` 端口，接受 HTTP CONNECT 和 SOCKS5。分享是显式的：客户端必须自行配置代理。
- 手机 VPN 通过 `hev-socks5-tunnel` 把 TUN 数据包送入仅监听回环地址、端口为 `2081` 的 SOCKS 监听器，从而让手机与热点客户端共用同一套路由引擎。
- Android 只允许同时存在一个活动的 VPN。Andi 会把自身排除在自己的 VPN 之外，以避免路由环路。
- 内核二进制以 `libsingbox.so` 的形式打包，Android 会将其解压到应用的 native 库目录；应用不会执行被下载到可写目录中的代码。

组件结构与数据流详见[架构说明](docs/ARCHITECTURE.md)。

## 规则与 DNS

路由优先级：

1. 手动代理域名
2. 手动直连域名
3. 手动直连 CIDR
4. 私有网络直连
5. 订阅规则（保持原有顺序）
6. 最终路由

订阅中的 `FINAL` 会覆盖手动的默认路由；若订阅没有 `FINAL`，则保留手动设置的默认路由。IP 规则在需要时才会显式解析；带 `no-resolve` 的规则不会触发额外的查询。

手动规则与订阅中的域名规则同时决定使用哪个解析器：走代理的域名使用远程 Cloudflare DoH，直连域名使用 `223.5.5.5`，服务器域名则通过直连 DNS 引导解析，以免形成环路。国家/地区规则依据解析出的目标 IP 判断。热点分享不会截获客户端在代理之外发出的 DNS 查询；手机 VPN 会截获隧道内的 DNS 查询并交给同一个内核处理。

只会导入 `[Rule]` 段。MITM、证书配置、URL 重写以及常规系统设置均被忽略。遇到不支持的路由条目时，更新会整体失败并保留上一份缓存，绝不会静默丢弃。

## 隐私与安全

- 服务器凭据与配置文件使用 Android Keystore 中的 AES-GCM 密钥加密。本应用已禁用 Android 备份与设备间迁移。
- 运行时内核配置以 `0600` 权限写入，并在停止时删除。订阅校验所用的配置是临时文件。
- 订阅必须使用 HTTPS，且不得内嵌凭据或片段（fragment）。下载有大小限制，并会校验重定向。
- 没有分析、广告、遥测或远程控制服务。应用不会上传你的配置或使用数据。
- 二进制与原生库均取自上游固定版本的官方发布，并在 [`scripts/fetch-core.py`](scripts/fetch-core.py) 中通过 SHA-256 校验。

另见[隐私说明](docs/PRIVACY.md)与[第三方声明](THIRD_PARTY_NOTICES.md)。

## 兼容性与限制

- **已验证平台：** 零售版 Android 16.0 手机，arm64-v8a，4 KiB 内存页，未 root。其他 ABI 以及 16 KiB 页大小的设备可以构建，但尚未在真机上验证；欢迎反馈结果。
- **热点客户端：** 应用会创建一个 `LocalOnlyHotspot`；在 Android 16.0 上，其名称和密码由系统生成。如需固定名称/密码，请改为配置系统热点并分享其网络接口。用于让应用自定义热点名称的 Android 16.1 API 已编入代码，但尚未在真机上验证。
- **不是透明代理：** 热点客户端必须手动设置 HTTP/SOCKS5 代理。游戏、仅使用 UDP 的应用以及整机代理，需要在客户端侧配合 SOCKS5/VPN/TUN 工具。
- **同时只能有一个 VPN：** Android 不允许两个 VPN 同时处于活动状态，因此 Andi 无法与其他 VPN 服务并行运行。
- **订阅由用户自行提供：** 规则内容及其授权由订阅者自行负责。应用只使用其中的路由规则。
- **未上架 Play Store：** 请手动安装 APK；若从镜像站下载，请自行校验校验和。

历史验证细节与仍然存在的边界情况见[验证说明](docs/VERIFICATION.md)。

## 语言

应用界面支持以下语言：

- 英语（非中文系统环境下的默认语言）
- 简体中文
- 日本語（日语）
- 한국어（韩语）
- Русский（俄语）

可在 **设置 → 语言** 中切换；在 Android 13 及以上版本，也可在 **系统设置 → 应用 → Andi Proxy → 语言** 中切换。译文存放于 [`app/src/main/assets/i18n/`](app/src/main/assets/i18n/)，查表时以中文字符串作为键。修改语言表后请运行 `python3 scripts/check-i18n.py`。欢迎指正译文错误。

## 从源码构建

环境要求：JDK 17、Android SDK Platform 36、Build Tools 35.0.0、Python 3.10+。

```sh
git clone https://github.com/kongdayan/andi-proxy.git
cd andi-proxy

# Download the pinned sing-box and hev-socks5-tunnel binaries (all ABIs + macOS host core)
python3 scripts/fetch-core.py

# Point Gradle at your SDK if ANDROID_HOME is not already set
export ANDROID_HOME=/path/to/android-sdk

./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

调试版 APK 会按 ABI 分别输出到 `app/build/outputs/apk/debug/`。发布版构建采用相同的 ABI 拆分方式；签名、版本管理与发版细节见[构建说明](docs/BUILDING.md)。

常用检查：

```sh
python3 scripts/check-i18n.py       # translation keys, placeholders and coverage
python3 scripts/core_smoke.py       # real sing-box routing/fixture smoke test (macOS arm64 host core)
```

## 文档

- [使用 Andi Proxy](docs/USAGE.md) · [简体中文](docs/USAGE.zh-CN.md) —— 配置、分享、规则与问题排查
- [架构说明](docs/ARCHITECTURE.md) —— 组件、数据流、存储与 DNS
- [构建与发布](docs/BUILDING.md) —— SDK 配置、签名、ABI 拆分与 CI
- [隐私说明](docs/PRIVACY.md) —— 存了什么、发送了什么，以及什么都没有做
- [验证说明](docs/VERIFICATION.md) —— 哪些已在真机上测试、哪些还没有
- [第三方声明](THIRD_PARTY_NOTICES.md) —— 版本、源码链接与 SHA-256 摘要
- [历史开发记录](docs/development/README.md) —— 中文设计与验收记录

## 项目结构

```text
app/src/main/java/com/andi/proxy/   Activities, services, rule engine, VPN, widgets
app/src/main/assets/i18n/           English, Japanese, Korean and Russian tables
app/src/main/assets/licenses/       GPL, MIT and third-party notices shipped in the APK
app/src/test/                       JVM unit tests
app/src/androidTest/                opt-in device smoke runners
docs/                               usage, architecture, build, privacy, verification
scripts/                            pinned core fetch, i18n validation, smoke tests
.github/workflows/                  CI and signed release pipeline
```

## 参与贡献

欢迎提交 issue、pull request、设备反馈和译文修正。动手前请先阅读[贡献指南](CONTRIBUTING.md)与[安全策略](SECURITY.md)。提交缺陷报告时，请附上 Android 版本、设备型号、ABI、应用版本，以及当时启用的是手机 VPN、热点分享，还是两者。

如果新增了面向用户的字符串，请将其加入 `app/src/main/assets/i18n/en.json`（如果方便，也同步到其他语言表），并运行 `python3 scripts/check-i18n.py`。切勿在 issue 中填写凭据、服务器地址或个人信息。

## 许可证

Andi Proxy 以 **GPL-3.0-or-later** 许可证发布，详见 [LICENSE](LICENSE) 与 [COPYRIGHT](COPYRIGHT)。随附的 sing-box 二进制同样为 GPL-3.0-or-later；hev-socks5-tunnel 为 MIT。确切的版本、源码链接与 SHA-256 摘要列于 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。应用图标为原创矢量素材。

本项目是独立项目，与 SagerNet、sing-box、hev-socks5-tunnel、Samsung 或 Google 均无隶属关系。
