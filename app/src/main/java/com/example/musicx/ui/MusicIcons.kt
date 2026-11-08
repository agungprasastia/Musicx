package com.example.musicx.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object MusicIcons {
    private val stroke = SolidColor(Color.White)

    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = stroke,
                stroke = stroke,
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7.5f, 5.5f)
                lineTo(18.5f, 12f)
                lineTo(7.5f, 18.5f)
                close()
            }
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = stroke,
                stroke = stroke,
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 5.5f)
                lineTo(9.5f, 5.5f)
                lineTo(9.5f, 18.5f)
                lineTo(7f, 18.5f)
                close()
                moveTo(14.5f, 5.5f)
                lineTo(17f, 5.5f)
                lineTo(17f, 18.5f)
                lineTo(14.5f, 18.5f)
                close()
            }
        }.build()
    }

    val SkipNext: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_skip_next",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = stroke,
                stroke = stroke,
                strokeLineWidth = 1.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 5.5f)
                lineTo(14.5f, 12f)
                lineTo(6f, 18.5f)
                close()
                moveTo(16.5f, 5.5f)
                lineTo(18.5f, 5.5f)
                lineTo(18.5f, 18.5f)
                lineTo(16.5f, 18.5f)
                close()
            }
        }.build()
    }

    val SkipPrevious: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_skip_previous",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = stroke,
                stroke = stroke,
                strokeLineWidth = 1.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 5.5f)
                lineTo(9.5f, 12f)
                lineTo(18f, 18.5f)
                close()
                moveTo(5.5f, 5.5f)
                lineTo(7.5f, 5.5f)
                lineTo(7.5f, 18.5f)
                lineTo(5.5f, 18.5f)
                close()
            }
        }.build()
    }

    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = stroke,
                stroke = stroke,
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3.8f)
                lineTo(20f, 10.5f)
                lineTo(20f, 20f)
                lineTo(14.5f, 20f)
                lineTo(14.5f, 14.5f)
                lineTo(9.5f, 14.5f)
                lineTo(9.5f, 20f)
                lineTo(4f, 20f)
                lineTo(4f, 10.5f)
                close()
            }
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.5f, 10.5f)
                arcToRelative(6.5f, 6.5f, 0f, true, true, 13f, 0f)
                arcToRelative(6.5f, 6.5f, 0f, true, true, -13f, 0f)
                moveTo(15.5f, 15.5f)
                lineTo(20.5f, 20.5f)
            }
        }.build()
    }

    val Library: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_library",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4.5f); lineTo(4f, 19.5f)
                moveTo(9f, 4.5f); lineTo(9f, 19.5f)
                moveTo(14f, 4.5f); lineTo(14f, 19.5f)
                moveTo(17.5f, 5.5f); lineTo(20.5f, 19f)
            }
        }.build()
    }

    val Spotify: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_spotify",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Soundwave arcs inside circular feel
                moveTo(5f, 9f)
                quadTo(12f, 6.5f, 19f, 9f)
                moveTo(6.5f, 12.5f)
                quadTo(12f, 10.5f, 17.5f, 12.5f)
                moveTo(8f, 16f)
                quadTo(12f, 14.5f, 16f, 16f)
            }
        }.build()
    }

    val Queue: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_queue",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(8f, 6f); lineTo(21f, 6f)
                moveTo(8f, 12f); lineTo(21f, 12f)
                moveTo(8f, 18f); lineTo(21f, 18f)
            }
            path(fill = stroke) {
                for (y in listOf(6f, 12f, 18f)) {
                    moveTo(4.5f, y)
                    arcToRelative(1.5f, 1.5f, 0f, true, true, -3f, 0f)
                    arcToRelative(1.5f, 1.5f, 0f, true, true, 3f, 0f)
                    close()
                }
            }
        }.build()
    }

    val Lyrics: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_lyrics",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 4f)
                lineTo(18f, 4f)
                quadTo(21f, 4f, 21f, 7f)
                lineTo(21f, 14f)
                quadTo(21f, 17f, 18f, 17f)
                lineTo(10f, 17f)
                lineTo(6f, 20f)
                lineTo(6f, 17f)
                quadTo(3f, 17f, 3f, 14f)
                lineTo(3f, 7f)
                quadTo(3f, 4f, 6f, 4f)
                close()
                moveTo(7.5f, 8.5f); lineTo(16.5f, 8.5f)
                moveTo(7.5f, 12.5f); lineTo(13.5f, 12.5f)
            }
        }.build()
    }

    val ChevronDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_chevron_down",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 9f)
                lineTo(12f, 15f)
                lineTo(18f, 9f)
            }
        }.build()
    }

    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_chevron_right",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 6f)
                lineTo(15f, 12f)
                lineTo(9f, 18f)
            }
        }.build()
    }

    val Speed: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_speed",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.5f, 16.5f)
                arcToRelative(8.5f, 8.5f, 0f, true, true, 15f, 0f)
                moveTo(12f, 15f)
                lineTo(16f, 9.5f)
                moveTo(5f, 19.5f)
                lineTo(19f, 19.5f)
            }
        }.build()
    }

    val Timer: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_timer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.5f, 12f)
                arcToRelative(8.5f, 8.5f, 0f, true, true, 17f, 0f)
                arcToRelative(8.5f, 8.5f, 0f, true, true, -17f, 0f)
                moveTo(12f, 7.5f)
                lineTo(12f, 12f)
                lineTo(15.5f, 14f)
            }
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 7f); lineTo(19f, 7f)
                moveTo(5f, 12f); lineTo(19f, 12f)
                moveTo(5f, 17f); lineTo(19f, 17f)
                moveTo(9f, 5.5f); lineTo(9f, 8.5f)
                moveTo(15f, 10.5f); lineTo(15f, 13.5f)
                moveTo(11f, 15.5f); lineTo(11f, 18.5f)
            }
        }.build()
    }

    val Shuffle: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_shuffle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 7f); lineTo(7f, 7f); lineTo(17f, 17f); lineTo(20f, 17f)
                moveTo(17f, 14f); lineTo(20f, 17f); lineTo(17f, 20f)
                moveTo(4f, 17f); lineTo(7f, 17f); lineTo(9.5f, 14.5f)
                moveTo(14.5f, 9.5f); lineTo(17f, 7f); lineTo(20f, 7f)
                moveTo(17f, 4f); lineTo(20f, 7f); lineTo(17f, 10f)
            }
        }.build()
    }

    val Repeat: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_repeat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(5f, 8f); lineTo(17f, 8f); arcToRelative(3f, 3f, 0f, false, true, 3f, 3f); lineTo(20f, 13f)
                moveTo(17f, 5f); lineTo(20f, 8f); lineTo(17f, 11f)
                moveTo(19f, 16f); lineTo(7f, 16f); arcToRelative(3f, 3f, 0f, false, true, -3f, -3f); lineTo(4f, 11f)
                moveTo(7f, 19f); lineTo(4f, 16f); lineTo(7f, 13f)
            }
        }.build()
    }

    val Heart: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_heart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 20f)
                lineTo(10.55f, 18.7f)
                quadTo(5.4f, 14.05f, 3.2f, 11.1f)
                quadTo(1f, 8.15f, 2.5f, 4.95f)
                quadTo(4f, 1.75f, 8.5f, 3.25f)
                quadTo(10.7f, 4f, 12f, 6.1f)
                quadTo(13.3f, 4f, 15.5f, 3.25f)
                quadTo(20f, 1.75f, 21.5f, 4.95f)
                quadTo(23f, 8.15f, 20.8f, 11.1f)
                quadTo(18.6f, 14.05f, 13.45f, 18.7f)
                close()
            }
        }.build()
    }

    val Plus: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(stroke = stroke, strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 5f); lineTo(12f, 19f)
                moveTo(5f, 12f); lineTo(19f, 12f)
            }
        }.build()
    }

    val Trash: ImageVector by lazy {
        ImageVector.Builder(
            name = "ic_trash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 7f); lineTo(20f, 7f)
                moveTo(10f, 11f); lineTo(10f, 17f)
                moveTo(14f, 11f); lineTo(14f, 17f)
                moveTo(5f, 7f); lineTo(6f, 19f)
                quadTo(6f, 20f, 7f, 20f)
                lineTo(17f, 20f)
                quadTo(18f, 20f, 18f, 19f)
                lineTo(19f, 7f)
                moveTo(9f, 7f); lineTo(9f, 4f)
                lineTo(15f, 4f); lineTo(15f, 7f)
            }
        }.build()
    }
}
