plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidLibrary {
        namespace = "com.example.todo.shared"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        // commonTest を JVM 上で実行できるようにする。
        // iOS のテストはシミュレータ、Wasm のテストはブラウザが必要なため、
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

    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
        }

        androidMain.dependencies {
            // androidApp 側に Compose を書かずに済むよう、setContent もここで面倒を見る
            implementation(libs.androidx.activity.compose)
            implementation(libs.sqldelight.android.driver)
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }

        wasmJsMain.dependencies {
            // ブラウザ API（localStorage / document）へのアクセス
            implementation(libs.kotlinx.browser)
        }

        commonTest.dependencies {
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
