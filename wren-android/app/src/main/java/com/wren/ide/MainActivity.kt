package com.wren.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.wren.ide.core.network.NetworkClient
import com.wren.ide.core.storage.SessionManager
import com.wren.ide.core.theme.WrenTheme
import com.wren.ide.features.auth.AuthScreen
import com.wren.ide.features.editor.IDEWorkspaceScreen
import com.wren.ide.features.ai.AIAgentScreen
import com.wren.ide.features.credits.CreditsScreen
import com.wren.ide.features.owner.OwnerAdminScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sessionManager = SessionManager(applicationContext)
        
        // Sync static client authentication headers on startup
        NetworkClient.setAuthToken(sessionManager.jwtToken)

        setContent {
            WrenTheme {
                var currentScreen by remember { 
                    mutableStateOf(if (sessionManager.isLoggedIn) "workspace" else "auth") 
                }

                when (currentScreen) {
                    "auth" -> {
                        AuthScreen(
                            sessionManager = sessionManager,
                            onAuthSuccess = { currentScreen = "workspace" }
                        )
                    }
                    "workspace" -> {
                        IDEWorkspaceScreen(
                            sessionManager = sessionManager,
                            onNavToAI = { currentScreen = "ai_agent" },
                            onNavToCredits = { currentScreen = "credits" },
                            onNavToOwner = { currentScreen = "owner_dashboard" },
                            onLogout = { currentScreen = "auth" }
                        )
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
