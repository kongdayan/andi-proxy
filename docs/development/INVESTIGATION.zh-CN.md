> 历史开发记录，保留中文原文。设备标识、UID、局域网地址和公网出口 IP 已脱敏；当前行为请以 [使用指南](../USAGE.zh-CN.md) 和 [验证记录](../VERIFICATION.md) 为准。

# Android 无 root 热点代理调查

调查与实测日期：2026-10-01（Asia/Shanghai）。

## 结论与参考软件

用户指定的软件是 [VPN代理分享服务:无需Root（WellDoMax）](https://play.google.com/store/apps/details?hl=zh&id=com.github.welldomax.proxyserver)。其开发者说明明确要求热点／局域网客户端设置代理，然后访问手机的 HTTP、HTTPS、SOCKS5 或 Shadowsocks 服务。它可以把代理发起的连接交给手机已有 VPN；部分设备通过附加插件解决 VPN 下本机服务不可访问的问题。

因此「不需要 root」成立的机制是应用层代理服务。客户端先选择代理，连接由手机 App 自己创建。用户最初要求的「热点连接后全部流量自动接管」则需要处理手机内核转发路径。普通 App 的本地代理服务器不会接收未发往它的客户端流量。

本项目实现同类无 root 使用效果，并将 HY2／带密码 SOCKS5 出站直接集成到同一 App，避免先连接另一个 VPN App 才能共享的依赖。没有复用第三方软件的私有代码或品牌。

## 实机信息

| 项目 | 实测结果 |
|---|---|
| 制造商 / 型号 | Retail Android phone (redacted) |
| Android / API | 16 / 36 |
| ABI | arm64-v8a |
| ADB | 已获用户 USB 调试授权 |
| root | 用户确认未 root；ADB shell 未发现 su |
| 页面大小 | 4096 字节 |
| 当前上游 Wi-Fi | wlan0，私网 IPv4 |
| App 热点接口 | wlanX，LocalOnlyHotspot 已创建成功 |
| 手机能力 | Wi-Fi Direct、SoftAP ACS、WPA3 SAE |
| 本次 Wi-Fi 并发 | 上游 wlan0 与 App 热点 wlanX 同时存在 |
| 系统转发加速 | BPF offload 可用；本项目无 root 模式无需修改 |

上述 Wi-Fi 并发能力和接口名称只在此设备已验证，不保证其他手机相同。App 自动对比热点创建前后的接口及私网地址，存在歧义时拒绝猜测，提示使用系统热点选择地址。

## 路线对比

| 方案 | 手机 root | 客户端操作 | 覆盖范围 | 本项目状态 |
|---|---|---|---|---|
| 手机本地 HTTP/SOCKS5 服务 | 无需 | 设置 Wi-Fi HTTP 代理或 SOCKS 客户端 | 发往代理的连接 | 已实现 |
| 手机 LocalOnlyHotspot + 应用层代理 | 无需 | 连接热点并设置代理 | 发往代理的连接；无原生互联网转发 | 已实现、实机热点已验证 |
| 系统热点 + 本地服务 | 无需 | 系统中开热点、客户端设置代理 | 发往代理的连接；其余可能直连 | 已实现服务与地址选择，未实测系统热点开关 |
| 手机 VpnService | 无需 | 用户授予 VPN 权限 | 手机自身对应用户／应用的流量 | 已实现本机分流及应用白名单 |
| 客户端 VPN/TUN → 手机 SOCKS5 | 手机无需 | 客户端安装和授权工具 | 客户端工具接管的流量 | 服务端支持，客户端未开发 |
| 内核透明接管热点流量 | 需要 root／系统能力 | 客户端无需配置代理 | 转发流量，受内核和加速兼容性影响 | 此无 root 手机不实施 |

Android [VPN 开发说明](https://developer.android.com/develop/connectivity/vpn)描述了 VpnService、TUN 和应用流量路由。热点转发还有独立的 [硬件 offload 路径](https://source.android.com/docs/core/data/tethering-offload)，会绕过应用处理器。开源 [VPN Hotspot](https://github.com/Mygod/VPNHotspot) 要求 root，并需要管理路由、iptables 和 offload。sing-box [TUN 文档](https://sing-box.sagernet.org/configuration/inbound/tun/)同样说明 Android 转发流量接管依赖 root 服务或 shell。

热点控制方面，Android [LocalOnlyHotspot API](https://developer.android.com/reference/android/net/wifi/WifiManager#startLocalOnlyHotspot(android.net.wifi.WifiManager.LocalOnlyHotspotCallback,%20android.os.Handler))允许创建局域网热点，Android 13+ 使用附近设备权限。普通系统互联网热点受 [TetheringManager 的权限与系统实现](https://developer.android.com/reference/android/net/TetheringManager)约束；公开 API 出现不等于所有厂商普通 App 都能直接启用。本设备非 root `cmd wifi help` 没有可用的 `start-softap`。本版通过系统设置页面提供普通热点入口。

## 核心与协议

采用未修改的官方 sing-box 1.14.2 Android arm64 可执行文件，应用只生成配置和管理进程。选择原因是同时支持 HTTP／SOCKS5 入站、HY2／SOCKS5 出站、TCP／UDP、DNS 和规则，不需要重新实现 QUIC、TLS 或 UDP ASSOCIATE。

- [SOCKS 出站](https://sing-box.sagernet.org/configuration/outbound/socks/)提供 version 5、username、password 与 UDP 支持。App 要求填写认证用户名和密码；实际错误密码测试已通过。
- [HY2 出站](https://sing-box.sagernet.org/configuration/outbound/hysteria2/)提供认证密码、TLS、Salamander 混淆与 UDP。
- [DNS HTTPS 服务](https://sing-box.sagernet.org/configuration/dns/server/https/)中显式设置 detour=proxy，避免新版 DNS 默认直接拨号绕过指定代理出口。
- [路由规则动作](https://sing-box.sagernet.org/configuration/route/rule_action/)用于嗅探、路由和 DNS 请求处理。

用户给出的 JSON 为 HY2，端口 443，peer 作为 SNI，obfs=none，allowInsecure=1。导入保留这些语义；密码不写入报告或 APK。新建 HY2 配置默认校验证书。

## 后续建议

真实第二客户端 Wi-Fi 连接、规则订阅和地区规则库已经实现并通过验证。后续可继续做长期息屏、多客户端压力和断网重连验证，增加连接二维码与流量统计。如以后仍要求客户端全部流量自动分流，在这台无 root 手机上需要客户端侧 VPN/TUN，或改用具备路由控制能力的外部设备。

## 2026-10-01 实现补充

当前实现包含真实 Shadowrocket 规则订阅、本机 VpnService 与应用白名单、One UI 风格界面、日夜主题、桌面小组件、快捷设置磁贴、图标快捷菜单和状态通知。原生小窗、窗口尺寸变化和上下分屏也已实测；小尺寸下折叠标题，保持导航与白名单列表可操作。

Android 16.0 新增 `startLocalOnlyHotspotWithConfiguration` 和 `SoftApConfiguration.Builder.setChannels`，允许普通应用选择频段。SSID、密码 setter 则在 **36.1** 才成为公开 API。测试机版本为 36.0，实际通过 5 GHz（5745 MHz）测试，名称/密码由系统生成；App 对不支持的字段禁用编辑，提供系统热点入口。16.1 分支已写入，但没有 16.1 设备验证。

本机 VpnService 使用 HEV JNI 把 TUN 包送到仅监听 loopback 的 SOCKS 端口 2081，再由同一 sing-box 规则引擎转发。排除本 App UID 防止环路，通过 `addDisallowedApplication` 实现白名单；VPN 仅作用于手机本机。Android 系统绑定 VPN 服务，因此停止操作必须显式结束隧道，而不能仅调用 stopService。

用户指定订阅实际为 392 条规则，包含 `GEOIP,CN,DIRECT` 与 `FINAL,proxy`；已下载并验证中国 IP 规则库，以保留其分流含义。规则以原子缓存指针发布，更新失败不会覆盖旧缓存；不执行 MITM 或 URL Rewrite。

实际外部 Wi-Fi 客户端已连接 App 热点并完成 HTTP 和 SOCKS5 HTTPS 出口测试，不再仅限手机自身连接监听端口。完整证据与未验证项目见 `VERIFICATION.md`。

新增主要资料：

- [Android WifiManager 热点接口](https://developer.android.com/reference/android/net/wifi/WifiManager#startLocalOnlyHotspotWithConfiguration(android.net.wifi.SoftApConfiguration,java.util.concurrent.Executor,android.net.wifi.WifiManager.LocalOnlyHotspotCallback))
- [SoftApConfiguration.Builder 36 / 36.1 API](https://developer.android.com/reference/android/net/wifi/SoftApConfiguration.Builder)
- [Android VpnService.Builder 应用排除](https://developer.android.com/reference/android/net/VpnService.Builder#addDisallowedApplication(java.lang.String))
- [sing-box 路由规则](https://sing-box.sagernet.org/configuration/route/rule/)、[规则集](https://sing-box.sagernet.org/configuration/rule-set/)
- [SagerNet GeoIP 数据来源](https://github.com/SagerNet/sing-geoip)
- [HEV TUN 转发源码与 API](https://github.com/heiher/hev-socks5-tunnel/tree/2.18.0)
- [指定 Shadowrocket 订阅](https://johnshall.github.io/Shadowrocket-ADBlock-Rules-Forever/sr_cnip.conf)
