package com.example.game

import com.example.audio.AudioManager
import com.example.progression.PowerUpKind
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.sqrt

enum class ParticleBurstKind {
    DUST_PUFF,
    COIN_SPARKLE,
    HOVERBOARD_EXPLOSION,
    POWERUP_BURST
}

data class ParticleEvent(
    val kind: ParticleBurstKind,
    val x: Float,
    val y: Float,
    val z: Float,
    val r: Float = 1f,
    val g: Float = 0.85f,
    val b: Float = 0.1f
)

class GameManager(
    private val audioManager: AudioManager
) {
    val player = PlayerController()
    val trackManager = TrackManager()
    val powerUpManager = PowerUpManager()

    private val inputQueue = ConcurrentLinkedQueue<InputAction>()
    val particleEvents = ConcurrentLinkedQueue<ParticleEvent>()

    @Volatile
    var phase: GamePhase = GamePhase.MENU
        private set

    var speed: Float = BASE_SPEED
        private set
    var scoreFloat: Double = 0.0
        private set
    var runCoins: Int = 0
        private set
    var keysCollectedInRun: Int = 0
        private set
    var powerUpsUsedInRun: Int = 0
        private set
    var hoverboardsUsedInRun: Int = 0
        private set

    var permanentMultiplier: Int = 1
        private set
    var savedHighScore: Long = 0L
        private set
    var hoverboardsOwned: Int = 0
        private set
    var headstartsOwned: Int = 0
        private set
    var keysOwned: Int = 0
        private set

    var cameraShakeTimer: Float = 0f
        private set
    var cameraShakeIntensity: Float = 0f
        private set

    private var coinStreak: Int = 0
    private var coinStreakTimer: Float = 0f
    private var runElapsedSec: Float = 0f
    private var reviveUsedCount: Int = 0
    private var reviveCountdownSec: Float = 5f
    private var lastHudEmitTimeMs: Long = 0L
    private var isNewHighScoreFlag: Boolean = false
    private var missionTierLeveledFlag: Boolean = false

    // Callbacks wired by GameViewModel
    var onConsumeHoverboard: (() -> Boolean)? = null
    var onConsumeHeadstart: (() -> Boolean)? = null
    var onCommitRunResult: ((RunOutcomeReport) -> Unit)? = null
    var onHudSnapshot: ((HudSnapshot) -> Unit)? = null

    data class RunOutcomeReport(
        val score: Long,
        val coins: Int,
        val keysCollected: Int,
        val jumps: Int,
        val rolls: Int,
        val powerUpsUsed: Int,
        val hoverboardsUsed: Int,
        val rooftopMeters: Int,
        val laneSwitches: Int
    )

    companion object {
        const val BASE_SPEED = 14.5f
        const val MAX_SPEED = 33.0f
        const val HEADSTART_SPEED = 42.0f
        const val JETPACK_SPEED = 29.0f
    }

    init {
        trackManager.reset(false)
    }

    fun syncProgressionState(
        highScore: Long,
        permMultiplier: Int,
        hoverboards: Int,
        headstarts: Int,
        keys: Int,
        upgradeLevels: Map<PowerUpKind, Int>
    ) {
        savedHighScore = highScore
        permanentMultiplier = permMultiplier
        hoverboardsOwned = hoverboards
        headstartsOwned = headstarts
        keysOwned = keys
        powerUpManager.configureUpgradeLevels(upgradeLevels)
        if (phase == GamePhase.MENU) {
            emitSnapshot(force = true)
        }
    }

    fun startNewRun() {
        inputQueue.clear()
        particleEvents.clear()
        player.resetForNewRun()
        powerUpManager.reset()
        trackManager.reset(false)

        speed = BASE_SPEED
        scoreFloat = 0.0
        runCoins = 0
        keysCollectedInRun = 0
        powerUpsUsedInRun = 0
        hoverboardsUsedInRun = 0
        coinStreak = 0
        coinStreakTimer = 0f
        runElapsedSec = 0f
        reviveUsedCount = 0
        reviveCountdownSec = 5f
        cameraShakeTimer = 0f
        isNewHighScoreFlag = false
        missionTierLeveledFlag = false
        phase = GamePhase.PLAYING

        audioManager.startMusic()
        emitSnapshot(force = true)
    }

    fun pauseRun() {
        if (phase == GamePhase.PLAYING) {
            phase = GamePhase.PAUSED
            audioManager.pauseMusic()
            emitSnapshot(force = true)
        }
    }

    fun resumeRun() {
        if (phase == GamePhase.PAUSED) {
            phase = GamePhase.PLAYING
            audioManager.startMusic()
            emitSnapshot(force = true)
        }
    }

    fun returnToMenu() {
        phase = GamePhase.MENU
        audioManager.pauseMusic()
        player.resetForNewRun()
        powerUpManager.reset()
        trackManager.reset(false)
        emitSnapshot(force = true)
    }

    fun enqueueInput(action: InputAction) {
        if (phase == GamePhase.PLAYING) {
            inputQueue.offer(action)
        }
    }

    fun triggerHeadstartFromHud() {
        if (phase != GamePhase.PLAYING || runElapsedSec > 4.5f) return
        if (headstartsOwned <= 0 || powerUpManager.isHeadstartActive) return
        if (onConsumeHeadstart?.invoke() == true) {
            headstartsOwned = (headstartsOwned - 1).coerceAtLeast(0)
            powerUpManager.activateHeadstart()
            powerUpsUsedInRun++
            audioManager.playPowerUp()
            particleEvents.offer(
                ParticleEvent(
                    ParticleBurstKind.POWERUP_BURST,
                    player.worldX,
                    player.worldY + 1f,
                    player.worldZ,
                    1f, 0.45f, 0.05f
                )
            )
            emitSnapshot(force = true)
        }
    }

    fun activateHoverboard(): Boolean {
        if (phase != GamePhase.PLAYING || powerUpManager.isHoverboardActive) return false
        if (hoverboardsOwned <= 0) return false
        if (onConsumeHoverboard?.invoke() == true) {
            hoverboardsOwned = (hoverboardsOwned - 1).coerceAtLeast(0)
            powerUpManager.activateHoverboard()
            hoverboardsUsedInRun++
            audioManager.playHoverboardActivate()
            particleEvents.offer(
                ParticleEvent(
                    ParticleBurstKind.POWERUP_BURST,
                    player.worldX,
                    player.worldY + 0.5f,
                    player.worldZ,
                    0f, 0.72f, 1f
                )
            )
            emitSnapshot(force = true)
            return true
        }
        return false
    }

    fun acceptRevive() {
        if (phase != GamePhase.REVIVE_PROMPT) return
        reviveUsedCount++
        trackManager.clearObstaclesAhead(player.worldZ, 44f)
        player.grantInvulnerability(2.2f)
        phase = GamePhase.PLAYING
        audioManager.playHoverboardCrash()
        audioManager.startMusic()
        particleEvents.offer(
            ParticleEvent(
                ParticleBurstKind.HOVERBOARD_EXPLOSION,
                player.worldX,
                player.worldY + 1f,
                player.worldZ,
                0f, 0.9f, 0.5f
            )
        )
        emitSnapshot(force = true)
    }

    fun declineRevive() {
        if (phase != GamePhase.REVIVE_PROMPT) return
        finalizeGameOver()
    }

    fun markMissionTierLeveledUp() {
        missionTierLeveledFlag = true
        audioManager.playMissionComplete()
        emitSnapshot(force = true)
    }

    fun update(rawDt: Float) {
        val dt = rawDt.coerceIn(0f, 0.05f)

        if (cameraShakeTimer > 0f) {
            cameraShakeTimer = (cameraShakeTimer - dt).coerceAtLeast(0f)
        }

        if (phase == GamePhase.REVIVE_PROMPT) {
            reviveCountdownSec -= dt
            if (reviveCountdownSec <= 0f) {
                finalizeGameOver()
            } else {
                emitSnapshot(force = false)
            }
            return
        }

        if (phase != GamePhase.PLAYING) return

        runElapsedSec += dt
        if (coinStreakTimer > 0f) {
            coinStreakTimer -= dt
            if (coinStreakTimer <= 0f) coinStreak = 0
        }

        // Drain input queue
        while (true) {
            val action = inputQueue.poll() ?: break
            if (action == InputAction.DoubleTap) {
                activateHoverboard()
            } else {
                player.queueInput(action)
            }
        }

        // Speed progression over distance + power-up boosts
        val distanceSpeed = (BASE_SPEED + player.worldZ * 0.0085f).coerceAtMost(MAX_SPEED)
        speed = when {
            powerUpManager.isHeadstartActive -> HEADSTART_SPEED
            powerUpManager.isJetpackActive -> maxOf(distanceSpeed, JETPACK_SPEED)
            else -> distanceSpeed
        }

        // Update Power-Up timers
        powerUpManager.update(dt) {
            // Ensure clear landing track when Jetpack finishes
            trackManager.clearObstaclesAhead(player.worldZ, 36f)
        }

        // Evaluate ground/roof surface under player
        val (surfaceY, onRoof) = trackManager.evaluateSurfaceUnderPlayer(
            player.worldX,
            player.worldZ,
            player.worldY
        )

        // Update PlayerController
        val previousLane = player.update(
            dt = dt,
            forwardSpeed = speed,
            hasSuperSneakers = powerUpManager.isSuperSneakersActive,
            isJetpackActive = powerUpManager.isJetpackActive,
            groundSurfaceHeight = surfaceY,
            isOnRoofSurface = onRoof,
            onFootstep = {
                audioManager.playFootstep()
                if (!powerUpManager.isHoverboardActive) {
                    particleEvents.offer(
                        ParticleEvent(
                            ParticleBurstKind.DUST_PUFF,
                            player.worldX,
                            player.worldY + 0.08f,
                            player.worldZ - 0.25f,
                            0.85f, 0.80f, 0.72f
                        )
                    )
                }
            },
            onSwipeSound = { audioManager.playSwipe() },
            onJumpSound = { audioManager.playJump() },
            onRollSound = { audioManager.playRoll() }
        )

        // Advance track & object pools
        trackManager.update(
            dt = dt,
            playerZ = player.worldZ,
            isJetpackActive = powerUpManager.isJetpackActive,
            onTrainHorn = { audioManager.playTrainHorn() }
        )

        // Update score (Distance * Active Multiplier)
        val activeMult = permanentMultiplier * (if (powerUpManager.isDoubleMultiplierActive) 2 else 1)
        scoreFloat += (speed * dt) * activeMult * 1.45
        val currentScore = scoreFloat.toLong()
        if (currentScore > savedHighScore && savedHighScore > 0L) {
            isNewHighScoreFlag = true
        }

        // Collectibles & Magnet Attraction
        processCollectibles(dt)

        // Obstacle Collisions (skipped when high in Jetpack flight)
        if (!powerUpManager.isJetpackActive && player.worldY < 5.0f) {
            processCollisions(previousLane)
        }

        emitSnapshot(force = false)
    }

    private fun processCollectibles(dt: Float) {
        val px = player.worldX
        val py = player.worldY + player.currentHitboxHeight() * 0.5f
        val pz = player.worldZ
        val magnetActive = powerUpManager.isMagnetActive

        for (col in trackManager.collectibles) {
            if (!col.active) continue

            // Coin Magnet attraction across all 3 lanes
            if (magnetActive && col.kind == CollectibleKind.COIN) {
                val dz = col.z - pz
                if (dz in -2.5f..16.5f) {
                    val dx = px - col.x
                    val dy = py - col.y
                    val dzPull = pz - col.z
                    val dist = sqrt(dx * dx + dy * dy + dzPull * dzPull).coerceAtLeast(0.01f)
                    val pullStep = (34f * dt).coerceAtMost(dist)
                    col.x += (dx / dist) * pullStep
                    col.y += (dy / dist) * pullStep
                    col.z += (dzPull / dist) * pullStep
                }
            }

            // Pickup intersection
            if (abs(col.x - px) < 1.05f &&
                abs(col.z - pz) < 1.15f &&
                abs(col.y - py) < 1.35f
            ) {
                col.active = false
                when (col.kind) {
                    CollectibleKind.COIN -> {
                        runCoins++
                        audioManager.playCoin(coinStreak)
                        coinStreak = (coinStreak + 1).coerceAtMost(9)
                        coinStreakTimer = 0.65f
                        particleEvents.offer(
                            ParticleEvent(
                                ParticleBurstKind.COIN_SPARKLE,
                                col.x, col.y, col.z,
                                1.0f, 0.86f, 0.1f
                            )
                        )
                    }
                    CollectibleKind.JETPACK -> {
                        powerUpManager.activate(PowerUpKind.JETPACK)
                        powerUpsUsedInRun++
                        audioManager.playPowerUp()
                        particleEvents.offer(ParticleEvent(ParticleBurstKind.POWERUP_BURST, col.x, col.y, col.z, 1f, 0.45f, 0.1f))
                    }
                    CollectibleKind.SUPER_SNEAKERS -> {
                        powerUpManager.activate(PowerUpKind.SNEAKERS)
                        powerUpsUsedInRun++
                        audioManager.playPowerUp()
                        particleEvents.offer(ParticleEvent(ParticleBurstKind.POWERUP_BURST, col.x, col.y, col.z, 0.1f, 0.95f, 0.45f))
                    }
                    CollectibleKind.COIN_MAGNET -> {
                        powerUpManager.activate(PowerUpKind.MAGNET)
                        powerUpsUsedInRun++
                        audioManager.playPowerUp()
                        particleEvents.offer(ParticleEvent(ParticleBurstKind.POWERUP_BURST, col.x, col.y, col.z, 1f, 0.24f, 0.34f))
                    }
                    CollectibleKind.DOUBLE_MULTIPLIER -> {
                        powerUpManager.activate(PowerUpKind.MULTIPLIER)
                        powerUpsUsedInRun++
                        audioManager.playPowerUp()
                        particleEvents.offer(ParticleEvent(ParticleBurstKind.POWERUP_BURST, col.x, col.y, col.z, 1f, 0.85f, 0.05f))
                    }
                    CollectibleKind.HOVERBOARD_PICKUP -> {
                        hoverboardsOwned++
                        powerUpsUsedInRun++
                        audioManager.playPowerUp()
                    }
                    CollectibleKind.KEY_PICKUP -> {
                        keysCollectedInRun++
                        keysOwned++
                        audioManager.playMissionComplete()
                    }
                }
            }
        }
    }

    private fun processCollisions(previousLane: Lane) {
        if (player.invulnerableTimer > 0f) return

        val px = player.worldX
        val pyBottom = player.worldY
        val pyTop = player.worldY + player.currentHitboxHeight()
        val pzFront = player.worldZ + PlayerController.HALF_DEPTH
        val pzBack = player.worldZ - PlayerController.HALF_DEPTH

        for (o in trackManager.obstacles) {
            if (!o.active) continue
            if (o.type == ObstacleType.TUNNEL_ARCH ||
                o.type == ObstacleType.OVERHEAD_BRIDGE ||
                o.type == ObstacleType.SUPPORT_PILLAR
            ) continue

            // Check Z overlap
            if (pzFront < o.zStart || pzBack > o.zEnd) continue

            // Check X overlap
            val halfObstacleWidth = 0.92f
            val dx = abs(px - o.customX)
            if (dx > halfObstacleWidth + PlayerController.HALF_WIDTH) continue

            // Ramp handling: if on ramp portion of RAMP_TRAIN, running smoothly up!
            if (o.type == ObstacleType.RAMP_TRAIN && o.rampLength > 0f && player.worldZ <= o.zStart + o.rampLength) {
                val prog = ((player.worldZ - o.zStart) / o.rampLength).coerceIn(0f, 1f)
                val rampSurface = prog * o.topY
                if (pyBottom >= rampSurface - 0.8f) continue
            }

            // Walking cleanly on top of train roof
            if ((o.type == ObstacleType.STATIONARY_TRAIN ||
                    o.type == ObstacleType.RAMP_TRAIN ||
                    o.type == ObstacleType.MOVING_TRAIN) &&
                pyBottom >= o.topY - 0.55f
            ) {
                continue
            }

            // Check vertical overlap against obstacle bottomY..topY
            if (pyTop <= o.bottomY + 0.08f || pyBottom >= o.topY - 0.08f) {
                continue
            }

            // Determine whether this is a side-clip stumble or a direct front crash
            val isSideClip = (previousLane != player.targetLane && dx > 0.65f) ||
                (dx > 0.85f && (o.type == ObstacleType.LOW_HURDLE || o.type == ObstacleType.FULL_BLOCKER))

            if (isSideClip) {
                if (previousLane != player.targetLane) {
                    player.pushBackToPreviousLane(previousLane)
                }
                val fatalSecondStumble = player.triggerStumble {
                    cameraShakeTimer = 0.28f
                    cameraShakeIntensity = 0.32f
                    audioManager.playStumble()
                }
                if (fatalSecondStumble) {
                    handleFatalImpact()
                }
            } else {
                handleFatalImpact()
            }
            return
        }
    }

    private fun handleFatalImpact() {
        // Hoverboard shield absorbs crash, explodes, clears nearby obstacles!
        if (powerUpManager.consumeHoverboardOnCrash()) {
            trackManager.clearObstaclesAhead(player.worldZ, 22f)
            player.grantInvulnerability(1.8f)
            cameraShakeTimer = 0.38f
            cameraShakeIntensity = 0.55f
            audioManager.playHoverboardCrash()
            particleEvents.offer(
                ParticleEvent(
                    ParticleBurstKind.HOVERBOARD_EXPLOSION,
                    player.worldX,
                    player.worldY + 0.9f,
                    player.worldZ,
                    0f, 0.85f, 1f
                )
            )
            emitSnapshot(force = true)
            return
        }

        // Crash!
        cameraShakeTimer = 0.45f
        cameraShakeIntensity = 0.65f
        audioManager.playCrash()
        audioManager.pauseMusic()

        val reviveCost = 1 shl reviveUsedCount.coerceAtMost(4) // 1, 2, 4...
        if (keysOwned >= reviveCost) {
            reviveCountdownSec = 5.0f
            phase = GamePhase.REVIVE_PROMPT
            emitSnapshot(force = true)
        } else {
            finalizeGameOver()
        }
    }

    private fun finalizeGameOver() {
        phase = GamePhase.GAME_OVER
        val finalScore = scoreFloat.toLong()
        if (finalScore > savedHighScore) {
            isNewHighScoreFlag = true
        }
        onCommitRunResult?.invoke(
            RunOutcomeReport(
                score = finalScore,
                coins = runCoins,
                keysCollected = keysCollectedInRun,
                jumps = player.totalJumps,
                rolls = player.totalRolls,
                powerUpsUsed = powerUpsUsedInRun,
                hoverboardsUsed = hoverboardsUsedInRun,
                rooftopMeters = player.rooftopMetersAccum.toInt(),
                laneSwitches = player.totalLaneSwitches
            )
        )
        emitSnapshot(force = true)
    }

    private fun emitSnapshot(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastHudEmitTimeMs < 50L) return // ~20 Hz UI updates while GL runs at 60 FPS
        lastHudEmitTimeMs = now

        val activeMult = permanentMultiplier * (if (powerUpManager.isDoubleMultiplierActive) 2 else 1)
        val reviveCost = 1 shl reviveUsedCount.coerceAtMost(4)
        onHudSnapshot?.invoke(
            HudSnapshot(
                phase = phase,
                score = scoreFloat.toLong(),
                runCoins = runCoins,
                keysCollectedInRun = keysCollectedInRun,
                activeMultiplier = activeMult,
                permanentMultiplier = permanentMultiplier,
                isDoubleMultiplierActive = powerUpManager.isDoubleMultiplierActive,
                speedMetersPerSec = speed,
                distanceMeters = player.worldZ.toInt(),
                chaserTimerSec = player.chaserTimer,
                isChaserVisible = player.chaserTimer > 0f,
                isHoverboardActive = powerUpManager.isHoverboardActive,
                isJetpackActive = powerUpManager.isJetpackActive,
                canTriggerHeadstart = phase == GamePhase.PLAYING && runElapsedSec < 4.5f && headstartsOwned > 0 && !powerUpManager.isHeadstartActive,
                headstartsOwned = headstartsOwned,
                hoverboardsOwned = hoverboardsOwned,
                reviveCostKeys = reviveCost,
                reviveCountdownSec = reviveCountdownSec,
                activeTimers = powerUpManager.buildHudTimers(),
                isNewHighScore = isNewHighScoreFlag,
                missionTierLeveledUp = missionTierLeveledFlag,
                runJumps = player.totalJumps,
                runRolls = player.totalRolls,
                runPowerUps = powerUpsUsedInRun,
                runHoverboards = hoverboardsUsedInRun,
                runRooftopMeters = player.rooftopMetersAccum.toInt(),
                runLaneSwitches = player.totalLaneSwitches
            )
        )
    }
}
