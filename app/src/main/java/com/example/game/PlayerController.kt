package com.example.game

import kotlin.math.abs

class PlayerController {
    var targetLane: Lane = Lane.CENTER
        private set
    var worldX: Float = 0f
        private set
    var worldY: Float = 0f
        private set
    var worldZ: Float = 0f

    var velocityY: Float = 0f
        private set
    var isGrounded: Boolean = true
        private set
    var isRolling: Boolean = false
        private set
    var rollTimer: Float = 0f
        private set
    var currentGroundY: Float = 0f
        private set
    var onTrainRoof: Boolean = false
        private set

    var runAnimTime: Float = 0f
        private set
    var laneSwitchTilt: Float = 0f
        private set
    var stumbleAnimTimer: Float = 0f
        private set

    // Chaser & Stumble system
    var chaserTimer: Float = 0f
        private set
    var chaserZOffset: Float = -2.4f
        private set
    var invulnerableTimer: Float = 0f
        private set

    // Input buffering (~180ms)
    private var bufferedVerticalAction: InputAction? = null
    private var bufferedVerticalTimer: Float = 0f
    private var bufferedLaneAction: InputAction? = null
    private var bufferedLaneTimer: Float = 0f

    // Run Telemetry for Missions
    var totalJumps: Int = 0
        private set
    var totalRolls: Int = 0
        private set
    var totalLaneSwitches: Int = 0
        private set
    var rooftopMetersAccum: Float = 0f
        private set

    private var footstepTimer: Float = 0f

    companion object {
        const val NORMAL_HEIGHT = 1.75f
        const val ROLL_HEIGHT = 0.875f // 50% collision hitbox reduction
        const val HALF_WIDTH = 0.42f
        const val HALF_DEPTH = 0.35f
        const val BASE_JUMP_VELOCITY = 14.6f
        const val SUPER_SNEAKERS_JUMP_VELOCITY = 20.6f // ~2x peak height
        const val GRAVITY = -42.0f
        const val FAST_FALL_VELOCITY = -38.0f
        const val ROLL_DURATION = 0.68f
        const val INPUT_BUFFER_WINDOW = 0.19f
        const val CHASER_DURATION = 8.0f
    }

    fun resetForNewRun() {
        targetLane = Lane.CENTER
        worldX = 0f
        worldY = 0f
        worldZ = 0f
        velocityY = 0f
        isGrounded = true
        isRolling = false
        rollTimer = 0f
        currentGroundY = 0f
        onTrainRoof = false
        runAnimTime = 0f
        laneSwitchTilt = 0f
        stumbleAnimTimer = 0f
        chaserTimer = 3.5f // Guard & dog appear at start of run before dropping back
        chaserZOffset = -2.6f
        invulnerableTimer = 0f
        bufferedVerticalAction = null
        bufferedVerticalTimer = 0f
        bufferedLaneAction = null
        bufferedLaneTimer = 0f
        totalJumps = 0
        totalRolls = 0
        totalLaneSwitches = 0
        rooftopMetersAccum = 0f
        footstepTimer = 0f
    }

    fun currentHitboxHeight(): Float = if (isRolling) ROLL_HEIGHT else NORMAL_HEIGHT

    fun queueInput(action: InputAction) {
        when (action) {
            InputAction.SwipeLeft, InputAction.SwipeRight -> {
                bufferedLaneAction = action
                bufferedLaneTimer = INPUT_BUFFER_WINDOW
            }
            InputAction.SwipeUp, InputAction.SwipeDown -> {
                bufferedVerticalAction = action
                bufferedVerticalTimer = INPUT_BUFFER_WINDOW
            }
            InputAction.DoubleTap -> Unit
        }
    }

    fun triggerStumble(onStumble: () -> Unit): Boolean {
        if (invulnerableTimer > 0f) return false
        return if (chaserTimer > 0.1f && chaserTimer < CHASER_DURATION - 0.5f) {
            // Chaser was already right behind player -> second hit causes Crash!
            true
        } else {
            chaserTimer = CHASER_DURATION
            chaserZOffset = -2.2f
            stumbleAnimTimer = 0.45f
            invulnerableTimer = 0.85f
            onStumble()
            false
        }
    }

    fun grantInvulnerability(seconds: Float) {
        invulnerableTimer = maxOf(invulnerableTimer, seconds)
    }

    fun pushBackToPreviousLane(previousLane: Lane) {
        targetLane = previousLane
    }

