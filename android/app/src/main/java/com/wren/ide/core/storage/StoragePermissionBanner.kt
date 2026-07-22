package com.wren.ide.core.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wren.ide.core.theme.BorderGray
import com.wren.ide.core.theme.ElectricCyan
import com.wren.ide.core.theme.PrimaryObsidian
import com.wren.ide.core.theme.SecondaryCard
import com.wren.ide.core.theme.TextLight
import com.wren.ide.core.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun StoragePermissionBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val granted = rememberAllFilesAccessState()

    if (!granted) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = SecondaryCard.copy(alpha = 0.92f),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryObsidian, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wren necesita permiso para crear proyectos reales.",
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Activa el acceso a todos los archivos para guardar carpetas y archivos en tu dispositivo.",
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }

                Button(
                    onClick = { WrenFileStorage.requestAllFilesAccess(context) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = PrimaryObsidian
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
                    Text("Permitir", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun rememberAllFilesAccessState(): Boolean {
    var granted by remember { mutableStateOf(WrenFileStorage.hasAllFilesAccess()) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = WrenFileStorage.hasAllFilesAccess()
            if (current != granted) granted = current
            delay(700)
        }
    }
    return granted
}
