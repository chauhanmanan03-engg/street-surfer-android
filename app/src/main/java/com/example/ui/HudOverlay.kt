package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.ActivePowerUpTimer
import com.example.game.HudSnapshot
import com.example.ui.theme.StreetCharcoal
import com.example.ui.theme.StreetCoral
import com.example.ui.theme.StreetCyan
import com.example.ui.theme.StreetMint
import com.example.ui.theme.StreetOffWhite
import com.example.ui.theme.StreetOrange
import com.example.ui.theme.StreetSlate
import com.example.ui.theme.StreetYellow
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil

@Composable
fun InGameHudOverlay(
    hud: HudSnapshot,
    soundEnabled: Boolean,
    onPause: () -> Unit,
    onToggleSound: () -> Unit,
    onTriggerHeadstart: () -> Unit,
    onTriggerHoverboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = NumberFormat.getIntegerInstance(Locale.US)

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Bar: Pause + Mute (Left) | Score + Multiplier + Coins (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = StreetSlate.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, StreetOffWhite.copy(alpha = 0.2f))
                ) {
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = stringResource(R.string.pause),
                            tint = StreetOffWhite
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = StreetSlate.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, StreetOffWhite.copy(alpha = 0.2f))
                ) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("hud_mute_button")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (soundEnabled) "Mute Audio" else "Unmute Audio",
                            tint = StreetOffWhite
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Multiplier badge
                    val multBg = if (hud.isDoubleMultiplierActive) StreetYellow else StreetCyan
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = multBg,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "x${hud.activeMultiplier}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 16.sp),
                            color = StreetCharcoal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Score pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StreetSlate.copy(alpha = 0.88f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, StreetYellow.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = formatter.format(hud.score).padStart(6, '0'),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 20.sp),
                            color = StreetOffWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                // Coin counter pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StreetSlate.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(StreetYellow)
                                .border(2.dp, Color(0xFFFFA000), CircleShape)
                        )
                        Text(
                            text = formatter.format(hud.runCoins),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                            color = StreetYellow
                        )
                    }
                }
            }
        }

        // Chaser Warning Pill (Top Center)
        AnimatedVisibility(
            visible = hud.isChaserVisible && hud.chaserTimerSec > 1.2f && hud.chaserTimerSec < 7.8f,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 62.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = StreetCoral.copy(alpha = 0.92f),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = StreetOffWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.chaser_warning),
                        style = MaterialTheme.typography.labelMedium,
                        color = StreetOffWhite
                    )
                }
            }
        }

        // Bottom-Left: Radial Countdown Timers for Active Power-Ups
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            hud.activeTimers.forEach { timer ->
                RadialPowerUpBadge(timer = timer)
            }
        }

        // Bottom-Right: Headstart Quick Trigger + Hoverboard Shortcut
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(visible = hud.canTriggerHeadstart) {
                Button(
                    onClick = onTriggerHeadstart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StreetOrange,
                        contentColor = StreetOffWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("headstart_button")
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${stringResource(R.string.headstart)} (${hud.headstartsOwned})",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (hud.hoverboardsOwned > 0 && !hud.isHoverboardActive) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StreetSlate.copy(alpha = 0.88f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, StreetCyan),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTriggerHoverboard() }
                        .testTag("hoverboard_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Activate Hoverboard",
                            tint = StreetCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "BOARD x${hud.hoverboardsOwned}",
                            style = MaterialTheme.typography.labelMedium,
                            color = StreetOffWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadialPowerUpBadge(timer: ActivePowerUpTimer) {
    val progress = (timer.remainingSec / timer.totalSec.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
    val ringColor = Color(timer.colorHex)
    val secDisplay = ceil(timer.remainingSec.toDouble()).toInt()

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = StreetSlate.copy(alpha = 0.86f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ringColor.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(38.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 4.dp.toPx()
                    drawArc(
                        color = ringColor.copy(alpha = 0.22f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${secDisplay}s",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                    color = StreetOffWhite,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = timer.label,
                style = MaterialTheme.typography.labelMedium,
                color = ringColor
            )
        }
    }
}
