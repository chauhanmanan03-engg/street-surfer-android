package com.example.game

enum class GamePhase {
    MENU,
    PLAYING,
    PAUSED,
    REVIVE_PROMPT,
    GAME_OVER
}

enum class Lane(val index: Int) {
    LEFT(-1),
    CENTER(0),
    RIGHT(1);

    // Camera looks along +Z with up=+Y, so screen-left is +X and screen-right is -X
    fun worldX(): Float = -index * LANE_WIDTH

    companion object {
        const val LANE_WIDTH = 2.7f

        fun fromIndex(idx: Int): Lane = when (idx.coerceIn(-1, 1)) {
            -1 -> LEFT
            0 -> CENTER
            else -> RIGHT
        }
    }
}

enum class ObstacleType {
    LOW_HURDLE,        // Must jump over
    HIGH_BARRIER,      // Must roll/slide under (or Super Sneaker jump over)
    FULL_BLOCKER,      // Tall wall, must switch lanes
    STATIONARY_TRAIN,  // Parked boxcar; roof is walkable
    RAMP_TRAIN,        // Parked train with front ramp to run onto roof
    MOVING_TRAIN,      // Approaching train with headlights & horn
    TUNNEL_ARCH,       // Environmental tunnel architecture spanning tracks
    OVERHEAD_BRIDGE,   // Overhead pedestrian bridge
    SUPPORT_PILLAR     // Narrow pillar placed between tracks (visual/gap framing)
}

enum class CollectibleKind {
    COIN,
    JETPACK,
    SUPER_SNEAKERS,
    COIN_MAGNET,
    DOUBLE_MULTIPLIER,
    HOVERBOARD_PICKUP,
    KEY_PICKUP
}

sealed interface InputAction {
    data object SwipeLeft : InputAction
    data object SwipeRight : InputAction
    data object SwipeUp : InputAction
    data object SwipeDown : InputAction
    data object DoubleTap : InputAction
}

data class ActivePowerUpTimer(
    val label: String,
    val remainingSec: Float,
    val totalSec: Float,
    val colorHex: Long
)

data class HudSnapshot(
    val phase: GamePhase = GamePhase.MENU,
    val score: Long = 0L,
    val runCoins: Int = 0,
    val keysCollectedInRun: Int = 0,
    val activeMultiplier: Int = 1,
    val permanentMultiplier: Int = 1,
    val isDoubleMultiplierActive: Boolean = false,
    val speedMetersPerSec: Float = 14f,
    val distanceMeters: Int = 0,
    val chaserTimerSec: Float = 0f,
    val isChaserVisible: Boolean = false,
    val isHoverboardActive: Boolean = false,
    val isJetpackActive: Boolean = false,
    val canTriggerHeadstart: Boolean = false,
    val headstartsOwned: Int = 0,
    val hoverboardsOwned: Int = 0,
    val reviveCostKeys: Int = 1,
    val reviveCountdownSec: Float = 5f,
    val activeTimers: List<ActivePowerUpTimer> = emptyList(),
    val isNewHighScore: Boolean = false,
    val missionTierLeveledUp: Boolean = false,
    val runJumps: Int = 0,
    val runRolls: Int = 0,
    val runPowerUps: Int = 0,
    val runHoverboards: Int = 0,
    val runRooftopMeters: Int = 0,
    val runLaneSwitches: Int = 0
)
