package com.example.todo.ui

import com.example.todo.domain.Priority
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * 日時を "2026-09-11 01:23" の形で返す。作成日時と期限の両方で使う。
 *
 * LocalDateTime のプロパティ名は kotlinx-datetime のバージョンで変わることがあるため、
 * ISO 文字列（2026-09-11T01:23:45.123）を整形する方法を採って将来の改名に影響されないようにしている。
 */
@OptIn(ExperimentalTime::class)
fun formatDateTime(epochMillis: Long): String =
    Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toString()
        .replace('T', ' ')
        .take(16)

/**
 * [formatDateTime] と同じ "2026-09-30 18:00" 形式の入力を解釈してエポックミリ秒にする。
 * 空文字（期限なし）と解釈できない文字列はどちらも null を返すため、
 * 入力エラーとして扱いたい側は「空でないのに null」で判定する。
 */
@OptIn(ExperimentalTime::class)
fun parseDateTimeInput(text: String): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    return runCatching {
        LocalDateTime.parse(trimmed.replace(' ', 'T'))
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    }.getOrNull()
}

/** 画面に出す優先度の名前。 */
val Priority.label: String
    get() = when (this) {
        Priority.High -> "優先度 高"
        Priority.Medium -> "優先度 中"
        Priority.Low -> "優先度 低"
        Priority.None -> "優先度 なし"
    }

/** 選択肢として並べる短い名前。 */
val Priority.shortLabel: String
    get() = when (this) {
        Priority.High -> "高"
        Priority.Medium -> "中"
        Priority.Low -> "低"
        Priority.None -> "なし"
    }
