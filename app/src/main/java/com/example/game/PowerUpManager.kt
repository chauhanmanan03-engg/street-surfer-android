package com.example.game

import com.example.progression.PowerUpKind

class PowerUpManager {
    var jetpackRemaining: Float = 0f
        private set
    var jetpackTotal: Float = 10f
        private set

    var sneakersRemaining: Float = 0f
        private set
    var sneakersTotal: Float = 12f
        private set

    var magnetRemaining: Float = 0f
        private set
    var magnetTotal: Float = 12f
        private set

    var multiplierRemaining: Float = 0f
        private set
    var multiplierTotal: Float = 12f
        private set

    var hoverboardRemaining: Float = 0f
        private set
    val hoverboardTotal: Float = 30f

    var headstartRemaining: Float = 0f
        private set
    val headstartTotal: Float = 4.5f

    private var upgradeLevels: Map<PowerUpKind, Int> = emptyMap()

    val isJetpackActive: Boolean get() = jetpackRemaining > 0f || headstartRemaining > 0f
    val isSuperSneakersActive: Boolean get() = sneakersRemaining > 0f
    val isMagnetActive: Boolean get() = magnetRemaining > 0f
    val isDoubleMultiplierActive: Boolean get() = multiplierRemaining > 0f
    val isHoverboardActive: Boolean get() = hoverboardRemaining > 0f
    val isHeadstartActive: Boolean get() = headstartRemaining > 0f

    fun configureUpgradeLevels(levels: Map<PowerUpKind, Int>) {
        upgradeLevels = levels
    }

    fun reset() {
        jetpackRemaining = 0f
        sneakersRemaining = 0f
        magnetRemaining = 0f
        multiplierRemaining = 0f
        hoverboardRemaining = 0f
        headstartRemaining = 0f
    }

    fun activate(kind: PowerUpKind) {
        val level = upgradeLevels[kind] ?: 1
        val duration = kind.durationForLevel(level)
        when (kind) {
            PowerUpKind.JETPACK -> {
                jetpackTotal = duration
                jetpackRemaining = duration
            }
            PowerUpKind.SNEAKERS -> {
                sneakersTotal = duration
                sneakersRemaining = duration
            }
            PowerUpKind.MAGNET -> {
                magnetTotal = duration
                magnetRemaining = duration
            }
            PowerUpKind.MULTIPLIER -> {
                multiplierTotal = duration
                multiplierRemaining = duration
            }
        }
    }

    fun activateHoverboard(): Boolean {
        if (hoverboardRemaining > 0f) return false
        hoverboardRemaining = hoverboardTotal
        return true
    }

    fun activateHeadstart(): Boolean {
        if (headstartRemaining > 0f) return false
        headstartRemaining = headstartTotal
        return true
    }

    fun consumeHoverboardOnCrash(): Boolean {
        if (hoverboardRemaining <= 0f) return false
        hoverboardRemaining = 0f
        return true
    }

    fun update(dt: Float, onJetpackEnded: () -> Unit) {
        val wasJetpacking = isJetpackActive
        if (jetpackRemaining > 0f) jetpackRemaining = (jetpackRemaining - dt).coerceAtLeast(0f)
        if (headstartRemaining > 0f) headstartRemaining = (headstartRemaining - dt).coerceAtLeast(0f)
        if (wasJetpacking && !isJetpackActive) {
            onJetpackEnded()
        }
        if (sneakersRemaining > 0f) sneakersRemaining = (sneakersRemaining - dt).coerceAtLeast(0f)
        if (magnetRemaining > 0f) magnetRemaining = (magnetRemaining - dt).coerceAtLeast(0f)
        if (multiplierRemaining > 0f) multiplierRemaining = (multiplierRemaining - dt).coerceAtLeast(0f)
        if (hoverboardRemaining > 0f) hoverboardRemaining = (hoverboardRemaining - dt).coerceAtLeast(0f)
    }

    fun buildHudTimers(): List<ActivePowerUpTimer> {
        val list = ArrayList<ActivePowerUpTimer>(5)
        if (isJetpackActive) {
            val rem = maxOf(jetpackRemaining, headstartRemaining)
            val tot = if (headstartRemaining > jetpackRemaining) headstartTotal else jetpackTotal
            list.add(ActivePowerUpTimer("JET", rem, tot, 0xFFFF6D00))
        }
        if (isHoverboardActive) {
            list.add(ActivePowerUpTimer("BOARD", hoverboardRemaining, hoverboardTotal, 0xFF00B0FF))
        }
        if (isSuperSneakersActive) {
            list.add(ActivePowerUpTimer("JUMP", sneakersRemaining, sneakersTotal, 0xFF00E676))
        }
        if (isMagnetActive) {
            list.add(ActivePowerUpTimer("MAG", magnetRemaining, magnetTotal, 0xFFFF3D57))
        }
        if (isDoubleMultiplierActive) {
            list.add(ActivePowerUpTimer("2X", multiplierRemaining, multiplierTotal, 0xFFFFD600))
        }
        return list
    }
}
