package com.example.todo.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 画面で使うアイコン。
 *
 * Material のアイコンライブラリ（material-icons-core / -extended）は
 * Compose Multiplatform 1.8 以降で配信されなくなったため、
 * 必要な分だけ ImageVector として持っている。パスは Material Symbols のものと同じ。
 */
object AppIcons {

    val Add: ImageVector = materialIcon(
        name = "Add",
        pathData = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z",
    )

    val Delete: ImageVector = materialIcon(
        name = "Delete",
        pathData = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
    )

    val Back: ImageVector = materialIcon(
        name = "ArrowBack",
        pathData = "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z",
    )

    val Close: ImageVector = materialIcon(
        name = "Close",
        pathData = "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 " +
            "17.59 19 19 17.59 13.41 12z",
    )
}

/** 24dp・24 viewport の Material アイコン相当の ImageVector を組み立てる。 */
private fun materialIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // 実際の色は Icon コンポーザブルが LocalContentColor で塗り替える
        addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
    }.build()
