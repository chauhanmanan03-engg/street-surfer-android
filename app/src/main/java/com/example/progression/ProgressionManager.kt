package com.example.progression

import com.example.data.MissionEntity
import com.example.data.PlayerSaveEntity
import com.example.data.PowerUpUpgradeEntity
import com.example.data.StreetSurferDao
import com.example.data.UnlockableItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class PowerUpKind(
    val id: String,
    val displayName: String,
    val description: String,
    val baseDurationSec: Float,
    val bonusPerLevelSec: Float
) {
    JETPACK("JETPACK", "Jetpack", "Fly high above all obstacles & rain coins", 10f, 1.5f),
    SNEAKERS("SNEAKERS", "Super Sneakers", "Double jump height onto train roofs", 12f, 2.0f),
    MAGNET("MAGNET", "Coin Magnet", "Pulls coins from all 3 lanes to you", 12f, 2.0f),
    MULTIPLIER("MULTIPLIER", "2x Multiplier", "Doubles your active score multiplier", 12f, 2.0f);

    fun durationForLevel(level: Int): Float {
        val clamped = level.coerceIn(1, 6)
        return baseDurationSec + (clamped - 1) * bonusPerLevelSec
    }

    fun upgradeCost(currentLevel: Int): Int = when (currentLevel) {
        1 -> 500
        2 -> 1200
        3 -> 2500
        4 -> 4500
        5 -> 8000
        else -> 0
    }
}

enum class MissionMetric {
    COINS_IN_RUN,
    JUMPS_TOTAL,
    ROLLS_TOTAL,
    POWERUPS_TOTAL,
    SCORE_IN_RUN,
    HOVERBOARDS_TOTAL,
    ROOFTOP_METERS_TOTAL,
    LANE_SWITCHES_TOTAL
}

data class MissionTemplate(
    val key: String,
    val title: String,
    val metric: MissionMetric,
    val target: Int,
    val singleRun: Boolean
)

data class CharacterSkin(
    val id: String,
    val name: String,
    val subtitle: String,
    val costCoins: Int,
    val hoodieRgb: FloatArray,
    val accentRgb: FloatArray,
    val pantsRgb: FloatArray,
    val shoeRgb: FloatArray
)

data class HoverboardStyle(
    val id: String,
    val name: String,
    val subtitle: String,
    val costCoins: Int,
    val deckRgb: FloatArray,
    val glowRgb: FloatArray
)

data class ProgressionSnapshot(
    val save: PlayerSaveEntity,
    val upgradeLevels: Map<PowerUpKind, Int>,
    val missions: List<MissionEntity>,
    val unlockedIds: Set<String>
)

data class RunStatsPayload(
    val runScore: Long,
    val runCoins: Int,
    val jumps: Int,
    val rolls: Int,
    val powerUpsUsed: Int,
    val hoverboardsUsed: Int,
    val rooftopMeters: Int,
    val laneSwitches: Int
)

object StreetCatalog {
    val characters = listOf(
        CharacterSkin(
            id = "char_blaze",
            name = "Jake Blaze",
            subtitle = "Cyan Denim & Coral Cap",
            costCoins = 0,
            hoodieRgb = floatArrayOf(0.05f, 0.70f, 1.0f),
            accentRgb = floatArrayOf(1.0f, 0.24f, 0.34f),
            pantsRgb = floatArrayOf(0.16f, 0.22f, 0.36f),
            shoeRgb = floatArrayOf(1.0f, 0.84f, 0.0f)
        ),
        CharacterSkin(
            id = "char_vibe",
            name = "Tricky Vibe",
            subtitle = "Electric Yellow & Violet",
            costCoins = 1200,
            hoodieRgb = floatArrayOf(1.0f, 0.84f, 0.0f),
            accentRgb = floatArrayOf(0.67f, 0.0f, 1.0f),
            pantsRgb = floatArrayOf(0.15f, 0.14f, 0.24f),
            shoeRgb = floatArrayOf(0.0f, 0.90f, 0.46f)
        ),
        CharacterSkin(
            id = "char_beats",
            name = "Fresh Beats",
            subtitle = "Neon Mint & Crimson Varsity",
            costCoins = 2500,
            hoodieRgb = floatArrayOf(0.0f, 0.90f, 0.46f),
            accentRgb = floatArrayOf(1.0f, 0.24f, 0.34f),
            pantsRgb = floatArrayOf(0.12f, 0.16f, 0.22f),
            shoeRgb = floatArrayOf(0.0f, 0.69f, 1.0f)
        ),
        CharacterSkin(
            id = "char_king",
            name = "King Graffiti",
            subtitle = "Royal Gold & Obsidian",
            costCoins = 5000,
            hoodieRgb = floatArrayOf(0.98f, 0.68f, 0.05f),
            accentRgb = floatArrayOf(0.0f, 0.90f, 1.0f),
            pantsRgb = floatArrayOf(0.10f, 0.10f, 0.14f),
            shoeRgb = floatArrayOf(1.0f, 0.24f, 0.34f)
        )
    )