    fun update(
        dt: Float,
        forwardSpeed: Float,
        hasSuperSneakers: Boolean,
        isJetpackActive: Boolean,
        groundSurfaceHeight: Float,
        isOnRoofSurface: Boolean,
        onFootstep: () -> Unit,
        onSwipeSound: () -> Unit,
        onJumpSound: () -> Unit,
        onRollSound: () -> Unit
    ): Lane {
        val previousLane = targetLane
        currentGroundY = groundSurfaceHeight
        onTrainRoof = isOnRoofSurface

        if (invulnerableTimer > 0f) invulnerableTimer = (invulnerableTimer - dt).coerceAtLeast(0f)
        if (stumbleAnimTimer > 0f) stumbleAnimTimer = (stumbleAnimTimer - dt).coerceAtLeast(0f)

        // Update chaser countdown (falls back off-screen after 8 clean seconds)
        if (chaserTimer > 0f) {
            chaserTimer = (chaserTimer - dt).coerceAtLeast(0f)
            val targetOffset = if (chaserTimer > 1.5f) -2.4f else -10.5f
            chaserZOffset += (targetOffset - chaserZOffset) * (dt * 4.5f).coerceAtMost(1f)
        } else {
            chaserZOffset += (-14.0f - chaserZOffset) * (dt * 3f).coerceAtMost(1f)
        }

        // Advance forward
        val deltaZ = forwardSpeed * dt
        worldZ += deltaZ
        runAnimTime += dt * (forwardSpeed / 12.0f)

        if (isOnRoofSurface && isGrounded && !isJetpackActive) {
            rooftopMetersAccum += deltaZ
        }

        // Process lane buffer
        if (bufferedLaneTimer > 0f) {
            bufferedLaneTimer -= dt
            val laneAct = bufferedLaneAction
            val closeEnoughToTarget = abs(worldX - targetLane.worldX()) < 1.35f
            if (laneAct != null && closeEnoughToTarget) {
                val nextIdx = when (laneAct) {
                    InputAction.SwipeLeft -> (targetLane.index - 1).coerceAtLeast(-1)
                    InputAction.SwipeRight -> (targetLane.index + 1).coerceAtMost(1)
                    else -> targetLane.index
                }
                if (nextIdx != targetLane.index) {
                    targetLane = Lane.fromIndex(nextIdx)
                    totalLaneSwitches++
                    laneSwitchTilt = if (laneAct == InputAction.SwipeLeft) 1f else -1f
                    onSwipeSound()
                }
                bufferedLaneAction = null
                bufferedLaneTimer = 0f
            }
        }

        // Smooth snappy lateral lerp
        val targetX = targetLane.worldX()
        val lerpAlpha = (dt * 15.5f).coerceAtMost(1f)
        worldX += (targetX - worldX) * lerpAlpha
        laneSwitchTilt *= (1f - (dt * 9f).coerceAtMost(1f))

        // Jetpack flight overrides vertical gravity
        if (isJetpackActive) {
            val targetFlightY = 6.2f
            worldY += (targetFlightY - worldY) * (dt * 6f).coerceAtMost(1f)
            velocityY = 0f
            isGrounded = false
            isRolling = false
            rollTimer = 0f
            bufferedVerticalAction = null
            bufferedVerticalTimer = 0f
            return previousLane
        }

        // Process vertical buffer (Jump & Roll / Fast-fall)
        if (bufferedVerticalTimer > 0f) {
            bufferedVerticalTimer -= dt
            when (bufferedVerticalAction) {
                InputAction.SwipeUp -> {
                    if (isGrounded) {
                        isRolling = false
                        rollTimer = 0f
                        isGrounded = false
                        velocityY = if (hasSuperSneakers) SUPER_SNEAKERS_JUMP_VELOCITY else BASE_JUMP_VELOCITY
                        totalJumps++
                        onJumpSound()
                        bufferedVerticalAction = null
                        bufferedVerticalTimer = 0f
                    }
                }
                InputAction.SwipeDown -> {
                    if (!isGrounded) {
                        // Fast-fall slam straight into roll!
                        velocityY = minOf(velocityY, FAST_FALL_VELOCITY)
                        // Keep SwipeDown buffered until ground impact to auto-roll immediately
                    } else if (!isRolling) {
                        isRolling = true
                        rollTimer = ROLL_DURATION
                        totalRolls++
                        onRollSound()
                        bufferedVerticalAction = null
                        bufferedVerticalTimer = 0f
                    } else {
                        rollTimer = ROLL_DURATION
                        bufferedVerticalAction = null
                        bufferedVerticalTimer = 0f
                    }
                }
                else -> Unit
            }
        }

        // Roll timer
        if (isRolling) {
            rollTimer -= dt
            if (rollTimer <= 0f) {
                isRolling = false
                rollTimer = 0f
            }
        }

        // Gravity & vertical integration
        if (!isGrounded || worldY > currentGroundY + 0.01f) {
            velocityY += GRAVITY * dt
            worldY += velocityY * dt
            if (worldY <= currentGroundY) {
                worldY = currentGroundY
                velocityY = 0f
                isGrounded = true
                // Execute buffered fast-fall roll or buffered jump immediately on landing
                if (bufferedVerticalAction == InputAction.SwipeDown && bufferedVerticalTimer > 0f) {
                    isRolling = true
                    rollTimer = ROLL_DURATION
                    totalRolls++
                    onRollSound()
                    bufferedVerticalAction = null
                    bufferedVerticalTimer = 0f
                } else if (bufferedVerticalAction == InputAction.SwipeUp && bufferedVerticalTimer > 0f) {
                    isGrounded = false
                    velocityY = if (hasSuperSneakers) SUPER_SNEAKERS_JUMP_VELOCITY else BASE_JUMP_VELOCITY
                    totalJumps++
                    onJumpSound()
                    bufferedVerticalAction = null
                    bufferedVerticalTimer = 0f
                }
            }
        } else {
            worldY = currentGroundY
            velocityY = 0f
            isGrounded = true
            // Footstep cadence
            if (!isRolling) {
                footstepTimer += dt * (forwardSpeed / 14f)
                if (footstepTimer >= 0.28f) {
                    footstepTimer = 0f
                    onFootstep()
                }
            }
        }

        return previousLane
    }
}
