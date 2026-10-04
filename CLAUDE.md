# PixelPilot

スマートフォンを PC のマウス / キーボードとして使うアプリ。

## 現状

- `app/` は v1.0(Android 単体の Bluetooth HID マウス)。Kotlin + Jetpack Compose、minSdk 28。
- v2.0 は設計段階。方針は `docs/design/v2.0.md` を参照し、設計に関わる変更はこのドキュメントと整合させること。決定事項が変わる場合はドキュメントも更新する。

## v2.0 の設計原則(要約)

- 基本はモバイル単体で Bluetooth HID を使い、ホスト側のインストールを不要にする。デスクトップ版は高機能版。
- Kotlin Multiplatform / Compose Multiplatform。**部品(入力イベント・プロトコル・UI 部品)は共通、骨格(画面遷移・オンボーディング・接続フロー)はプラットフォーム別**。
- 共通コードに `isIOS` / `isAndroid` のような OS 分岐を持ち込まない。違いは `PlatformTransports` などのインターフェースで注入し、**OS 名ではなく能力(利用可能な経路)で振る舞いを決める**。
- 入力イベントは HID Usage コードで表現する。
- デスクトップ版との通信路は USB > Bluetooth > LAN の優先順。
- DI は Koin(Dagger / Hilt は KMP 非対応のため不採用)。

## ビルド・テスト

```sh
./gradlew assembleDebug     # ビルド
./gradlew test              # 単体テスト
./gradlew lint              # Android Lint
```

Android SDK が必要。実機の Bluetooth 挙動はエミュレータでは確認できない。

## ブランチ運用

- ブランチ名は基本的に `{issue番号}/{タスク名}` とする(例: `21/hid-report-descriptor`)。
- issue が無い場合は種別の接頭辞を付ける: `feature/`, `fix/`, `docs/` など(例: `docs/v2-design`)。
- `claude/` 接頭辞は極力使用しない。

## 規約

- ドキュメント・コメントは日本語で書く。
- 接続先の機器名などをハードコードしない。
- HID Report Descriptor やレポートのバイト列を変更する場合は単体テストを追加する。
- 依存ライブラリ・プラグインのバージョンは `gradle/libs.versions.toml`(Version Catalog)で管理する。