    val hoverboards = listOf(
        HoverboardStyle(
            id = "board_sunburst",
            name = "Sunburst Cruiser",
            subtitle = "Classic Yellow & Cyan Glow",
            costCoins = 0,
            deckRgb = floatArrayOf(1.0f, 0.84f, 0.0f),
            glowRgb = floatArrayOf(0.0f, 0.72f, 1.0f)
        ),
        HoverboardStyle(
            id = "board_turbocyan",
            name = "Skyline Pulse",
            subtitle = "Hyper Cyan Neon Wake",
            costCoins = 800,
            deckRgb = floatArrayOf(0.0f, 0.72f, 1.0f),
            glowRgb = floatArrayOf(0.45f, 0.94f, 1.0f)
        ),
        HoverboardStyle(
            id = "board_magma",
            name = "Magma Drift",
            subtitle = "Coral Blaze Trail",
            costCoins = 1600,
            deckRgb = floatArrayOf(1.0f, 0.24f, 0.34f),
            glowRgb = floatArrayOf(1.0f, 0.48f, 0.05f)
        ),
        HoverboardStyle(
            id = "board_mint",
            name = "Mint Phantom",
            subtitle = "Emerald Spark Trail",
            costCoins = 2400,
            deckRgb = floatArrayOf(0.0f, 0.90f, 0.46f),
            glowRgb = floatArrayOf(0.65f, 1.0f, 0.80f)
        ),
        HoverboardStyle(
            id = "board_gold",
            name = "Gold Rush",
            subtitle = "Prismatic Gold Trail",
            costCoins = 4000,
            deckRgb = floatArrayOf(1.0f, 0.78f, 0.12f),
            glowRgb = floatArrayOf(1.0f, 0.96f, 0.55f)
        )
    )

    private val missionSets: List<List<MissionTemplate>> = listOf(
        listOf(
            MissionTemplate("m0_coins", "Collect 80 coins in one run", MissionMetric.COINS_IN_RUN, 80, true),
            MissionTemplate("m0_jumps", "Jump 20 times", MissionMetric.JUMPS_TOTAL, 20, false),
            MissionTemplate("m0_power", "Use 2 power-ups", MissionMetric.POWERUPS_TOTAL, 2, false)
        ),
        listOf(
            MissionTemplate("m1_rolls", "Roll under barriers 12 times", MissionMetric.ROLLS_TOTAL, 12, false),
            MissionTemplate("m1_score", "Score 3,500 points in one run", MissionMetric.SCORE_IN_RUN, 3500, true),
            MissionTemplate("m1_lanes", "Switch lanes 40 times", MissionMetric.LANE_SWITCHES_TOTAL, 40, false)
        ),
        listOf(
            MissionTemplate("m2_coins", "Collect 180 coins in one run", MissionMetric.COINS_IN_RUN, 180, true),
            MissionTemplate("m2_board", "Activate a Hoverboard 2 times", MissionMetric.HOVERBOARDS_TOTAL, 2, false),
            MissionTemplate("m2_roof", "Run 80m across train roofs", MissionMetric.ROOFTOP_METERS_TOTAL, 80, false)
        ),
        listOf(
            MissionTemplate("m3_score", "Score 10,000 points in one run", MissionMetric.SCORE_IN_RUN, 10000, true),
            MissionTemplate("m3_power", "Pick up 5 power-ups", MissionMetric.POWERUPS_TOTAL, 5, false),
            MissionTemplate("m3_jumps", "Jump 50 times", MissionMetric.JUMPS_TOTAL, 50, false)
        )
    )

    fun missionSetForIndex(index: Int): List<MissionEntity> {
        val tierBoost = 1 + (index / missionSets.size)
        val templateSet = missionSets[index % missionSets.size]
        return templateSet.mapIndexed { slot, template ->
            val scaledTarget = if (tierBoost > 1) (template.target * (1 + (tierBoost - 1) * 0.4f)).toInt() else template.target
            val scaledTitle = if (tierBoost > 1) {
                template.title.replace(template.target.toString(), scaledTarget.toString())
            } else {
                template.title
            }
            MissionEntity(
                slotIndex = slot,
                missionKey = "${template.key}_$index",
                title = scaledTitle,
                metricType = template.metric.name,
                targetValue = scaledTarget,
                currentProgress = 0,
                isCompleted = false,
                isSingleRun = template.singleRun
            )
        }
    }

    fun findCharacter(id: String): CharacterSkin =
        characters.firstOrNull { it.id == id } ?: characters.first()

