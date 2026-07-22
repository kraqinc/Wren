package com.wren.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.wren.ide.core.network.ConnectionStatusBanner
import com.wren.ide.core.network.NetworkClient
import com.wren.ide.core.storage.SessionManager
import com.wren.ide.core.storage.StoragePermissionBanner
import com.wren.ide.core.storage.StoragePermissionGate
import com.wren.ide.core.storage.WrenFileStorage
import com.wren.ide.core.theme.WrenTheme
import com.wren.ide.features.ai.AIAgentScreen
import com.wren.ide.features.auth.AuthScreen
import com.wren.ide.features.credits.CreditsScreen
import com.wren.ide.features.editor.IDEWorkspaceScreen
import com.wren.ide.features.owner.OwnerAdminScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(applicationContext)
        NetworkClient.setAuthToken(sessionManager.jwtToken)

        setContent {
            WrenTheme {
                var currentScreen by remember {
                    mutableStateOf(
                        when {
                            !sessionManager.isLoggedIn -> "auth"
                            WrenFileStorage.hasAllFilesAccess() -> "workspace"
                            else -> "storage_permission"
                        }
                    )
                }

                fun openWorkspaceOrPermission() {
                    currentScreen = if (WrenFileStorage.hasAllFilesAccess()) {
                        "workspace"
                    } else {
                        "storage_permission"
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    ConnectionStatusBanner()

                    Crossfade(
                        targetState = currentScreen,
                        animationSpec = tween(durationMillis = 260),
                        label = "wren_screen",
                        modifier = Modifier.weight(1f)
                    ) { screen ->
                        when (screen) {
                            "auth" -> {
                                AuthScreen(
                                    sessionManager = sessionManager,
                                    onAuthSuccess = { openWorkspaceOrPermission() }
                                )
                            }

                            "storage_permission" -> {
                                StoragePermissionGate(
                                    onGranted = { currentScreen = "workspace" }
                                )
                            }

                            "workspace" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    StoragePermissionBanner()
                                    Box(modifier = Modifier.weight(1f)) {
                                        IDEWorkspaceScreen(
                                            sessionManager = sessionManager,
                                            onNavToAI = { currentScreen = "ai_agent" },
                                            onNavToCredits = { currentScreen = "credits" },
                                            onNavToOwner = { currentScreen = "owner_dashboard" },
                                            onLogout = {
                                                sessionManager.clearSession()
                                                NetworkClient.setAuthToken(null)
                                                currentScreen = "auth"
                                            }
                                        )
                                    }
                                }
                            }

                            "ai_agent" -> {
                                AIAgentScreen(
                                    sessionManager = sessionManager,
                                    onNavBack = { currentScreen = "workspace" }
                                )
                            }

                            "credits" -> {
                                CreditsScreen(
                                    sessionManager = sessionManager,
                                    onNavBack = { currentScreen = "workspace" }
                                )
                            }

                            "owner_dashboard" -> {
                                OwnerAdminScreen(
                                    sessionManager = sessionManager,
                                    onNavBack = { currentScreen = "workspace" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
