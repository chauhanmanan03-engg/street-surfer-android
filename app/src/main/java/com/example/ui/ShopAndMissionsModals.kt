package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progression.PowerUpKind
import com.example.progression.ProgressionManager
import com.example.progression.ProgressionSnapshot
import com.example.progression.StreetCatalog
import com.example.ui.theme.StreetCharcoal
import com.example.ui.theme.StreetCyan
import com.example.ui.theme.StreetMint
import com.example.ui.theme.StreetMutedText
import com.example.ui.theme.StreetOffWhite
import com.example.ui.theme.StreetOrange
import com.example.ui.theme.StreetSlate
import com.example.ui.theme.StreetSlateSurface
import com.example.ui.theme.StreetYellow
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MissionsModalSheet(
    progression: ProgressionSnapshot?,
    onClose: () -> Unit
) {
    val missions = progression?.missions ?: emptyList()
    val permMult = progression?.save?.permanentMultiplier ?: 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreetCharcoal.copy(alpha = 0.80f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StreetSlate,
            border = BorderStroke(2.dp, StreetMint.copy(alpha = 0.8f)),
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("MISSIONS", style = MaterialTheme.typography.headlineLarge, color = StreetMint)
                        Text(
                            text = "Multiplier x$permMult / x${ProgressionManager.MAX_PERMANENT_MULTIPLIER}",
                            style = MaterialTheme.typography.labelMedium,
                            color = StreetYellow
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("close_missions_button")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Missions", tint = StreetOffWhite)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StreetSlateSurface
                ) {
                    Text(
                        text = "Complete all 3 active missions to permanently raise your Score Multiplier by +1x and earn +1 Bonus Key!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StreetMutedText,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                missions.forEach { m ->
                    val frac = (m.currentProgress.toFloat() / m.targetValue.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StreetSlateSurface,
                        border = BorderStroke(1.dp, if (m.isCompleted) StreetMint else StreetOffWhite.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = m.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StreetOffWhite,
                                    modifier = Modifier.weight(1f)
                                )
                                if (m.isCompleted) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Completed", tint = StreetMint)
                                } else {
                                    Text(
                                        text = "${m.currentProgress} / ${m.targetValue}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                                        color = StreetYellow
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { frac },
                                color = if (m.isCompleted) StreetMint else StreetCyan,
                                trackColor = StreetCharcoal,
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreetShopModalSheet(
    progression: ProgressionSnapshot?,
    selectedTab: ShopTab,
    onSelectTab: (ShopTab) -> Unit,
    onBuyUpgrade: (PowerUpKind) -> Unit,
    onBuyHoverboard: () -> Unit,
    onBuyHeadstart: () -> Unit,
    onBuyKey: () -> Unit,
    onSelectCharacter: (String) -> Unit,
    onSelectBoard: (String) -> Unit,
    onClose: () -> Unit
) {
    val save = progression?.save
    val coins = save?.coins ?: 0
    val keys = save?.keys ?: 0
    val upgrades = progression?.upgradeLevels ?: emptyMap()
    val unlocked = progression?.unlockedIds ?: emptySet()
    val formatter = NumberFormat.getIntegerInstance(Locale.US)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreetCharcoal.copy(alpha = 0.85f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StreetSlate,
            border = BorderStroke(2.dp, StreetCyan.copy(alpha = 0.75f)),
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("STREET SHOP", style = MaterialTheme.typography.headlineLarge, color = StreetCyan)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WalletPill(coins = coins, keys = keys)
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("close_shop_button")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close Shop", tint = StreetOffWhite)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = StreetSlateSurface,
                    contentColor = StreetYellow,
                    edgePadding = 4.dp,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    ShopTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { onSelectTab(tab) },
                            text = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selectedTab == tab) StreetYellow else StreetMutedText
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        ShopTab.UPGRADES -> {
                            items(PowerUpKind.entries) { kind ->
                                val level = upgrades[kind] ?: 1
                                val cost = kind.upgradeCost(level)
                                val isMax = level >= 6
                                val canAfford = !isMax && coins >= cost
                                UpgradeShopCard(
                                    kind = kind,
                                    level = level,
                                    cost = cost,
                                    canAfford = canAfford,
                                    formatter = formatter,
                                    onBuy = { onBuyUpgrade(kind) }
                                )
                            }
                        }

                        ShopTab.GEAR -> {
                            item {
                                ConsumableShopCard(
                                    title = "Hoverboard",
                                    subtitle = "30s crash shield — blasts nearby hazards on impact",
                                    ownedCount = save?.hoverboardsOwned ?: 0,
                                    cost = ProgressionManager.HOVERBOARD_COST,
                                    canAfford = coins >= ProgressionManager.HOVERBOARD_COST,
                                    accentColor = StreetCyan,
                                    icon = { Icon(Icons.Filled.Shield, contentDescription = null, tint = StreetCyan) },
                                    onBuy = onBuyHoverboard
                                )
                            }
                            item {
                                ConsumableShopCard(
                                    title = "Headstart",
                                    subtitle = "Trigger at run start for high-speed aerial coin blast",
                                    ownedCount = save?.headstartsOwned ?: 0,
                                    cost = ProgressionManager.HEADSTART_COST,
                                    canAfford = coins >= ProgressionManager.HEADSTART_COST,
                                    accentColor = StreetOrange,
                                    icon = { Icon(Icons.Filled.RocketLaunch, contentDescription = null, tint = StreetOrange) },
                                    onBuy = onBuyHeadstart
                                )
                            }
                            item {
                                ConsumableShopCard(
                                    title = "Revive Key",
                                    subtitle = "Save yourself after a crash to keep high-score runs going",
                                    ownedCount = keys,
                                    cost = ProgressionManager.KEY_COST,
                                    canAfford = coins >= ProgressionManager.KEY_COST,
                                    accentColor = StreetYellow,
                                    icon = { Icon(Icons.Filled.Key, contentDescription = null, tint = StreetYellow) },
                                    onBuy = onBuyKey
                                )
                            }
                        }

                        ShopTab.CHARACTERS -> {
                            items(StreetCatalog.characters) { skin ->
                                val isUnlocked = skin.costCoins == 0 || skin.id in unlocked
                                val isEquipped = save?.equippedCharacterId == skin.id
                                CosmeticShopCard(
                                    name = skin.name,
                                    subtitle = skin.subtitle,
                                    primaryColor = Color(skin.hoodieRgb[0], skin.hoodieRgb[1], skin.hoodieRgb[2]),
                                    secondaryColor = Color(skin.accentRgb[0], skin.accentRgb[1], skin.accentRgb[2]),
                                    cost = skin.costCoins,
                                    isUnlocked = isUnlocked,
                                    isEquipped = isEquipped,
                                    canAfford = coins >= skin.costCoins,
                                    formatter = formatter,
                                    onAction = { onSelectCharacter(skin.id) }
                                )
                            }
                        }

                        ShopTab.BOARDS -> {
                            items(StreetCatalog.hoverboards) { board ->
                                val isUnlocked = board.costCoins == 0 || board.id in unlocked
                                val isEquipped = save?.equippedBoardId == board.id
                                CosmeticShopCard(
                                    name = board.name,
                                    subtitle = board.subtitle,
                                    primaryColor = Color(board.deckRgb[0], board.deckRgb[1], board.deckRgb[2]),
                                    secondaryColor = Color(board.glowRgb[0], board.glowRgb[1], board.glowRgb[2]),
                                    cost = board.costCoins,
                                    isUnlocked = isUnlocked,
                                    isEquipped = isEquipped,
                                    canAfford = coins >= board.costCoins,
                                    formatter = formatter,
                                    onAction = { onSelectBoard(board.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpgradeShopCard(
    kind: PowerUpKind,
    level: Int,
    cost: Int,
    canAfford: Boolean,
    formatter: NumberFormat,
    onBuy: () -> Unit
) {
    val isMax = level >= 6
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StreetSlateSurface
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(kind.displayName, style = MaterialTheme.typography.titleLarge, color = StreetOffWhite)
                    Text(
                        text = "${kind.description} (${kind.durationForLevel(level)}s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StreetMutedText
                    )
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onBuy,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMax) StreetMint else StreetYellow,
                        contentColor = StreetCharcoal,
                        disabledContainerColor = StreetCharcoal.copy(alpha = 0.6f),
                        disabledContentColor = StreetMutedText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = if (isMax) "MAX" else formatter.format(cost),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)
                    )
                }
            }

            // Level 1..6 pips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                for (i in 1..6) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (i <= level) StreetMint else StreetCharcoal)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsumableShopCard(
    title: String,
    subtitle: String,
    ownedCount: Int,
    cost: Int,
    canAfford: Boolean,
    accentColor: Color,
    icon: @Composable () -> Unit,
    onBuy: () -> Unit
) {
    val formatter = NumberFormat.getIntegerInstance(Locale.US)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StreetSlateSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = StreetOffWhite)
                    Text("Owned: $ownedCount", style = MaterialTheme.typography.labelMedium, color = accentColor)
                }
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = StreetMutedText)
            }
            Button(
                onClick = onBuy,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StreetYellow,
                    contentColor = StreetCharcoal,
                    disabledContainerColor = StreetCharcoal.copy(alpha = 0.6f),
                    disabledContentColor = StreetMutedText
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(formatter.format(cost), style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
            }
        }
    }
}

@Composable
private fun CosmeticShopCard(
    name: String,
    subtitle: String,
    primaryColor: Color,
    secondaryColor: Color,
    cost: Int,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    canAfford: Boolean,
    formatter: NumberFormat,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StreetSlateSurface,
        border = if (isEquipped) BorderStroke(1.5.dp, StreetMint) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(primaryColor)
                    .border(3.dp, secondaryColor, RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge, color = StreetOffWhite)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = StreetMutedText)
            }
            Button(
                onClick = onAction,
                enabled = isUnlocked || canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isEquipped -> StreetMint
                        isUnlocked -> StreetCyan
                        else -> StreetYellow
                    },
                    contentColor = StreetCharcoal,
                    disabledContainerColor = StreetCharcoal.copy(alpha = 0.6f),
                    disabledContentColor = StreetMutedText
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(
                    text = when {
                        isEquipped -> "EQUIPPED"
                        isUnlocked -> "EQUIP"
                        else -> formatter.format(cost)
                    },
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