    fun findBoard(id: String): HoverboardStyle =
        hoverboards.firstOrNull { it.id == id } ?: hoverboards.first()
}

class ProgressionManager(private val dao: StreetSurferDao) {
    private val mutex = Mutex()

    companion object {
        const val HOVERBOARD_COST = 300
        const val HEADSTART_COST = 1000
        const val KEY_COST = 1500
        const val MAX_PERMANENT_MULTIPLIER = 30
    }

    val progressionFlow: Flow<ProgressionSnapshot> = combine(
        dao.observePlayerSave(),
        dao.observeUpgrades(),
        dao.observeMissions(),
        dao.observeUnlockables()
    ) { save, upgrades, missions, unlockables ->
        val safeSave = save ?: PlayerSaveEntity()
        val upgradeMap = PowerUpKind.entries.associateWith { kind ->
            upgrades.firstOrNull { it.powerUpId == kind.id }?.level ?: 1
        }
        val safeMissions = missions.ifEmpty { StreetCatalog.missionSetForIndex(safeSave.missionSetIndex) }
        val unlocked = unlockables.filter { it.isUnlocked }.map { it.itemId }.toSet() +
            setOf("char_blaze", "board_sunburst")
        ProgressionSnapshot(
            save = safeSave,
            upgradeLevels = upgradeMap,
            missions = safeMissions,
            unlockedIds = unlocked
        )
    }

