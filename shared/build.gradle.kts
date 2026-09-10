import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    // 下で dependsOn を明示的に足しており、そのままだと既定の階層テンプレートが
    // 適用されなくなる（iosMain がどのコンパイルにも繋がらなくなる）ため、明示的に呼ぶ。
    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.example.todo.shared"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        // commonTest を JVM 上で実行できるようにする。
        // iOS のテストはシミュレータ、Web のテストはブラウザが必要なため、
        // 日常的に回すのはこのホストテストを想定している。
        withHostTestBuilder {}.configure {}
    }

    // iOS 実機 (arm64) と Apple Silicon シミュレータ。
    // Compose Multiplatform 1.12 は Intel シミュレータ (iosX64) 向けを配信していないため対象外。
    // Xcode からは ComposeApp.framework として参照する。
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // Compose Multiplatform 版の Web。canvas に描画する。
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    // TypeScript / React 版の Web 向け。UI は持たず、ロジックを JS ライブラリとして出力する。
    // 型定義 (.d.ts) も生成するので、TS 側から型付きで扱える。
    js {
        // Vite からそのまま import できるよう UMD ではなく ES モジュールで出力する
        useEsModules()
        outputModuleName.set("todo-shared")
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        val commonMain = getByName("commonMain")

        // Compose UI を置く中間ソースセット。
        // js ターゲットには Compose UI を載せないため、commonMain には持ち込まずここに閉じ込める。
        val composeMain = create("composeMain").apply { dependsOn(commonMain) }

        // js と wasmJs の共通ソースセット。既定の階層テンプレートが用意してくれるので、
        // localStorage 実装はそこへ置けば両方から使える。
        val webMain = getByName("webMain")

        getByName("androidMain") { dependsOn(composeMain) }
        getByName("iosMain") { dependsOn(composeMain) }
        getByName("wasmJsMain") { dependsOn(composeMain) }

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            // Compose に依存しない ViewModel 本体のみ
            implementation(libs.lifecycle.viewmodel)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
        }

        composeMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
        }

        webMain.dependencies {
            // ブラウザ API（localStorage / document）へのアクセス
            implementation(libs.kotlinx.browser)
        }

        getByName("androidMain").dependencies {
            // androidApp 側に Compose を書かずに済むよう、setContent もここで面倒を見る
            implementation(libs.androidx.activity.compose)
            implementation(libs.sqldelight.android.driver)
        }

        getByName("iosMain").dependencies {
            implementation(libs.sqldelight.native.driver)
        }

        getByName("jsMain").dependencies {
            // このターゲットに Compose の UI は載せないが、Compose コンパイラプラグインは
            // 全コンパイルに適用されるため、runtime がクラスパスに無いとエラーになる。
            // 参照が無いので出力には含まれない。
            implementation(compose.runtime)
        }

        getByName("commonTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

sqldelight {
    databases {
        create("TodoDatabase") {
            packageName.set("com.example.todo.db")
        }
    }
}
