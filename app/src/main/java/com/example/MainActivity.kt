package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.GamePhase
import com.example.ui.GameOverOverlay
import com.example.ui.GameSurfaceView
import com.example.ui.GameViewModel
import com.example.ui.InGameHudOverlay
import com.example.ui.MainMenuOverlay
import com.example.ui.MissionsModalSheet
import com.example.ui.PauseOverlay
import com.example.ui.RevivePromptOverlay
import com.example.ui.StreetShopModalSheet
import com.example.ui.theme.StreetSurferTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreetSurferTheme {
                val vm: GameViewModel = viewModel()
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                val hud = uiState.hud
                val soundEnabled = uiState.progression?.save?.soundEnabled ?: true

                // Back button handling for modals, active run pause, or menu
                BackHandler(
                    enabled = uiState.isMissionsOpen || uiState.isShopOpen || hud.phase == GamePhase.PLAYING || hud.phase == GamePhase.PAUSED
                ) {
                    when {
                        uiState.isMissionsOpen -> vm.openMissions(false)
                        uiState.isShopOpen -> vm.openShop(false)
                        hud.phase == GamePhase.PLAYING -> vm.pauseRun()
                        hud.phase == GamePhase.PAUSED -> vm.resumeRun()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Full-bleed OpenGL ES 3.0 3D World
                    GameSurfaceView(
                        renderer = vm.renderer,
                        phase = hud.phase,
                        onInput = vm::sendInput,
                        onTogglePause = {
                            if (hud.phase == GamePhase.PLAYING) vm.pauseRun()
                            else if (hud.phase == GamePhase.PAUSED) vm.resumeRun()
                        }
                    )

                    // State-driven Compose HUD & Overlays with smooth transitions
                    androidx.compose.animation.AnimatedContent(
                        targetState = hud.phase,
                        transitionSpec = {
                            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260)) togetherWith
                                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))
                        },
                        label = "phaseOverlay"
                    ) { targetPhase ->
                        when (targetPhase) {
                            GamePhase.MENU -> {
                                MainMenuOverlay(
                                    progression = uiState.progression,
                                    onPlay = vm::startRun,
                                    onOpenMissions = { vm.openMissions(true) },
                                    onOpenShop = { vm.openShop(true) },
                                    onToggleSound = vm::toggleSound
                                )
                            }

                            GamePhase.PLAYING -> {
                                InGameHudOverlay(
                                    hud = hud,
                                    soundEnabled = soundEnabled,
                                    onPause = vm::pauseRun,
                                    onToggleSound = vm::toggleSound,
                                    onTriggerHeadstart = vm::triggerHeadstart,
                                    onTriggerHoverboard = vm::triggerHoverboard
                                )
                            }

                            GamePhase.PAUSED -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    InGameHudOverlay(
                                        hud = hud,
                                        soundEnabled = soundEnabled,
                                        onPause = vm::resumeRun,
                                        onToggleSound = vm::toggleSound,
                                        onTriggerHeadstart = {},
                                        onTriggerHoverboard = {}
                                    )
                                    PauseOverlay(
                                        soundEnabled = soundEnabled,
                                        onResume = vm::resumeRun,
                                        onRestart = vm::startRun,
                                        onToggleSound = vm::toggleSound,
                                        onMainMenu = vm::returnToMenu
                                    )
                                }
                            }

                            GamePhase.REVIVE_PROMPT -> {
                                RevivePromptOverlay(
                                    hud = hud,
                                    onAcceptRevive = vm::acceptRevive,
                                    onDeclineRevive = vm::declineRevive
                                )
                            }

                            GamePhase.GAME_OVER -> {
                                GameOverOverlay(
                                    hud = hud,
                                    progression = uiState.progression,
                                    onPlayAgain = vm::startRun,
                                    onOpenShop = { vm.openShop(true) },
                                    onMainMenu = vm::returnToMenu
                                )
                            }
                        }
                    }

                    // Missions & Shop Modals
                    if (uiState.isMissionsOpen) {
                        MissionsModalSheet(
                            progression = uiState.progression,
                            onClose = { vm.openMissions(false) }
                        )
                    }

                    if (uiState.isShopOpen) {
                        StreetShopModalSheet(
                            progression = uiState.progression,
                            selectedTab = uiState.selectedShopTab,
                            onSelectTab = vm::selectShopTab,
                            onBuyUpgrade = vm::buyUpgrade,
                            onBuyHoverboard = vm::buyHoverboard,
                            onBuyHeadstart = vm::buyHeadstart,
                            onBuyKey = vm::buyKey,
                            onSelectCharacter = vm::selectCharacter,
                            onSelectBoard = vm::selectBoard,
                            onClose = { vm.openShop(false) }
                        )
                    }
                }
            }
        }
    }
}
