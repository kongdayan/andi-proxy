<div align="center">
<img src="docs/images/icon.png" width="96" alt="Andi Proxy" />

# Andi Proxy

**あなたのホットスポットを、あなたのルールで。**

Hysteria2 または SOCKS5 プロキシを 1 つ用意するだけで、Android のホットスポットにつながるすべての端末で共有できます。root 権限は不要です。

[![CI](https://github.com/kongdayan/andi-proxy/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/kongdayan/andi-proxy/actions/workflows/ci.yml)
[![リリース](https://img.shields.io/github/v/release/kongdayan/andi-proxy?display_name=tag)](https://github.com/kongdayan/andi-proxy/releases/latest)
[![GPL-3.0-or-later](https://img.shields.io/badge/license-GPL--3.0--or--later-3979f6)](LICENSE)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3ddc84?logo=android)](https://github.com/kongdayan/andi-proxy/releases/latest)
[![言語](https://img.shields.io/badge/UI-EN%20%7C%20ZH%20%7C%20JA%20%7C%20KO%20%7C%20RU-8e7cc3)](#言語)

[English](README.md) · [简体中文](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Русский](README.ru.md)

</div>

Andi Proxy は、ありふれた Android スマートフォンを、小さく自己完結したプロキシゲートウェイに変えます。Hysteria2、あるいは認証付きの SOCKS5 アップストリームに接続したら、あとはスマートフォン自体をローカル VPN 経由でルーティングするもよし、スマートフォンのホットスポットや既存の Wi-Fi LAN を通じてノート PC、タブレット、ほかのスマートフォンとプロキシを共有するもよし。どちらを選ぶのも自由です。

root も `iptables` も不要で、常時接続のリモートコントロールも、裏で動くクラウドもありません。ホットスポットのクライアントは、Wi-Fi プロキシを `phone-address:2080` に向けるだけです。スマートフォン側のアプリも、アプリごとの除外設定を備えたローカル VPN を通じて、同じルールエンジンを利用できます。

> **ホットスポットのクライアントでは、必ずプロキシを設定してください。** スマートフォンの VPN が対象とするのはスマートフォン上のアプリであり、ほかの端末からの通信ではありません。これは root を使わない、現実的な設計によるものです。通常のアプリは、ホットスポットのカーネル転送経路を透過的に横取りできません。SOCKS5 に対応したクライアントであれば、同じアドレスとポートを利用できます。

![Andi Proxy のホーム画面](docs/images/home-en.png)

## Andi Proxy を選ぶ理由

- **アップストリームは 1 つ、共有方法は 2 通り。** このスマートフォンでプロキシを使う、ほかの端末と共有する、その両方を同時に使う。どれも自由に選べます。共有を停止しても、スマートフォンの VPN は動き続けます。
- **本物のルールルーティング。** Shadowrocket のルールリストや設定ファイルをインポートできるほか、ドメイン、CIDR、デフォルトルートのルールを手動で書くこともできます。`DIRECT`、`PROXY`、`REJECT` のすべてが機能します。
- **認証は、あるべき場所に。** Hysteria2 のパスワード、SOCKS5 のユーザー名とパスワード、任意のローカルプロキシ認証、TLS SNI、Salamander による難読化に対応します。
- **認証情報はスマートフォンの中だけに。** プロファイルは Android Keystore で暗号化され、バックアップの対象からも外れます。アクセス解析も広告もテレメトリーもありません。
- **普通のスマートフォンのために。** root もカーネルモジュールも、別途の VPN アプリも必要ありません。市販の Android 16 arm64 スマートフォンで動作を確認しています。
- **UI は 5 言語。** 英語、簡体字中国語、日本語、韓国語、ロシア語に対応し、アプリ内のほか、Android のアプリごとの言語設定からも切り替えられます。
- **オープンソースで、再現可能なビルド。** GitHub Actions が ABI ごとに 1 つの APK をビルドして署名し、タグを打つたびに SHA-256 チェックサムとあわせて公開します。

## スクリーンショット

| Settings | About | Languages |
| --- | --- | --- |
| ![Settings](docs/images/settings-en.png) | ![About](docs/images/about-en.png) | ![Languages](docs/images/languages-en.png) |

スクリーンショットは英語 UI です。アプリは簡体字中国語・日本語・韓国語・ロシア語にも対応しています。「設定 → 言語」または Android のアプリごとの言語設定で切り替えられます。

| Japanese About | Russian home |
| --- | --- |
| ![Japanese About](docs/images/about-ja.png) | ![Russian home](docs/images/home-ru.png) |

## 機能

| 分野 | できること |
| --- | --- |
| アップストリーム | TLS SNI、証明書検証のオン／オフ、Salamander 難読化に対応した Hysteria2。ユーザー名とパスワードを必須とし、UDP を任意で使える SOCKS5 |
| 共有 | アプリが作成するローカル専用ホットスポット、システムのホットスポット設定への導線、既存の Wi-Fi／LAN インターフェースでの共有。ポート `2080` で HTTP CONNECT と SOCKS5 を提供 |
| スマートフォンの VPN | hev-socks5-tunnel 経由で同じ sing-box コアを使用。システムアプリを含むアプリを検索して直接接続のリストに追加できる。Andi 自身は常に自動で除外 |
| ルール | `DOMAIN`、`DOMAIN-SUFFIX`、`DOMAIN-KEYWORD`、IPv4/IPv6 CIDR、`GEOIP`、`FINAL`/`MATCH`。手動ルール、プライベートネットワークの直接接続、サブスクリプションの順序 |
| DNS | プロキシ対象のドメインには Cloudflare のリモート DoH、直接接続するドメインには `223.5.5.5`、サーバーのホスト名は直接 DNS でブートストラップ。DNS ループは発生しません |
| サブスクリプション | Shadowrocket の `.conf` とルールリストに対応。HTTPS 限定、サイズ制限、アトミックなキャッシュ、起動時に 24 時間ごとの更新、キャッシュを置き換える前の検証 |
| UI | One UI 風のカード、ライト／ダーク／システムのテーマ、ゆったり／コンパクトのレイアウト、ホーム画面ウィジェット、クイック設定タイル、長押しのショートカット |
| プライバシー | Android Keystore による AES-GCM のプロファイル暗号化、バックアップを無効化、テレメトリーなし、リモートコントロールサービスなし |

## インストール

お使いの端末に合った APK を [最新リリース](https://github.com/kongdayan/andi-proxy/releases/latest) からダウンロードしてください。

| 端末 | APK |
| --- | --- |
| 最近のスマートフォンのほとんど（64 ビット ARM） | [AndiProxy-arm64-v8a.apk](https://github.com/kongdayan/andi-proxy/releases/latest/download/AndiProxy-arm64-v8a.apk) |
| 旧世代の 32 ビット ARM スマートフォン | [AndiProxy-armeabi-v7a.apk](https://github.com/kongdayan/andi-proxy/releases/latest/download/AndiProxy-armeabi-v7a.apk) |
| x86_64 のエミュレーターとタブレット | [AndiProxy-x86_64.apk](https://github.com/kongdayan/andi-proxy/releases/latest/download/AndiProxy-x86_64.apk) |
| 旧世代の x86 エミュレーター | [AndiProxy-x86.apk](https://github.com/kongdayan/andi-proxy/releases/latest/download/AndiProxy-x86.apk) |
| どれを選べばいいか分からない／1 つのファイルで済ませたい | [AndiProxy-universal.apk](https://github.com/kongdayan/andi-proxy/releases/latest/download/AndiProxy-universal.apk) |

**Android 8.0（API 26）** 以降が必要です。ユニバーサル APK はサイズがかなり大きいので、ABI ごとのファイルをおすすめします。各リリースには `SHA256SUMS` と、APK の署名方法を記した `SIGNING.txt` が含まれています。1.0.0 より前の開発版をインストールしている場合は、先にアンインストールしてください。公開版は新しい署名キーを使用しています。

Android から「アプリストアからのインストールではない」という警告が出ることがありますが、サイドロードしたオープンソース APK では想定どおりの動作です。本プロジェクトは Google Play では配信していません。

## クイックスタート

1. **サーバーを追加します。** 「サーバー」を開き、HY2/SOCKS5 の JSON プロファイルをインポートするか、アドレス、ポート、認証情報を入力します。最後に保存します。
2. **このスマートフォンをプロキシ化します。** ホームで「スマホのプロキシをオン」をタップし、VPN の接続要求を許可します。「VPN を通さないアプリ」を開けば、直接接続させたいアプリを検索して選べます。
3. **ほかの端末と共有します。** 「Wi-Fi 共有をオン」→「ほかのデバイスをつなぐ」の順にタップします。表示された Wi-Fi に接続したら、クライアントの Wi-Fi プロキシを「手動」にして、表示されているサーバーアドレスとポート `2080` を入力します。SOCKS5 に対応したクライアントなら、同じアドレスとポートがそのまま使えます。
4. **ルーティングを調整します。** 「ルール」を開いて Shadowrocket のサブスクリプションを貼り付け、すぐに更新するか、ドメイン／CIDR のルールを手動で書きます。ルール、サーバー、除外設定は、実行中のプロキシにも自動で反映されます。

クライアント側で Wi-Fi に「インターネット接続なし」と表示された場合は、「接続を続ける」を選んでください。インターネットへのアクセスはプロキシ経由で行われ、ホットスポットはローカル接続だけを提供します。

## 仕組み

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

- フォアグラウンドサービスが、生成された設定とともに固定バージョンの sing-box コアを起動します。設定はアプリのプライベートディレクトリにパーミッション `0600` で置かれ、セッションの停止時に削除されます。
- ローカルの mixed インバウンドは `2080` で待ち受け、HTTP CONNECT と SOCKS5 を受け付けます。共有は明示的です。クライアント側でプロキシを設定する必要があります。
- スマートフォンの VPN は `hev-socks5-tunnel` を使い、TUN のパケットを `2081` のループバック専用 SOCKS リスナーへ流し込みます。これにより、スマートフォンとホットスポットのクライアントが同じルートエンジンを共有します。
- Android で有効にできる VPN は 1 つだけです。Andi はルーティングループを避けるため、自身の VPN から自分自身を除外します。
- コアのバイナリは `libsingbox.so` としてパッケージされ、Android によってアプリのネイティブライブラリディレクトリへ展開されます。書き込み可能なディレクトリにダウンロードしたコードを実行することはありません。

コンポーネントの構成とデータフローについては、[アーキテクチャ](docs/ARCHITECTURE.md) を参照してください。

## ルールと DNS

ルーティングの優先順位は次のとおりです。

1. 手動のプロキシ対象ドメイン
2. 手動の直接接続ドメイン
3. 手動の直接接続 CIDR
4. プライベートネットワークの直接接続
5. サブスクリプションのルール（元の順序のまま）
6. 最終ルート

サブスクリプションに `FINAL` があれば、手動のデフォルトルートより優先されます。`FINAL` がなければ、手動のデフォルトがそのまま使われます。IP ルールは必要に応じて明示的に名前解決を行い、`no-resolve` のルールでは追加の問い合わせは発生しません。

手動ルールとサブスクリプションのドメインルールは、リゾルバーの選択にも関わります。プロキシ対象のドメインは Cloudflare のリモート DoH、直接接続するドメインは `223.5.5.5` を使い、サーバーのホスト名はループを避けるために直接 DNS でブートストラップします。地域ルールは、解決後の宛先 IP を使って判定します。プロキシを通らないクライアントの DNS 問い合わせは、ホットスポット共有では捕捉されません。スマートフォンの VPN はトンネル内の DNS を捕捉し、同じコアへ送ります。

インポートされるのは `[Rule]` セクションだけです。MITM、証明書の設定、URL リライト、General のシステム設定は無視されます。未対応のルーティングエントリがあると更新は失敗し、以前のキャッシュがそのまま使われます。黙って捨てられることはありません。

## プライバシーとセキュリティ

- サーバーの認証情報とプロファイルは、Android Keystore の AES-GCM 鍵で暗号化されます。このアプリでは、Android のバックアップとデバイス間の移行が無効になっています。
- 実行時に使うコアの設定はパーミッション `0600` で書き出され、停止時に削除されます。サブスクリプションの検証に使う設定は一時的なものです。
- サブスクリプションは、認証情報やフラグメントを埋め込んでいない HTTPS である必要があります。ダウンロードはサイズが制限され、リダイレクトも検査されます。
- アクセス解析、広告、テレメトリー、リモートコントロールサービスはありません。アプリがプロファイルや利用データをアップロードすることはありません。
- バイナリとネイティブライブラリは、公式アップストリームの固定リリースを使用し、[`scripts/fetch-core.py`](scripts/fetch-core.py) で SHA-256 による検証を行っています。

[プライバシー](docs/PRIVACY.md) と [サードパーティに関する通知](THIRD_PARTY_NOTICES.md) もあわせてご覧ください。

## 対応環境と制限

- **検証済みのプラットフォーム：** 市販の Android 16.0 スマートフォン、arm64-v8a、4 KiB メモリページ、root なし。ほかの ABI と 16 KiB ページサイズの端末もビルドしていますが、実機での検証はまだ行っていません。動作報告を歓迎します。
- **ホットスポットのクライアント：** アプリは `LocalOnlyHotspot` を作成し、Android 16.0 ではその名前とパスワードを Android が生成します。固定の名前とパスワードを使いたい場合は、システムのホットスポットを設定し、そのインターフェースを共有してください。アプリ側でホットスポット名を指定する Android 16.1 の API は組み込み済みですが、実機検証は行っていません。
- **透過的ではありません：** ホットスポットのクライアントでは、HTTP／SOCKS5 プロキシを設定する必要があります。ゲーム、UDP のみを使うアプリ、端末全体のプロキシには、クライアント側の SOCKS5／VPN／TUN ツールが必要です。
- **VPN は同時に 1 つだけ：** Android は 2 つの VPN を同時に有効にできません。Andi はほかの VPN サービスと並行して動作することはできません。
- **サブスクリプションは利用者が用意するもの：** ルールの内容とそのライセンスは利用者の責任です。使用されるのはルーティングルールだけです。
- **Play ストアでの配信はありません：** APK を手動でインストールしてください。ミラーからダウンロードした場合は、チェックサムを確認してください。

これまでの検証の詳細と、残っているエッジケースは [検証](docs/VERIFICATION.md) にまとめています。

## 言語

アプリの UI は次の言語に対応しています。

- 英語（中国語以外のシステムロケールではこれが既定）
- 簡体字中国語（简体中文）
- 日本語
- 韓国語（한국어）
- ロシア語（Русский）

切り替えは「設定 → 言語」から行います。Android 13 以降では「システム設定 → アプリ → Andi Proxy → 言語」からも変更できます。翻訳は [`app/src/main/assets/i18n/`](app/src/main/assets/i18n/) にあり、中国語の原文がルックアップキーになっています。テーブルを編集したあとは `python3 scripts/check-i18n.py` を実行してください。翻訳の修正は歓迎します。

## ソースからのビルド

必要なもの：JDK 17、Android SDK Platform 36、Build Tools 35.0.0、Python 3.10 以降。

```sh
git clone https://github.com/kongdayan/andi-proxy.git
cd andi-proxy

# 固定バージョンの sing-box と hev-socks5-tunnel のバイナリを取得（全 ABI + macOS ホスト用コア）
python3 scripts/fetch-core.py

# ANDROID_HOME が未設定なら、Gradle に SDK の場所を指定
export ANDROID_HOME=/path/to/android-sdk

./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

デバッグ APK は ABI ごとに `app/build/outputs/apk/debug/` へ出力されます。リリースビルドも同じ ABI 分割を使います。署名、バージョニング、リリースの詳細は [ビルド](docs/BUILDING.md) を参照してください。

便利なチェック：

```sh
python3 scripts/check-i18n.py       # 翻訳キー、プレースホルダー、網羅率
python3 scripts/core_smoke.py       # 実際の sing-box を使ったルーティング／フィクスチャのスモークテスト（macOS arm64 ホスト用コア）
```

## ドキュメント

- [Andi Proxy の使い方](docs/USAGE.md) · [简体中文](docs/USAGE.zh-CN.md) — セットアップ、共有、ルール、トラブルシューティング
- [アーキテクチャ](docs/ARCHITECTURE.md) — コンポーネント、データフロー、ストレージ、DNS
- [ビルドとリリース](docs/BUILDING.md) — SDK の設定、署名、ABI 分割、CI
- [プライバシー](docs/PRIVACY.md) — 何が保存され、何が送信され、何が送信されないのか
- [検証](docs/VERIFICATION.md) — 実機でテストした範囲と、そうでない範囲
- [サードパーティに関する通知](THIRD_PARTY_NOTICES.md) — バージョン、ソースへのリンク、SHA-256 ダイジェスト
- [開発履歴のメモ](docs/development/README.md) — 中国語で書かれた設計記録と受け入れ記録

## プロジェクト構成

```text
app/src/main/java/com/andi/proxy/   Activity、サービス、ルールエンジン、VPN、ウィジェット
app/src/main/assets/i18n/           英語、日本語、韓国語、ロシア語のテーブル
app/src/main/assets/licenses/       APK に同梱される GPL、MIT、サードパーティの通知
app/src/test/                       JVM のユニットテスト
app/src/androidTest/                オプトインの実機スモークランナー
docs/                               使い方、アーキテクチャ、ビルド、プライバシー、検証
scripts/                            固定コアの取得、i18n の検証、スモークテスト
.github/workflows/                  CI と署名付きリリースのパイプライン
```

## コントリビューション

Issue、プルリクエスト、実機での動作報告、翻訳の修正を歓迎します。まず [コントリビューション](CONTRIBUTING.md) と [セキュリティ](SECURITY.md) に目を通してください。不具合を報告する際は、Android のバージョン、端末のモデル、ABI、アプリのバージョン、そしてスマートフォンの VPN とホットスポット共有のどちら（あるいは両方）を有効にしていたかを記載してください。

ユーザー向けの文字列を追加する場合は、`app/src/main/assets/i18n/en.json` に追加し（可能であれば他の言語のテーブルにも）、`python3 scripts/check-i18n.py` を実行してください。Issue には認証情報、サーバーアドレス、個人情報を絶対に記載しないでください。

## ライセンス

Andi Proxy は **GPL-3.0-or-later** の下でライセンスされています。詳しくは [LICENSE](LICENSE) と [COPYRIGHT](COPYRIGHT) を参照してください。同梱の sing-box バイナリは GPL-3.0-or-later、hev-socks5-tunnel は MIT です。正確なバージョン、ソースへのリンク、SHA-256 ダイジェストは [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) に記載しています。アプリアイコンはオリジナルのベクターアセットです。

本プロジェクトは独立したプロジェクトであり、SagerNet、sing-box、hev-socks5-tunnel、Samsung、Google とは関係ありません。

## 作者について

**Andi Proxy**（[contributors](https://github.com/kongdayan/andi-proxy)）は上海を拠点とする独立開発者です。Andi Proxy は、Garmin ウォッチのデータを Mac に直接同期するネイティブ macOS 向けヘルスコンパニオンアプリ [the companion project](https://github.com/kongdayan/andi-proxy) の兄弟プロジェクトにあたります。どちらのプロジェクトにも同じ考え方が流れています。データは自分のデバイスの手元に置き、日々使うツールは小さく、静かで、頼りになるものであるべきだと、そういう考え方です。

作者とプロジェクトへのリンクは、アプリ内の「設定 → 作者とオープンソース情報」にも掲載しています。
