# 配布

v2.0 は **Firebase App Distribution** と **GitHub Releases** で配布する(Google Play には出さない。`docs/design/v2.0-standard.md` 13. 決定事項 10)。

| 配布物 | 配布先 | 状態 |
|---|---|---|
| デバッグ版(`androidApp`) | Firebase App Distribution | 整備済み(このドキュメント) |
| リリース版(署名済み APK) | GitHub Releases・Firebase App Distribution | 未整備(#72) |

## デバッグ版の配布

手元の端末で開発中の版を試すために、**デバッグ版を main にマージするたびに** 配布する(`.github/workflows/android-debug.yml`)。

- **アプリ ID**: `jp.ikanoshiokara.pixelpilot.debug`(`applicationIdSuffix`)。表示名は「PixelPilot (debug)」
  - v1.0(`jp.ikanoshiokara.pixelpilot`)や将来のリリース版と、同じ端末に並べて入れられる
- **タイミング**: main へのマージ(push)ごと
  - 続けてマージしたとき(スタック PR など)は、最初に **5 分待ち**、その間に次のマージが来たら前の実行を取り消す(`concurrency` の `cancel-in-progress`)。最後のマージの分だけが配布される
  - Actions から手動(`workflow_dispatch`)でも動かせる。そのときは待たない
- **版の番号**
  - `versionCode`: main のコミット数(配布するたびに増える)
  - `versionName`: `2.0.0-debug+<短いコミット>`(どのコードのビルドか分かるように)
- **リリースノート**: 前回配布したコミット(`android-debug` タグ)からのコミットの一覧。配布した後にタグを付け替える
- **署名**: **デバッグ鍵を固定する**。CI のデバッグ鍵はランナーごとに作り直されるため、そのままだと配布した版の上に新しい版を入れられない(署名が違うため)。決まった鍵を Secrets に置き、CI ではそれで署名する
- **Secrets が無いうちは、配布しないで終わる**(失敗にはしない)

### 準備(リポジトリのオーナーがすること)

1. **Firebase のプロジェクトを作る**(既存のプロジェクトでもよい)
2. Firebase のプロジェクトに Android アプリを **`jp.ikanoshiokara.pixelpilot.debug`** で登録する
   - `google-services.json` はダウンロードしなくてよい(アプリに Firebase の SDK は入れない)
3. **App Distribution を有効にし、テスターのグループを作る**(例: `developers`)。自分をテスターとして追加する
4. Google Cloud で **サービスアカウント** を作り、ロール **Firebase App Distribution Admin** を付け、鍵(JSON)を作る
5. **デバッグ鍵を用意する**。手元の `~/.android/debug.keystore`(Windows は `%USERPROFILE%\.android\debug.keystore`)を使うと、手元のビルドと CI のビルドの署名がそろう(パスワードは `android`、別名は `androiddebugkey` のまま)。base64 にする:
   - macOS / Linux: `base64 < ~/.android/debug.keystore | tr -d '\n'`
   - Windows(PowerShell): `[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\.android\debug.keystore"))`
6. GitHub の Settings → Secrets and variables → Actions に置く:

| 種類 | 名前 | 値 |
|---|---|---|
| Secret | `FIREBASE_ANDROID_DEBUG_APP_ID` | Firebase の App ID(`1:...:android:...`。Firebase コンソールのプロジェクトの設定 → マイアプリ) |
| Secret | `FIREBASE_SERVICE_ACCOUNT` | サービスアカウントの鍵(JSON そのまま) |
| Secret | `ANDROID_DEBUG_KEYSTORE` | デバッグ鍵の base64 |
| Variable | `FIREBASE_TESTER_GROUPS` | 配布先のグループのエイリアス(カンマ区切り。例: `developers`) |

7. Actions の「Android debug」を手動で実行し、配布されることを確かめる
8. 端末に **App Tester** アプリを入れ(招待メールのリンクから)、招待を受けてインストールする
   - 初回は「提供元不明のアプリ」のインストールを許可する必要がある

### 手元のビルドとの関係

- 手元で `./gradlew :androidApp:installDebug` した版も、手元の `~/.android/debug.keystore` で署名される。手順 5 で同じ鍵を Secrets に置けば、配布版と手元の版を上書きし合える
- 手元のビルドでは `versionCode` は 2、`versionName` は `2.0.0-debug` になる。配布版(コミット数)の方が大きいため、手元の版から配布版へは上書きできるが、逆はできない。その場合はアンインストールしてから入れる
