package com.wren.ide.core.storage

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wren.ide.core.theme.BorderGray
import com.wren.ide.core.theme.EditorYellow
import com.wren.ide.core.theme.ElectricCyan
import com.wren.ide.core.theme.PrimaryObsidian
import com.wren.ide.core.theme.SecondaryCard
import com.wren.ide.core.theme.TerminalGreen
import com.wren.ide.core.theme.TextLight
import com.wren.ide.core.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun StoragePermissionGate(
    onGranted: () -> Unit
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(WrenFileStorage.hasAllFilesAccess()) }
    val transition = rememberInfiniteTransition(label = "permission_gate")
    val pulse by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(tween(3200)),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        while (true) {
            val current = WrenFileStorage.hasAllFilesAccess()
            if (current != granted) granted = current
            delay(700)
        }
    }

    LaunchedEffect(granted) {
        if (granted) onGranted()
    }

    if (!granted) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PrimaryObsidian)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF07080A),
                            Color(0xFF0B0D10),
                            Color(0xFF07080A)
                        )
                    )
                )
                drawCircle(
                    color = ElectricCyan.copy(alpha = 0.18f * pulse),
                    radius = size.minDimension * 0.36f,
                    center = Offset(size.width * 0.76f, size.height * 0.18f)
                )
                drawCircle(
                    color = TerminalGreen.copy(alpha = 0.10f * pulse),
                    radius = size.minDimension * 0.24f,
                    center = Offset(size.width * 0.20f, size.height * 0.78f)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color(0xFF111215).copy(alpha = 0.94f),
                    shape = RoundedCornerShape(30.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(SecondaryCard, RoundedCornerShape(22.dp))
                                .border(1.dp, BorderGray, RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Wren necesita permiso para crear proyectos en tu dispositivo",
                            color = TextLight,
                            fontSize = 21.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Te agradecemos que actives el acceso a todos los archivos. Así Wren podrá crear carpetas reales, guardar proyectos, abrir la terminal completa y sincronizar tu workspace con el almacenamiento del teléfono.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SecondaryCard, RoundedCornerShape(18.dp))
                                .border(1.dp, BorderGray, RoundedCornerShape(18.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = EditorYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Acceso a todos los archivos",
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { WrenFileStorage.requestAllFilesAccess(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = PrimaryObsidian
                            )
                        ) {
                            Text("Permitir", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Al concederlo, Wren podrá crear y administrar tus proyectos en una carpeta real del dispositivo.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
