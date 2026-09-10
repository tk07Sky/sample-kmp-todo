# TodoKmp

Kotlin Multiplatform + Compose Multiplatform で作った Todo 管理アプリです。
Android / iOS / Web (Wasm) の 3 プラットフォームで、ドメイン・状態管理・UI をすべて共有しています。

## 機能

- Todo の追加・編集・削除
- 完了 / 未完了の切り替え
- 「すべて / 未完了 / 完了」での絞り込み
- 完了済みの一括削除
- 未完了件数・全件数の表示
- データの永続化（アプリを閉じても残る）
- ダークモード対応

## モジュール構成

```
todo-kmp/
├── shared/          Kotlin Multiplatform ライブラリ（アプリの中身はすべてここ）
│   └── src/
│       ├── commonMain/     ドメイン・ViewModel・Compose UI・SQLDelight スキーマ
│       ├── androidMain/    Android 用の SQLite ドライバと setContent
│       ├── iosMain/        iOS 用の SQLite ドライバと UIViewController
│       └── wasmJsMain/     Web 用の localStorage 実装とエントリポイント
├── androidApp/      Android アプリの器（Activity と Manifest のみ）
└── iosApp/          Xcode プロジェクト（SwiftUI から Compose を埋め込む）
```

### なぜ `shared` と `androidApp` を分けているか

AGP 9.0 以降、`com.android.application` プラグインは
`org.jetbrains.kotlin.multiplatform` プラグインと同じモジュールに適用できなくなりました。
そのため KMP 側は `com.android.kotlin.multiplatform.library` を使うライブラリモジュールとし、
アプリ本体は別モジュールに分けています。

`androidApp` には Compose のコードを一切置いていません。
`setContent { App(...) }` の呼び出しごと `shared/androidMain` に移してあるため、
`androidApp` は Compose コンパイラを適用する必要がなく、薄い器のままで済みます。

## 技術選定

| 領域 | 選択 | 補足 |
| --- | --- | --- |
| UI | Compose Multiplatform 1.12.0 | 3 プラットフォームで UI コードを共有 |
| 状態管理 | androidx.lifecycle ViewModel (KMP 版) | `viewModel { }` が共通コードで使える |
| DB (Android/iOS) | SQLDelight 2.3.2 | 型安全な SQL、Flow で監視できる |
| DB (Web) | localStorage + kotlinx.serialization | 後述の理由により SQLDelight を使っていない |
| DI | 手動 DI | 依存が少ないため、ライブラリを入れずコンストラクタ渡しで完結させている |

### Web だけ SQLDelight を使っていない理由

SQLDelight の Wasm 向けドライバ（`web-worker-driver` + sql.js）は
データベースをメモリ上にしか保持できず、ページをリロードすると内容が消えます。
Todo アプリとしては保存されないと意味がないため、Web だけ localStorage を使っています。

差し替わるのは `TodoRepository` の実装クラス 1 つだけで、
ドメイン・ViewModel・UI はすべて共通のままです。

## セットアップ

### 1. ビルドツール（mise）

JDK と Gradle は [mise](https://mise.jdx.dev/) で固定しています。

```sh
mise trust     # 初回のみ。.mise.toml を信頼する
mise install   # JDK 21 と Gradle 9.7.1 を導入する
```

導入されるバージョンは `.mise.toml` に記載しています。
プロジェクトディレクトリに入ると自動で切り替わります。

### 2. Android SDK

Android SDK は mise の管理対象外です。Android Studio か cmdline-tools で導入し、
`local.properties` に場所を書いてください（このリポジトリには含めていません）。

```properties
sdk.dir=/Users/<ユーザー名>/Library/Android/sdk
```

必要な API レベルは 37（compileSdk / targetSdk）、最小は 24 です。

### 3. iOS（macOS のみ）

Xcode が必要です。`iosApp/Configuration/Config.xcconfig` の `TEAM_ID` は
実機で動かす場合のみ設定してください（シミュレータでは空のままで動きます）。

## 実行方法

### Android

```sh
./gradlew :androidApp:installDebug     # 接続中の端末・エミュレータにインストール
./gradlew :androidApp:assembleDebug    # APK をビルドするだけ
```

### iOS

```sh
open iosApp/iosApp.xcodeproj
```

Xcode で `iosApp` スキームを選び、シミュレータか実機に対して実行します。
ビルド時に `Compile Kotlin Framework` フェーズが Gradle を呼び、
`ComposeApp.framework` を生成して埋め込みます。

対象は実機 (arm64) と Apple Silicon シミュレータです。
Compose Multiplatform 1.12 は Intel シミュレータ (iosX64) 向けを配信していないため対象外にしています。
シミュレータで動かす場合は、Xcode の Settings > Components から iOS のシミュレータランタイムを
あらかじめ入れておいてください（未導入だとシミュレータ向けにビルドできません）。

### Web (Wasm)

```sh
./gradlew :shared:wasmJsBrowserDevelopmentRun    # 開発サーバを起動する
./gradlew :shared:wasmJsBrowserDistribution      # 配布用の静的ファイルを出力する
```

Wasm GC に対応したブラウザが必要です（Chrome 119 以降、Firefox 120 以降、Safari 18.4 以降）。

## テスト

```sh
./gradlew :shared:testAndroidHostTest
```

`shared/src/commonTest` に、ViewModel の状態遷移と絞り込みロジックのテストを置いています。
テスト用のリポジトリ実装（`InMemoryTodoRepository`）は SQLDelight 実装と同じ並び順を再現しています。

共通テストは JVM 上で実行する `testAndroidHostTest` を日常的に使う想定です。
`./gradlew :shared:allTests` はこれに加えて iOS とブラウザでも実行するため、
iOS シミュレータのランタイムと Chrome が必要になります。

## バージョン

依存のバージョンは `gradle/libs.versions.toml` に集約しています（2026-09 時点の安定版）。

- Kotlin 2.4.20
- Compose Multiplatform 1.12.0
- Android Gradle Plugin 9.4.0
- SQLDelight 2.3.2
- Gradle 9.7.1 / JDK 21
