# TodoKmp

Kotlin Multiplatform + Compose Multiplatform で作った Todo 管理アプリです。
Android / iOS / Web (Wasm) の 3 プラットフォームで、ドメイン・状態管理・UI をすべて共有しています。

## 機能

- Todo の追加・削除
- 詳細画面での編集（タイトル・メモ・期限・優先度・タグ）
- 完了 / 未完了の切り替え
- 「すべて / 未完了 / 完了」での絞り込み
- 「作成順 / 期限順 / 優先度順」での並べ替え
- 一覧での期限・優先度・タグの表示（期限切れは色を変える）
- 完了済みの一括削除
- 未完了件数・全件数の表示
- データの永続化（アプリを閉じても残る）
- ダークモード対応

### 詳細画面

一覧から 1 件を開くと、タイトル・メモに加えて期限・優先度・タグを編集できます。

| 項目 | 内容 |
| --- | --- |
| 期限 | 日時まで指定する。未設定も可 |
| 優先度 | 高 / 中 / 低 / なし。既定はなし |
| タグ | 1 件に複数付けられる。自由入力で、他の Todo で使ったタグは候補として出る |

並べ替えはどの順でも「未完了が先」で、完了済みが未完了より上に来ることはありません。
期限順では期限なしが、優先度順では優先度なしが最後に回ります。
決着がつかない場合は作成が新しい順です。

この規則は Kotlin 側（`shared/src/commonMain` の `domain/Todo.kt`）と
TypeScript 側（`web/src/todoStore.ts`）にそれぞれ実装があります。
Web の 2 実装で並びがずれないよう、変更するときは両方を直してください。

## モジュール構成

```
todo-kmp/
├── shared/          Kotlin Multiplatform ライブラリ（アプリの中身はすべてここ）
│   └── src/
│       ├── commonMain/     ドメイン・ViewModel・SQLDelight スキーマとマイグレーション（Compose 非依存）
│       ├── composeMain/    Compose UI（Android / iOS / wasmJs で共有）
│       ├── webMain/        localStorage 実装（js / wasmJs で共有）
│       ├── androidMain/    Android 用の SQLite ドライバと setContent
│       ├── iosMain/        iOS 用の SQLite ドライバと UIViewController
│       ├── wasmJsMain/     Compose 版 Web のエントリポイント
│       └── jsMain/         TypeScript 版 Web 向けの facade（TodoStore）
├── androidApp/      Android アプリの器（Activity と Manifest のみ）
├── iosApp/          Xcode プロジェクト（SwiftUI から Compose を埋め込む）
└── web/             TypeScript / React 版の Web アプリ（Vite）
```

Web は 2 通りの実装を並行して置いています。詳細は「Web の 2 つの実装」を参照してください。

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
| Web の UI | Compose 版と TypeScript / React 版の 2 通り | 「Web の 2 つの実装」を参照 |
| DI | 手動 DI | 依存が少ないため、ライブラリを入れずコンストラクタ渡しで完結させている |
| 画面遷移 | 自前 | 一覧と詳細の 2 枚のみ。Compose 版は状態、TypeScript 版は History API |

## Web の 2 つの実装

Web は 2 通りの実装を並行して置いており、どちらも同じ機能・同じ配色です。
共有しているのはドメイン・リポジトリ・永続化で、違うのは UI の作り方だけです。

| | Compose Multiplatform 版 | TypeScript / React 版 |
| --- | --- | --- |
| 場所 | `shared/src/wasmJsMain` | `web/` |
| UI の言語 | Kotlin（Android / iOS と共通） | TypeScript |
| 描画 | canvas | DOM |
| CSS | 当てられない | 当てられる |
| テキスト選択・ブラウザ内検索 | 不可 | 可 |
| スクリーンリーダー | 弱い | 通常の HTML と同じ |
| 画面遷移 | アプリ内の状態（URL は変わらない） | URL（`/todo/<id>`）・ブラウザの戻るが効く |
| 期限の入力 | テキスト（`2026-09-30 18:00` 形式） | `<input type="datetime-local">` |
| 転送サイズ (gzip) | 約 4,545 KB | 約 183 KB |
| UI の保守 | モバイルと 1 つ | Web 用に別途必要 |

Compose 版は canvas に描画するため、UI コードをモバイルとそのまま共有できる代わりに、
DOM が存在しないことに由来する制約（CSS・テキスト選択・アクセシビリティ）と
バンドルサイズを引き受けることになります。

詳細画面への遷移もこの違いが出ます。Compose 版は canvas なので URL を持てず、
どちらの画面を出しているかをアプリ内の状態として持っています。
TypeScript 版は History API を直接使い、`/todo/<id>` を URL として扱うため、
リロード・ブックマーク・ブラウザの戻るがそのまま動きます。
画面が 2 枚しかないので、どちらもナビゲーションライブラリは入れていません。

