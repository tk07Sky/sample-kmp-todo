package com.example.todo.ui

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * 作成日時を "2026-09-11 01:23" の形で返す。
 *
 * LocalDateTime のプロパティ名は kotlinx-datetime のバージョンで変わることがあるため、
 * ISO 文字列（2026-09-11T01:23:45.123）を整形する方法を採って将来の改名に影響されないようにしている。
 */
@OptIn(ExperimentalTime::class)
fun formatCreatedAt(epochMillis: Long): String =
    Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toString()
        .replace('T', ' ')
        .take(16)
