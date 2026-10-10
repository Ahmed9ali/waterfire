package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.data.preferences.WallpaperMode
import java.io.File

@Composable
fun WallpaperBackground(
    mode: WallpaperMode,
    solidColorHex: String,
    customImagePath: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (mode) {
            WallpaperMode.GRADIENT_1 -> {
                // Sunset Ember: Deep Navy #0B0F19 to Rich Sunset Orange #DE5833
                val brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070B14),
                        Color(0xFF131A2E),
                        Color(0xFF421D1E),
                        Color(0xFFDE5833)
                    )
                )
                GradientCanvasWithDots(brush = brush, dotColor = Color.White.copy(alpha = 0.08f))
            }
            WallpaperMode.GRADIENT_2 -> {
                // Midnight Purple: Dark Obsidian #0A0612 to Coral Violet #882B63 to #FF5722
                val brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090514),
                        Color(0xFF1D0E30),
                        Color(0xFF5B1648),
                        Color(0xFFFF5722)
                    )
                )
                GradientCanvasWithDots(brush = brush, dotColor = Color.White.copy(alpha = 0.09f))
            }
            WallpaperMode.GRADIENT_3 -> {
                // Ocean Abyss: Oceanic Teal #04121A to Electric Amber #006064 to #FF6F00
                val brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030D14),
                        Color(0xFF0A2533),
                        Color(0xFF0D47A1),
                        Color(0xFFFF6D00)
                    )
                )
                GradientCanvasWithDots(brush = brush, dotColor = Color.White.copy(alpha = 0.08f))
            }
            WallpaperMode.GRADIENT_4 -> {
                // Vulcan Flame: Charcoal Obsidian #101012 to Intense Flame #C43D1A / #FF9100
                val brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E0E10),
                        Color(0xFF1F1E24),
                        Color(0xFF6B1D11),
                        Color(0xFFFF6D00)
                    )
                )
                GradientCanvasWithDots(brush = brush, dotColor = Color.White.copy(alpha = 0.08f))
            }
            WallpaperMode.GRADIENT_5 -> {
                // Cyber Twilight: Deep Twilight Indigo #08081A to Radiant Tangerine #4A1A3A / #FF7043
                val brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070817),
                        Color(0xFF181538),
                        Color(0xFF4C1D42),
                        Color(0xFFFF7043)
                    )
                )
                GradientCanvasWithDots(brush = brush, dotColor = Color.White.copy(alpha = 0.08f))
            }
            WallpaperMode.SOLID_COLOR -> {
                val parsedColor = remember(solidColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(solidColorHex))
                    } catch (e: Exception) {
                        Color(0xFF16171B)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(parsedColor)
                )
            }
            WallpaperMode.GALLERY_IMAGE -> {
                val bitmap = remember(customImagePath) {
                    if (customImagePath.isNotBlank()) {
                        val file = File(customImagePath)
                        if (file.exists()) {
                            try {
                                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                    } else null
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Custom Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback to default gradient
                    val brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0B0F19), Color(0xFF1E1728), Color(0xFFDE5833))
                    )
                    GradientCanvasWithDots(brush = brush, dotColor = Color.White.copy(alpha = 0.08f))
                }
            }
        }

        // Subtle dark scrim so text and shortcuts stay readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun GradientCanvasWithDots(
    brush: Brush,
    dotColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(brush = brush)

        // Subtle dotted matrix overlay
        val spacing = 28.dp.toPx()
        val radius = 1.2.dp.toPx()
        val cols = (size.width / spacing).toInt() + 1
        val rows = (size.height / spacing).toInt() + 1

        for (c in 0 until cols) {
            for (r in 0 until rows) {
                drawCircle(
                    color = dotColor,
                    radius = radius,
                    center = Offset(c * spacing + (spacing / 2), r * spacing + (spacing / 2))
                )
            }
        }
    }
}
