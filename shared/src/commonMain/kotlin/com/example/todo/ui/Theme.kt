package com.example.todo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Material 3 のカラーロールは一部だけ指定すると、残りが既定のパープル系のまま残り
// 配色がちぐはぐになる。そのため主要なロールをひととおり指定している。
// ベースは緑系の色相。

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4F1CE),
    onPrimaryContainer = Color(0xFF002114),
    inversePrimary = Color(0xFF99D5B3),

    secondary = Color(0xFF4E6355),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8D6),
    onSecondaryContainer = Color(0xFF0B1F14),

    tertiary = Color(0xFF3B6470),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFE9F8),
    onTertiaryContainer = Color(0xFF001F27),

    background = Color(0xFFFBFDF8),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF404942),
    surfaceTint = Color(0xFF2E6B4F),
    inverseSurface = Color(0xFF2E312F),
    inverseOnSurface = Color(0xFFEFF1EC),

    surfaceBright = Color(0xFFFBFDF8),
    surfaceDim = Color(0xFFDBDED9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F8F2),
    surfaceContainer = Color(0xFFEFF2EC),
    surfaceContainerHigh = Color(0xFFEAEDE7),
    surfaceContainerHighest = Color(0xFFE4E7E1),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = Color(0xFF707974),
    outlineVariant = Color(0xFFC0C9C0),
    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF99D5B3),
    onPrimary = Color(0xFF003825),
    primaryContainer = Color(0xFF135237),
    onPrimaryContainer = Color(0xFFB4F1CE),
    inversePrimary = Color(0xFF2E6B4F),

    secondary = Color(0xFFB5CCBB),
    onSecondary = Color(0xFF213528),
    secondaryContainer = Color(0xFF374B3E),
    onSecondaryContainer = Color(0xFFD0E8D6),

    tertiary = Color(0xFFA4CDDC),
    onTertiary = Color(0xFF063541),
    tertiaryContainer = Color(0xFF234C58),
    onTertiaryContainer = Color(0xFFBFE9F8),

    background = Color(0xFF111412),
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF111412),
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = Color(0xFF404942),
    onSurfaceVariant = Color(0xFFC0C9C0),
    surfaceTint = Color(0xFF99D5B3),
    inverseSurface = Color(0xFFE1E3DE),
    inverseOnSurface = Color(0xFF2E312F),

    surfaceBright = Color(0xFF373A37),
    surfaceDim = Color(0xFF111412),
    surfaceContainerLowest = Color(0xFF0C0F0D),
    surfaceContainerLow = Color(0xFF191C1A),
    surfaceContainer = Color(0xFF1D201E),
    surfaceContainerHigh = Color(0xFF272B28),
    surfaceContainerHighest = Color(0xFF323532),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF404942),
    scrim = Color(0xFF000000),
)

/** アプリ共通のテーマ。端末のダークモード設定に追従する。 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
