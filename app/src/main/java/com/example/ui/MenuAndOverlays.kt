package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.HudSnapshot
import com.example.progression.ProgressionSnapshot
import com.example.ui.theme.StreetCharcoal
import com.example.ui.theme.StreetCoral
import com.example.ui.theme.StreetCyan
import com.example.ui.theme.StreetMint
import com.example.ui.theme.StreetMutedText
import com.example.ui.theme.StreetOffWhite
import com.example.ui.theme.StreetSlate
import com.example.ui.theme.StreetSlateSurface
import com.example.ui.theme.StreetYellow
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil

@Composable
fun MainMenuOverlay(
    progression: ProgressionSnapshot?,
    onPlay: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenShop: () -> Unit,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val save = progression?.save
    val coins = save?.coins ?: 500
    val keys = save?.keys ?: 3
    val highScore = save?.highScore ?: 0L
    val multiplier = save?.permanentMultiplier ?: 1
    val soundEnabled = save?.soundEnabled ?: true
    val completedMissions = progression?.missions?.count { it.isCompleted } ?: 0
    val formatter = NumberFormat.getIntegerInstance(Locale.US)

    val entryAlpha = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    val entrySlide = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(36f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        entryAlpha.animateTo(1f, animationSpec = tween(480))
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        entrySlide.animateTo(
            0f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            )
        )
    }

    val pulseTransition = rememberInfiniteTransition(label = "playPulse")
    val playScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(950),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .graphicsLayer {
                alpha = entryAlpha.value
            }
    ) {
        // Top Header: High Score & Multiplier (Left) | Wallet & Mute (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = -entrySlide.value * 0.5f },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StreetSlate.copy(alpha = 0.80f),
                border = BorderStroke(1.5.dp, StreetYellow.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = StreetYellow, modifier = Modifier.size(20.dp))
                    Column {
                        Text(
                            text = "BEST ${formatter.format(highScore)}",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                            color = StreetOffWhite
                        )
                        Text(
                            text = "MULTIPLIER x$multiplier",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            color = StreetCyan
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WalletPill(coins = coins, keys = keys)
                Surface(
                    shape = CircleShape,
                    color = StreetSlate.copy(alpha = 0.80f),
                    border = BorderStroke(1.5.dp, StreetOffWhite.copy(alpha = 0.22f))
                ) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("menu_mute_button")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Toggle Sound",
                            tint = StreetOffWhite
                        )
                    }
                }
            }
        }

        // Graffiti Title Banner
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 74.dp)
                .graphicsLayer {
                    translationY = -entrySlide.value * 0.35f
                    scaleX = 0.94f + 0.06f * entryAlpha.value
                    scaleY = 0.94f + 0.06f * entryAlpha.value
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "STREET",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 46.sp),
                color = StreetYellow
            )
            Text(
                text = "SURFER",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                color = StreetOffWhite
            )
        }

        // Bottom Controls & Play CTA
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
                .graphicsLayer { translationY = entrySlide.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StreetYellow,
                    contentColor = StreetCharcoal
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .scale(playScale)
                    .testTag("play_button")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tap_to_play),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onOpenMissions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StreetSlate.copy(alpha = 0.90f),
                        contentColor = StreetOffWhite
                    ),
                    border = BorderStroke(1.5.dp, StreetMint),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("missions_button")
                ) {
                    Icon(Icons.Filled.TaskAlt, contentDescription = null, tint = StreetMint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${stringResource(R.string.missions)} ($completedMissions/3)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Button(
                    onClick = onOpenShop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StreetSlate.copy(alpha = 0.90f),
                        contentColor = StreetOffWhite
                    ),
                    border = BorderStroke(1.5.dp, StreetCyan),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("shop_button")
                ) {
                    Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = StreetCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.shop),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StreetCharcoal.copy(alpha = 0.72f)
            ) {
                Text(
                    text = "Swipe / WASD / Arrows to Run • Double-Tap / Space = Hoverboard",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = StreetMutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun WalletPill(coins: Int, keys: Int, modifier: Modifier = Modifier) {
    val formatter = NumberFormat.getIntegerInstance(Locale.US)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = StreetSlate.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, StreetOffWhite.copy(alpha = 0.16f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(StreetYellow)
                        .border(2.dp, Color(0xFFFFA000), CircleShape)
                )
                Text(
                    text = formatter.format(coins),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    color = StreetYellow
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(Icons.Filled.Key, contentDescription = "Keys", tint = StreetCyan, modifier = Modifier.size(15.dp))
                Text(
                    text = keys.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    color = StreetCyan
                )
            }
        }
    }
}

