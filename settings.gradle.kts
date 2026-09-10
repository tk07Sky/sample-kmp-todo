rootProject.name = "TodoKmp"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// shared    : Kotlin Multiplatform ライブラリ。ドメイン・データ・Compose UI をすべて持つ。
//             Android ライブラリ / iOS フレームワーク / Wasm 実行ファイルを出力する。
// androidApp: Android アプリの器。Activity と Manifest だけを持ち、画面は shared に任せる。
include(":shared")
include(":androidApp")
