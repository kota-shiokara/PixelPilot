# PixelPilot

スマートフォンを PC のマウス / キーボードとして使うアプリ。

## 現状

- v2.0(Kotlin Multiplatform)は `model/`・`domain/`・`view/`・`transport/`・`androidApp/` に置く。v1.0 の `app/` は v2.1 で削除した(#30)。
  - `model/*`(データ型)、`domain/*`(ロジックとインターフェース)、`view/*`(Compose Multiplatform の UI)、`transport/*`(経路の実装)、`androidApp`(骨格)
  - 各モジュールのビルド設定は `build-logic/` の convention plugin(`pixelpilot.kmp.library` / `pixelpilot.kmp.compose`)にまとめる。
  - v2.0 のターゲットは Android のみ。iOS・デスクトップのターゲットは v2.x で `build-logic` に追加する。
- v2.0 は開発中。方針は `docs/design/v2.0.md` を参照し、設計に関わる変更はこのドキュメントと整合させること。決定事項が変わる場合はドキュメントも更新する。

## v2.0 の設計原則(要約)

- 基本はモバイル単体で Bluetooth HID を使い、ホスト側のインストールを不要にする。デスクトップ版は高機能版。
- Kotlin Multiplatform / Compose Multiplatform。**部品(入力イベント・プロトコル・UI 部品)は共通、骨格(画面遷移・ホスト選択・接続フロー)はプラットフォーム別**。
- 共通コードに `isIOS` / `isAndroid` のような OS 分岐を持ち込まない。違いは `PlatformTransports` などのインターフェースで注入し、**OS 名ではなく能力(利用可能な経路)で振る舞いを決める**。
- モジュールは **model / domain / view のレイヤー**で分ける。依存は view → domain → model の一方向のみ。view は transport に依存しない。同じレイヤー内で横の依存は作らず、共有するものは `*/common` に置く(詳細は `docs/design/v2.0.md` 7. モジュール構成)。
- 入力イベントは HID Usage コードで表現する。
- デスクトップ版との通信路は USB > Bluetooth > LAN の優先順。
- DI は Koin(Dagger / Hilt は KMP 非対応のため不採用)。model・domain は Koin に依存させず、view・transport が各自の Koin モジュールを提供し、アプリが `startKoin` でまとめる。

## ビルド・テスト

```sh
./gradlew assembleDebug     # ビルド
./gradlew test              # 単体テスト
./gradlew lint              # Android Lint
```

Android SDK が必要。実機の Bluetooth 挙動はエミュレータでは確認できない。

CI(`.github/workflows/ci.yml`)が PR と `main` への push で上の 3 つを実行する。テスト・lint のレポートは実行結果のアーティファクト(`reports`)に残る。

デバッグ版は main へのマージごとに Firebase App Distribution で配布する(`.github/workflows/android-debug.yml`、アプリ ID は `jp.ikanoshiokara.pixelpilot.debug`)。準備の手順と仕組みは `docs/distribution.md`。

### 実行環境ごとの方針

- **リモート環境(Claude Code on the web などのクラウドセッション。環境変数 `CLAUDE_CODE_REMOTE=true`)**: ビルド・テストは時間がかかるため実行しない。
  - 変更は差分の読み直し(型・import・呼び出し側との整合)で検証する。
  - 実行していない検証(ビルド・テスト・lint)は、PR の説明に「未実施」と明記し、CI またはローカル環境での確認に委ねる。
- **ローカル環境**: ビルド・テスト・lint は積極的に実行してよい。変更のたびに回して確認する。

## ブランチ運用

- ブランチ名は基本的に `issue-{issue番号}/{タスク名}` とする(例: `issue-21/hid-report-descriptor`)。
- issue が無い場合は種別の接頭辞を付ける: `feature/`, `fix/`, `docs/` など(例: `docs/v2-design`)。
- `claude/` 接頭辞は極力使用しない。

## バージョン戦略

`メジャー.マイナー.パッチ` の形式でバージョンを付ける。

- **マイナー**: 開発はマイナーバージョン単位で進める。GitHub のマイルストーン(例: `v2.1`)を作り、対象の issue を紐づける。
- **パッチ**: 1 つのマイナーバージョンを複数のグループに分けて開発する場合、グループを 1 つ終えるごとにパッチを繰り上げる。
- **Backlog**: 次のバージョンに入れないもの(優先度が低いものなど)は `Backlog` マイルストーンに置き、将来のバージョンでトリアージする。
- **メジャー**: オーナーが納得した初回リリースと、それ以降の大規模変更時に繰り上げる。**判断はオーナーが行う。Claude はメジャーバージョンを繰り上げない。**

## 規約

- ドキュメント・コメントは日本語で書く。
- 接続先の機器名などをハードコードしない。
- HID Report Descriptor やレポートのバイト列を変更する場合は単体テストを追加する。
- 依存ライブラリ・プラグインのバージョンは `gradle/libs.versions.toml`(Version Catalog)で管理する。
