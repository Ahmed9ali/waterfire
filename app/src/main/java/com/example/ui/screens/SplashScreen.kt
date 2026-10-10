package com.example.ui.screens

import android.webkit.CookieManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.DarkBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_waterfox_logo),
                contentDescription = "WaterFox Logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "WaterFox",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Private. Fast. Yours.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = BrandOrange
            )
        }
    }

    LaunchedEffect(Unit) {
        // Parallel pre-warming of CookieManager and WebView subsystem
        launch(Dispatchers.IO) {
            try {
                CookieManager.getInstance().flush()
            } catch (e: Exception) {}
        }
        // Keep splash strictly under 600 ms as requested (400 ms)
        delay(400)
        onSplashFinished()
    }
}