    suspend fun ensureInitialized() = mutex.withLock {
        if (dao.getPlayerSave() == null) {
            dao.upsertPlayerSave(PlayerSaveEntity())
        }
        if (dao.getUpgrades().isEmpty()) {
            dao.upsertUpgrades(PowerUpKind.entries.map { PowerUpUpgradeEntity(it.id, 1) })
        }
        if (dao.getMissions().isEmpty()) {
            dao.upsertMissions(StreetCatalog.missionSetForIndex(0))
        }
        if (dao.getUnlockables().isEmpty()) {
            val defaults = StreetCatalog.characters.map {
                UnlockableItemEntity(it.id, "CHARACTER", it.costCoins == 0)
            } + StreetCatalog.hoverboards.map {
                UnlockableItemEntity(it.id, "BOARD", it.costCoins == 0)
            }
            dao.upsertUnlockables(defaults)
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) = mutex.withLock {
        val current = dao.getPlayerSave() ?: PlayerSaveEntity()
        dao.upsertPlayerSave(current.copy(soundEnabled = enabled))
    }

    suspend fun consumeHoverboard(): Boolean = mutex.withLock {
        val current = dao.getPlayerSave() ?: return@withLock false
        if (current.hoverboardsOwned <= 0) return@withLock false
        dao.upsertPlayerSave(current.copy(hoverboardsOwned = current.hoverboardsOwned - 1))
        true
    }

    suspend fun consumeHeadstart(): Boolean = mutex.withLock {
        val current = dao.getPlayerSave() ?: return@withLock false
        if (current.headstartsOwned <= 0) return@withLock false
        dao.upsertPlayerSave(current.copy(headstartsOwned = current.headstartsOwned - 1))
        true
    }

    suspend fun consumeReviveKeys(cost: Int): Boolean = mutex.withLock {
        val current = dao.getPlayerSave() ?: return@withLock false
        if (current.keys < cost) return@withLock false
        dao.upsertPlayerSave(current.copy(keys = current.keys - cost))
        true
    }

    suspend fun addBonusKey(amount: Int = 1) = mutex.withLock {
        val current = dao.getPlayerSave() ?: PlayerSaveEntity()
        dao.upsertPlayerSave(current.copy(keys = current.keys + amount))
    }

    suspend fun buyUpgrade(kind: PowerUpKind): Boolean = mutex.withLock {
        val save = dao.getPlayerSave() ?: return@withLock false
        val upgrades = dao.getUpgrades()
        val currentLevel = upgrades.firstOrNull { it.powerUpId == kind.id }?.level ?: 1
        if (currentLevel >= 6) return@withLock false
        val cost = kind.upgradeCost(currentLevel)
        if (save.coins < cost) return@withLock false
        dao.upsertPlayerSave(save.copy(coins = save.coins - cost))
        dao.upsertUpgrade(PowerUpUpgradeEntity(kind.id, currentLevel + 1))
        true
    }

    suspend fun buyHoverboard(): Boolean = mutex.withLock {
        val save = dao.getPlayerSave() ?: return@withLock false
        if (save.coins < HOVERBOARD_COST) return@withLock false
        dao.upsertPlayerSave(
            save.copy(
                coins = save.coins - HOVERBOARD_COST,
                hoverboardsOwned = save.hoverboardsOwned + 1
            )
        )
        true
    }

    suspend fun buyHeadstart(): Boolean = mutex.withLock {
        val save = dao.getPlayerSave() ?: return@withLock false
        if (save.coins < HEADSTART_COST) return@withLock false
        dao.upsertPlayerSave(
            save.copy(
                coins = save.coins - HEADSTART_COST,
                headstartsOwned = save.headstartsOwned + 1
            )
        )
        true
    }

    suspend fun buyKey(): Boolean = mutex.withLock {
        val save = dao.getPlayerSave() ?: return@withLock false
        if (save.coins < KEY_COST) return@withLock false
        dao.upsertPlayerSave(
            save.copy(
                coins = save.coins - KEY_COST,
                keys = save.keys + 1
            )
        )
        true
    }

    suspend fun unlockOrEquipCharacter(id: String): Boolean = mutex.withLock {
        val skin = StreetCatalog.findCharacter(id)
        val save = dao.getPlayerSave() ?: return@withLock false
        val unlockables = dao.getUnlockables()
        val isUnlocked = skin.costCoins == 0 || unlockables.any { it.itemId == id && it.isUnlocked }
        if (isUnlocked) {
            dao.upsertPlayerSave(save.copy(equippedCharacterId = id))
            return@withLock true
        }
        if (save.coins < skin.costCoins) return@withLock false
        dao.upsertPlayerSave(save.copy(coins = save.coins - skin.costCoins, equippedCharacterId = id))
        dao.upsertUnlockable(UnlockableItemEntity(id, "CHARACTER", true))
        true
    }

    suspend fun unlockOrEquipBoard(id: String): Boolean = mutex.withLock {
        val board = StreetCatalog.findBoard(id)
        val save = dao.getPlayerSave() ?: return@withLock false
        val unlockables = dao.getUnlockables()
        val isUnlocked = board.costCoins == 0 || unlockables.any { it.itemId == id && it.isUnlocked }
        if (isUnlocked) {
            dao.upsertPlayerSave(save.copy(equippedBoardId = id))
            return@withLock true
        }
        if (save.coins < board.costCoins) return@withLock false
        dao.upsertPlayerSave(save.copy(coins = save.coins - board.costCoins, equippedBoardId = id))
        dao.upsertUnlockable(UnlockableItemEntity(id, "BOARD", true))
        true
    }

    /**
     * Commits run coins, high score, and updates active missions.
     * Returns true if all 3 active missions finished and leveled up the permanent multiplier!
     */
    suspend fun recordRunOutcome(stats: RunStatsPayload, keysCollectedInRun: Int): Boolean = mutex.withLock {
        var save = dao.getPlayerSave() ?: PlayerSaveEntity()
        val newHigh = maxOf(save.highScore, stats.runScore)
        save = save.copy(
            coins = save.coins + stats.runCoins,
            keys = save.keys + keysCollectedInRun,
            highScore = newHigh
        )

        val currentMissions = dao.getMissions().ifEmpty { StreetCatalog.missionSetForIndex(save.missionSetIndex) }
        val updatedMissions = currentMissions.map { m ->
            if (m.isCompleted) {
                m
            } else {
                val delta = when (MissionMetric.valueOf(m.metricType)) {
                    MissionMetric.COINS_IN_RUN -> stats.runCoins
                    MissionMetric.JUMPS_TOTAL -> stats.jumps
                    MissionMetric.ROLLS_TOTAL -> stats.rolls
                    MissionMetric.POWERUPS_TOTAL -> stats.powerUpsUsed
                    MissionMetric.SCORE_IN_RUN -> stats.runScore.toInt()
                    MissionMetric.HOVERBOARDS_TOTAL -> stats.hoverboardsUsed
                    MissionMetric.ROOFTOP_METERS_TOTAL -> stats.rooftopMeters
                    MissionMetric.LANE_SWITCHES_TOTAL -> stats.laneSwitches
                }
                val newProg = if (m.isSingleRun) {
                    maxOf(m.currentProgress, delta)
                } else {
                    m.currentProgress + delta
                }
                val done = newProg >= m.targetValue
                m.copy(
                    currentProgress = newProg.coerceAtMost(m.targetValue),
                    isCompleted = done
                )
            }
        }

        val allCompleted = updatedMissions.all { it.isCompleted }
        if (allCompleted) {
            val nextSetIndex = save.missionSetIndex + 1
            val nextMultiplier = (save.permanentMultiplier + 1).coerceAtMost(MAX_PERMANENT_MULTIPLIER)
            save = save.copy(
                permanentMultiplier = nextMultiplier,
                keys = save.keys + 1,
                missionSetIndex = nextSetIndex
            )
            dao.upsertPlayerSave(save)
            dao.upsertMissions(StreetCatalog.missionSetForIndex(nextSetIndex))
            true
        } else {
            dao.upsertPlayerSave(save)
            dao.upsertMissions(updatedMissions)
            false
        }
    }
}
