package com.wren.ide.core.network

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wren.ide.core.theme.ErrorRed
import com.wren.ide.core.theme.EditorYellow
import com.wren.ide.core.theme.PrimaryObsidian
import com.wren.ide.core.theme.TextLight

/**
 * Banner flotante que refleja [NetworkClient.connectionState] tal cual es --
 * nunca "no dijo nada" cuando la red falla. Se coloca una sola vez en
 * MainActivity, encima de cualquier pantalla, así que no hay que repetir
 * esta lógica en cada screen.
 */
@Composable
fun ConnectionStatusBanner(modifier: Modifier = Modifier) {
    val state by NetworkClient.connectionState.collectAsState()

    AnimatedVisibility(
        visible = state != ConnectionState.CONNECTED,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        val (bg, icon, text, showRetry) = when (state) {
            ConnectionState.RECONNECTING -> BannerStyle(
                EditorYellow.copy(alpha = 0.16f),
                null,
                "Estamos intentando reconectar con el servidor. Espera…",
                false
            )
            ConnectionState.FAILED -> BannerStyle(
                ErrorRed.copy(alpha = 0.16f),
                Icons.Filled.WifiOff,
                "Lo sentimos, no pudimos reconectar.",
                true
            )
            ConnectionState.CONNECTED -> BannerStyle(Color.Transparent, null, "", false)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryObsidian)
                .background(bg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state == ConnectionState.RECONNECTING) {
                Text("↻", color = EditorYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            } else {
                icon?.let {
                    Icon(it, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = TextLight,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (showRetry) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { NetworkClient.resetConnectionState() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = EditorYellow, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reintentar", color = EditorYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class BannerStyle(
    val background: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
    val text: String,
    val showRetry: Boolean
)
