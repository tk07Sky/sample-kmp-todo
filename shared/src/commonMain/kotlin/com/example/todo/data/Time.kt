package com.example.todo.data

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 現在時刻をエポックミリ秒で返す。
 * 時刻取得箇所をここに集約しておくと、テストで差し替えやすい。
 */
@OptIn(ExperimentalTime::class)
internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
