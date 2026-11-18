package com.github.leaf.hyperportal.bigbang

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 1:1 vector icon set matching the Liquid Glass v2 preview.
 */
internal object LiquidGlassIcons {
    private val StrokeBrush = SolidColor(Color.Black)
    private const val STROKE_WIDTH = 1.9f

    val Edit: ImageVector by lazy {
        ImageVector.Builder(
            name = "LiquidEdit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 20f)
                horizontalLineTo(21f)
            }
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(16.5f, 3.5f)
                arcTo(2.12f, 2.12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19.5f, 6.5f)
                lineTo(7f, 19f)
                lineTo(3f, 20f)
                lineTo(4f, 16f)
                close()
            }
        }.build()
    }

    val SelectAll: ImageVector by lazy {
        ImageVector.Builder(
            name = "LiquidSelectAll",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Rounded square (4,4,16,16, rx=3)
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 4f)
                horizontalLineTo(13f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 7f)
                verticalLineTo(13f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 13f, 16f)
                horizontalLineTo(7f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, 13f)
                verticalLineTo(7f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7f, 4f)
                close()
            }
            // Stacked back card (M8 20h9a3 3 0 0 0 3-3V8)
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 20f)
                horizontalLineTo(17f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 20f, 17f)
                verticalLineTo(8f)
            }
        }.build()
    }

    val Share: ImageVector by lazy {
        ImageVector.Builder(
            name = "LiquidShare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3f)
                verticalLineTo(15f)
            }
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 8f)
                lineTo(12f, 3f)
                lineTo(17f, 8f)
            }
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 14f)
                verticalLineTo(19f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7f, 21f)
                horizontalLineTo(17f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 19f, 19f)
                verticalLineTo(14f)
            }
        }.build()
    }

    val More: ImageVector by lazy {
        ImageVector.Builder(
            name = "LiquidMore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            fun addDot(cx: Float) {
                val r = 1.5f
                path(
                    fill = StrokeBrush,
                    pathFillType = PathFillType.NonZero,
                ) {
                    moveTo(cx - r, 12f)
                    arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, cx + r, 12f)
                    arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, cx - r, 12f)
                    close()
                }
            }
            addDot(6f)
            addDot(12f)
            addDot(18f)
        }.build()
    }

    val Ocr: ImageVector by lazy {
        ImageVector.Builder(
            name = "LiquidOcr",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 7f)
                verticalLineTo(5f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 3f)
                horizontalLineTo(7f)
            }
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(17f, 3f)
                horizontalLineTo(19f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 5f)
                verticalLineTo(7f)
            }
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 17f)
                verticalLineTo(19f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19f, 21f)
                horizontalLineTo(17f)
            }
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 21f)
                horizontalLineTo(5f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 19f)
                verticalLineTo(17f)
            }
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 12f)
                horizontalLineTo(17f)
            }
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "LiquidClose",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6f, 6f)
                lineTo(18f, 18f)
            }
        }.build()
    }

    val Language: ImageVector by lazy {
        ImageVector.Builder(
            name = "LiquidLanguage",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Outer circle cx=12, cy=12, r=9
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 12f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 21f, 12f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 3f, 12f)
                close()
            }
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.6f, 9f)
                horizontalLineTo(20.4f)
            }
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.6f, 15f)
                horizontalLineTo(20.4f)
            }
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3f)
                arcTo(14f, 14f, 0f, isMoreThanHalf = false, isPositiveArc = false, 12f, 21f)
            }
            path(
                stroke = StrokeBrush,
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3f)
                arcTo(14f, 14f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 21f)
            }
        }.build()
    }
}
