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

    val DominoMask: ImageVector by lazy {
        symbol("domino_mask") {
            moveTo(7.8f, 18f)
            quadTo(6.53f, 18f, 5.36f, 17.55f)
            reflectiveQuadTo(3.28f, 16.23f)
            quadTo(2.08f, 15.1f, 1.54f, 13.56f)
            quadTo(1f, 12.02f, 1f, 10.38f)
            quadTo(1f, 8.42f, 1.95f, 7.21f)
            reflectiveQuadTo(4.73f, 6f)
            quadTo(5.08f, 6f, 5.39f, 6.06f)
            reflectiveQuadTo(6.03f, 6.25f)
            lineTo(12f, 8.48f)
            lineTo(17.98f, 6.25f)
            quadTo(18.3f, 6.13f, 18.61f, 6.06f)
            reflectiveQuadTo(19.28f, 6f)
            quadToRelative(1.82f, 0f, 2.77f, 1.21f)
            reflectiveQuadTo(23f, 10.38f)
            quadToRelative(0f, 1.65f, -0.54f, 3.19f)
            reflectiveQuadToRelative(-1.74f, 2.66f)
            quadTo(19.8f, 17.1f, 18.64f, 17.55f)
            quadTo(17.48f, 18f, 16.2f, 18f)
            quadToRelative(-1.65f, 0f, -2.8f, -0.75f)
            reflectiveQuadTo(12.25f, 16.5f)
            horizontalLineToRelative(-0.5f)
            quadToRelative(0f, 0f, -1.15f, 0.75f)
            reflectiveQuadTo(7.8f, 18f)
            close()
            moveToRelative(0f, -2f)
            quadToRelative(0.92f, 0f, 1.72f, -0.44f)
            reflectiveQuadTo(11f, 14.5f)
            horizontalLineToRelative(2f)
            quadToRelative(0.68f, 0.63f, 1.48f, 1.06f)
            reflectiveQuadTo(16.2f, 16f)
            quadToRelative(0.9f, 0f, 1.74f, -0.31f)
            quadToRelative(0.84f, -0.31f, 1.49f, -0.96f)
            quadToRelative(0.85f, -0.85f, 1.21f, -2f)
            reflectiveQuadTo(21f, 10.38f)
            quadTo(21f, 9.35f, 20.58f, 8.66f)
            quadTo(20.15f, 7.97f, 19.23f, 8f)
            quadTo(19.15f, 8f, 18.68f, 8.1f)
            lineTo(12f, 10.6f)
            lineTo(5.33f, 8.1f)
            quadTo(5.2f, 8.05f, 5.06f, 8.02f)
            reflectiveQuadTo(4.78f, 8f)
            quadTo(3.85f, 8f, 3.43f, 8.67f)
            reflectiveQuadTo(3f, 10.38f)
            quadToRelative(0f, 1.22f, 0.36f, 2.38f)
            reflectiveQuadToRelative(1.24f, 2f)
            quadToRelative(0.65f, 0.63f, 1.47f, 0.94f)
            reflectiveQuadTo(7.8f, 16f)
            close()
            moveTo(9.03f, 14.5f)
            quadToRelative(0.93f, 0f, 1.45f, -0.41f)
            quadTo(11f, 13.68f, 11f, 12.95f)
            quadTo(11f, 11.73f, 9.39f, 10.61f)
            reflectiveQuadTo(5.98f, 9.5f)
            quadTo(5.05f, 9.5f, 4.53f, 9.91f)
            reflectiveQuadTo(4f, 11.05f)
            quadToRelative(0f, 1.22f, 1.61f, 2.34f)
            reflectiveQuadTo(9.03f, 14.5f)
            close()
            moveTo(8.88f, 13f)
            quadTo(7.93f, 13f, 6.81f, 12.38f)
            reflectiveQuadTo(5.5f, 11.1f)
            quadTo(5.63f, 11.05f, 5.79f, 11.01f)
            reflectiveQuadTo(6.13f, 10.98f)
            quadToRelative(0.95f, 0f, 2.06f, 0.64f)
            reflectiveQuadTo(9.5f, 12.9f)
            quadTo(9.38f, 12.95f, 9.21f, 12.98f)
            reflectiveQuadTo(8.88f, 13f)
            close()
            moveToRelative(6.1f, 1.53f)
            quadToRelative(1.8f, 0f, 3.41f, -1.13f)
            reflectiveQuadTo(20f, 11.05f)
            quadTo(20f, 10.33f, 19.49f, 9.9f)
            quadTo(18.98f, 9.48f, 18.03f, 9.48f)
            quadToRelative(-1.8f, 0f, -3.41f, 1.13f)
            reflectiveQuadTo(13f, 12.95f)
            quadToRelative(0f, 0.72f, 0.53f, 1.15f)
            reflectiveQuadToRelative(1.45f, 0.43f)
            close()
            moveTo(15.13f, 13f)
            quadTo(14.95f, 13f, 14.8f, 12.98f)
            reflectiveQuadTo(14.53f, 12.9f)
            quadToRelative(0.2f, -0.65f, 1.31f, -1.27f)
            reflectiveQuadTo(17.9f, 11f)
            quadToRelative(0.17f, 0f, 0.32f, 0.02f)
            reflectiveQuadTo(18.5f, 11.1f)
            quadToRelative(-0.2f, 0.65f, -1.31f, 1.28f)
            reflectiveQuadTo(15.13f, 13f)
            close()
            moveTo(12f, 12f)
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