@Composable
fun PauseOverlay(
    soundEnabled: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleSound: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreetCharcoal.copy(alpha = 0.78f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StreetSlate,
            border = BorderStroke(2.dp, StreetCyan.copy(alpha = 0.6f)),
            modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "PAUSED",
                    style = MaterialTheme.typography.headlineLarge,
                    color = StreetYellow
                )

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = StreetYellow, contentColor = StreetCharcoal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("resume_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.resume), style = MaterialTheme.typography.titleLarge)
                }

                OutlinedButton(
                    onClick = onRestart,
                    border = BorderStroke(1.5.dp, StreetCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("restart_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = StreetCyan)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.restart), color = StreetOffWhite, style = MaterialTheme.typography.titleMedium)
                }

                OutlinedButton(
                    onClick = onToggleSound,
                    border = BorderStroke(1.5.dp, StreetOffWhite.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = null,
                        tint = StreetOffWhite
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (soundEnabled) "Audio: ON" else "Audio: OFF",
                        color = StreetOffWhite,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                OutlinedButton(
                    onClick = onMainMenu,
                    border = BorderStroke(1.5.dp, StreetCoral),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("menu_button")
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null, tint = StreetCoral)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.main_menu), color = StreetCoral, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun RevivePromptOverlay(
    hud: HudSnapshot,
    onAcceptRevive: () -> Unit,
    onDeclineRevive: () -> Unit
) {
    val sec = ceil(hud.reviveCountdownSec.toDouble()).toInt().coerceAtLeast(1)
    val prog = (hud.reviveCountdownSec / 5f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreetCharcoal.copy(alpha = 0.80f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StreetSlate,
            border = BorderStroke(2.dp, StreetCoral),
            modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.save_me),
                    style = MaterialTheme.typography.headlineLarge,
                    color = StreetCoral
                )
                Text(
                    text = "Keep your run alive & blast nearby obstacles!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StreetMutedText,
                    textAlign = TextAlign.Center
                )
                LinearProgressIndicator(
                    progress = { prog },
                    color = StreetYellow,
                    trackColor = StreetSlateSurface,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
                Text(
                    text = "${sec}s",
                    style = MaterialTheme.typography.labelLarge,
                    color = StreetYellow
                )

                Button(
                    onClick = onAcceptRevive,
                    colors = ButtonDefaults.buttonColors(containerColor = StreetMint, contentColor = StreetCharcoal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("revive_button")
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "REVIVE (${hud.reviveCostKeys} KEY${if (hud.reviveCostKeys > 1) "S" else ""})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                OutlinedButton(
                    onClick = onDeclineRevive,
                    border = BorderStroke(1.dp, StreetMutedText.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("skip_revive_button")
                ) {
                    Text("No Thanks", color = StreetMutedText, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    hud: HudSnapshot,
    progression: ProgressionSnapshot?,
    onPlayAgain: () -> Unit,
    onOpenShop: () -> Unit,
    onMainMenu: () -> Unit
) {
    val formatter = NumberFormat.getIntegerInstance(Locale.US)
    val highScore = progression?.save?.highScore ?: hud.score
    val missions = progression?.missions ?: emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        StreetCharcoal.copy(alpha = 0.78f),
                        StreetCharcoal.copy(alpha = 0.94f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StreetSlate,
            border = BorderStroke(2.dp, StreetYellow.copy(alpha = 0.75f)),
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "RUN COMPLETE",
                    style = MaterialTheme.typography.headlineLarge,
                    color = StreetOffWhite
                )

                if (hud.isNewHighScore) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StreetYellow
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = StreetCharcoal, modifier = Modifier.size(16.dp))
                            Text(
                                text = stringResource(R.string.new_high_score),
                                style = MaterialTheme.typography.labelMedium,
                                color = StreetCharcoal
                            )
                        }
                    }
                }

                if (hud.missionTierLeveledUp) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StreetMint
                    ) {
                        Text(
                            text = "MISSIONS COMPLETE! MULTIPLIER UP TO x${progression?.save?.permanentMultiplier ?: hud.permanentMultiplier} + 1 KEY!",
                            style = MaterialTheme.typography.labelMedium,
                            color = StreetCharcoal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Score & Coins Summary Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StreetSlateSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SCORE", style = MaterialTheme.typography.labelMedium, color = StreetMutedText)
                            Text(
                                text = formatter.format(hud.score),
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp),
                                color = StreetYellow
                            )
                            Text(
                                text = "BEST ${formatter.format(highScore)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                color = StreetCyan
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("COINS", style = MaterialTheme.typography.labelMedium, color = StreetMutedText)
                            Text(
                                text = "+${formatter.format(hud.runCoins)}",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp),
                                color = StreetMint
                            )
                            Text(
                                text = "${hud.distanceMeters}m • x${hud.activeMultiplier}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                color = StreetMutedText
                            )
                        }
                    }
                }

                // Active Missions Summary
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("ACTIVE MISSIONS", style = MaterialTheme.typography.labelMedium, color = StreetMutedText)
                    missions.forEach { m ->
                        val frac = (m.currentProgress.toFloat() / m.targetValue.coerceAtLeast(1)).coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = m.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (m.isCompleted) StreetMint else StreetOffWhite
                                )
                                Text(
                                    text = "${m.currentProgress}/${m.targetValue}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (m.isCompleted) StreetMint else StreetYellow
                                )
                            }
                            LinearProgressIndicator(
                                progress = { frac },
                                color = if (m.isCompleted) StreetMint else StreetCyan,
                                trackColor = StreetSlateSurface,
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = onPlayAgain,
                    colors = ButtonDefaults.buttonColors(containerColor = StreetYellow, contentColor = StreetCharcoal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("play_again_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.play_again), style = MaterialTheme.typography.headlineSmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenShop,
                        border = BorderStroke(1.5.dp, StreetCyan),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = StreetCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.shop), color = StreetOffWhite)
                    }
                    OutlinedButton(
                        onClick = onMainMenu,
                        border = BorderStroke(1.5.dp, StreetOffWhite.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = null, tint = StreetOffWhite, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Menu", color = StreetOffWhite)
                    }
                }
            }
        }
    }
}