TypeScript 版では Kotlin 側は UI を持たず、`shared/src/jsMain` の `TodoStore` だけを
JS ライブラリとして公開しています。`@JsExport` は suspend 関数・`Flow`・`Long` を
そのまま扱えないため、この facade で以下のように変換しています。

- Flow の購読 → コールバック（戻り値の関数で解除）
- suspend 関数 → `Promise`
- `Long` の ID → `string`（JS の `number` では 53bit を超える値を表現できないため）

Kotlin から `.d.mts` を生成しているので、TypeScript 側は型付きで扱えます。

### DB のスキーマを変えるとき

`shared/src/commonMain/sqldelight/com/example/todo/db/` に、
最新のスキーマ（`Todo.sq`）とマイグレーション（`migrations/*.sqm`）を置いています。
`.sqm` はファイル名がバージョン番号で、`1.sqm` は v1 の DB を v2 に上げるものです。

列やテーブルを足すときは、`Todo.sq` を直すだけでなく `.sqm` も足してください。
`Todo.sq` だけを直すと新規インストールでは動きますが、
すでにアプリを入れている環境では列が無いまま起動して落ちます。

両者が食い違っていないかは `TodoDatabaseMigrationTest` が確認します
（新規作成した DB と、v1 から migrate した DB の列を突き合わせています）。

### Web だけ SQLDelight を使っていない理由

SQLDelight の Wasm 向けドライバ（`web-worker-driver` + sql.js）は
データベースをメモリ上にしか保持できず、ページをリロードすると内容が消えます。
Todo アプリとしては保存されないと意味がないため、Web だけ localStorage を使っています。

差し替わるのは `TodoRepository` の実装クラス 1 つだけで、
ドメイン・ViewModel・UI はすべて共通のままです。

## セットアップ

### 1. ビルドツール（mise）

JDK・Gradle・Node は [mise](https://mise.jdx.dev/) で固定しています。

```sh
mise trust     # 初回のみ。.mise.toml を信頼する
mise install   # 下記のバージョンを導入する
```

| ツール | バージョン | 用途 |
| --- | --- | --- |
| Java (Temurin) | 21.0.12+101.0.LTS | Kotlin 2.4 / AGP 9 の要件は JDK 17 以上 |
| Gradle | 9.7.1 | AGP 9 の要件は Gradle 9.1 以上 |
| Node | 26.3.0 | `web/`（TypeScript / React 版）のビルド |

`.mise.toml` では前方一致ではなく完全一致でバージョンを指定しています。
`temurin-21` のような書き方だとその時点の最新 21 系に解決されてしまい、
環境や時期によってビルドに使われる JDK が変わるためです。

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

### Web（Compose Multiplatform 版）

```sh
./gradlew :shared:wasmJsBrowserDevelopmentRun    # 開発サーバを起動する
./gradlew :shared:wasmJsBrowserDistribution      # 配布用の静的ファイルを出力する
```

Wasm GC に対応したブラウザが必要です（Chrome 119 以降、Firefox 120 以降、Safari 18.4 以降）。

### Web（TypeScript / React 版）

Kotlin 側のライブラリを先にビルドしてから、npm の依存を入れます。

```sh
./gradlew :shared:jsBrowserProductionLibraryDistribution   # Kotlin のロジックを JS ライブラリとして出力
cd web && npm install                                      # 初回のみ
npm run dev                                                # 開発サーバ (http://localhost:5173)
npm run build                                              # 本番ビルド (web/dist)
```

`web/package.json` は Kotlin の出力を `file:../shared/build/dist/js/productionLibrary` として参照しています。
Kotlin 側の公開 API（`shared/src/jsMain` の `TodoStore`）を変えたときは、
Gradle のビルドをやり直してから `npm install` し直してください。

詳細画面を `/todo/<id>` という URL で持っているため、配信する側で
「どのパスでも `index.html` を返す」設定が要ります（いわゆる SPA フォールバック）。
`npm run dev` と `npm run preview` は Vite が既定で面倒を見てくれるので、
そのままで動きます。`dist` を別のサーバーに置くときだけ設定してください。

## テスト

```sh
./gradlew :shared:testAndroidHostTest
```

- `shared/src/commonTest` … ViewModel の状態遷移、絞り込み・並べ替え、タグの正規化。
  テスト用のリポジトリ実装（`InMemoryTodoRepository`）は SQLDelight 実装と同じ並び順を再現しています。
- `shared/src/androidHostTest` … DB のマイグレーション。
  実際に SQLite 上で v1 の DB を作って migrate するため、JVM で動くこちらに置いています。

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
