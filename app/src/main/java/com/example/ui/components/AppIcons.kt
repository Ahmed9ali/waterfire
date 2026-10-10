package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Material Symbols Outlined (24dp, weight 400) as Compose vectors.
 * No extra dependency needed. Use with: Icon(imageVector = AppIcons.Home, ...)
 */
object AppIcons {

    val Home: ImageVector by lazy {
        symbol("home") {
            moveTo(6f, 19f)
            horizontalLineTo(9f)
            verticalLineTo(13f)
            horizontalLineToRelative(6f)
            verticalLineToRelative(6f)
            horizontalLineToRelative(3f)
            verticalLineTo(10f)
            lineTo(12f, 5.5f)
            lineTo(6f, 10f)
            verticalLineToRelative(9f)
            close()
            moveTo(4f, 21f)
            verticalLineTo(9f)
            lineTo(12f, 3f)
            lineToRelative(8f, 6f)
            verticalLineTo(21f)
            horizontalLineTo(13f)
            verticalLineTo(15f)
            horizontalLineTo(11f)
            verticalLineToRelative(6f)
            horizontalLineTo(4f)
            close()
            moveToRelative(8f, -8.75f)
            close()
        }
    }

    val Bookmark: ImageVector by lazy {
        symbol("bookmark") {
            bookmarkBody()
        }
    }

    val BookmarkCheck: ImageVector by lazy {
        symbol("bookmark_check") {
            moveTo(10.95f, 14f)
            lineTo(15.9f, 9.05f)
            lineTo(14.48f, 7.65f)
            lineToRelative(-3.53f, 3.53f)
            lineTo(9.53f, 9.75f)
            lineTo(8.1f, 11.18f)
            lineTo(10.95f, 14f)
            close()
            bookmarkBody()
        }
    }

    val Search: ImageVector by lazy {
        symbol("search") {
            moveTo(19.6f, 21f)
            lineTo(13.3f, 14.7f)
            quadToRelative(-0.75f, 0.6f, -1.72f, 0.95f)
            reflectiveQuadTo(9.5f, 16f)
            quadTo(6.78f, 16f, 4.89f, 14.11f)
            quadTo(3f, 12.23f, 3f, 9.5f)
            quadTo(3f, 6.77f, 4.89f, 4.89f)
            reflectiveQuadTo(9.5f, 3f)
            reflectiveQuadToRelative(4.61f, 1.89f)
            reflectiveQuadTo(16f, 9.5f)
            quadToRelative(0f, 1.1f, -0.35f, 2.07f)
            reflectiveQuadTo(14.7f, 13.3f)
            lineTo(21f, 19.6f)
            lineTo(19.6f, 21f)
            close()
            moveTo(9.5f, 14f)
            quadToRelative(1.88f, 0f, 3.19f, -1.31f)
            reflectiveQuadTo(14f, 9.5f)
            reflectiveQuadTo(12.69f, 6.31f)
            reflectiveQuadTo(9.5f, 5f)
            reflectiveQuadTo(6.31f, 6.31f)
            reflectiveQuadTo(5f, 9.5f)
            reflectiveQuadToRelative(1.31f, 3.19f)
            reflectiveQuadTo(9.5f, 14f)
            close()
        }
    }

    val MoreVert: ImageVector by lazy {
        symbol("more_vert") {
            moveTo(12f, 20f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(10f, 18f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(12f, 16f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 17.18f, 14f, 18f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(12f, 20f)
            close()
            moveToRelative(0f, -6f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(10f, 12f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(12f, 10f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 11.18f, 14f, 12f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(12f, 14f)
            close()
            moveTo(12f, 8f)
            quadTo(11.18f, 8f, 10.59f, 7.41f)
            reflectiveQuadTo(10f, 6f)
            reflectiveQuadTo(10.59f, 4.59f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 5.18f, 14f, 6f)
            reflectiveQuadTo(13.41f, 7.41f)
            reflectiveQuadTo(12f, 8f)
            close()
        }
    }

    // Outline shared by bookmark and bookmark_check
    private fun PathBuilder.bookmarkBody() {
        moveTo(5f, 21f)
        verticalLineTo(5f)
        quadTo(5f, 4.17f, 5.59f, 3.59f)
        reflectiveQuadTo(7f, 3f)
        horizontalLineTo(17f)
        quadToRelative(0.82f, 0f, 1.41f, 0.59f)
        reflectiveQuadTo(19f, 5f)
        verticalLineTo(21f)
        lineTo(12f, 18f)
        lineTo(5f, 21f)
        close()
        moveTo(7f, 17.95f)
        lineTo(12f, 15.8f)
        lineToRelative(5f, 2.15f)
        verticalLineTo(5f)
        horizontalLineTo(7f)
        verticalLineTo(17.95f)
        close()
        moveTo(7f, 5f)
        horizontalLineTo(17f)
        horizontalLineTo(12f)
        horizontalLineTo(7f)
        close()
    }

    private fun symbol(name: String, pathBuilder: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            .path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathBuilder = pathBuilder
            )
            .build()
}
