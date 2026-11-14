package com.example

import com.example.game.InputAction
import com.example.game.Lane
import com.example.game.ObstacleType
import com.example.game.PlayerController
import com.example.game.PowerUpManager
import com.example.game.TrackManager
import com.example.progression.PowerUpKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreetSurferLogicTest {

    @Test
    fun playerController_laneSwitchingAndInputBuffering_worksCorrectly() {
        val player = PlayerController()
        player.resetForNewRun()

        // Switch left to LEFT lane (screen-left is +X when camera looks along +Z)
        player.queueInput(InputAction.SwipeLeft)
        repeat(20) {
            player.update(0.016f, 15f, false, false, 0f, false, {}, {}, {}, {})
        }
        assertEquals(Lane.LEFT, player.targetLane)
        assertTrue(player.worldX > 2.0f)
        assertTrue(Lane.RIGHT.worldX() < -2.0f)

        // Jump, then buffer fast-fall SwipeDown mid-air -> should slam down and immediately enter roll
        player.queueInput(InputAction.SwipeUp)
        player.update(0.016f, 15f, false, false, 0f, false, {}, {}, {}, {})
        assertFalse(player.isGrounded)

        player.queueInput(InputAction.SwipeDown)
        var landed = false
        repeat(60) {
            player.update(0.016f, 15f, false, false, 0f, false, {}, {}, {}, {})
            if (player.isGrounded && !landed) {
                landed = true
                assertTrue("Player should auto-roll on fast-fall touchdown", player.isRolling)
                assertEquals(PlayerController.ROLL_HEIGHT, player.currentHitboxHeight(), 0.001f)
            }
        }
        assertTrue(landed)
    }

    @Test
    fun playerController_chaserRecoversAfterEightCleanSeconds() {
        val player = PlayerController()
        player.resetForNewRun()
        // Wait out initial chaser
        repeat(250) {
            player.update(0.02f, 15f, false, false, 0f, false, {}, {}, {}, {})
        }
        assertEquals(0f, player.chaserTimer, 0.001f)

        // Trigger stumble
        var stumbled = false
        val fatal = player.triggerStumble { stumbled = true }
        assertTrue(stumbled)
        assertFalse(fatal)
        assertEquals(8.0f, player.chaserTimer, 0.001f)

        // Second hit immediately while invulnerable is ignored; wait 1s then second hit is fatal
        repeat(60) {
            player.update(0.02f, 15f, false, false, 0f, false, {}, {}, {}, {})
        }
        val fatalSecond = player.triggerStumble {}
        assertTrue(fatalSecond)

        // Reset stumble and run 8 clean seconds -> chaser drops off
        val player2 = PlayerController()
        player2.resetForNewRun()
        repeat(250) { player2.update(0.02f, 15f, false, false, 0f, false, {}, {}, {}, {}) }
        player2.triggerStumble {}
        repeat(420) {
            player2.update(0.02f, 15f, false, false, 0f, false, {}, {}, {}, {})
        }
        assertEquals(0f, player2.chaserTimer, 0.001f)
    }

    @Test
    fun trackManager_neverBlocksAllThreeLanesSimultaneously() {
        val tm = TrackManager()
        tm.reset(false)
        var simZ = 0f
        repeat(150) {
            simZ += 12f
            tm.update(0.05f, simZ, false) {}
            // Check every 2m slice ahead for solvability (at least 1 lane not blocked by full wall/train)
            val checkZ = simZ + 12f
            val blockedLanes = mutableSetOf<Int>()
            for (o in tm.obstacles) {
                if (!o.active) continue
                if (o.laneIndex !in -1..1) continue
                if (checkZ in o.zStart..o.zEnd &&
                    (o.type == ObstacleType.FULL_BLOCKER ||
                        o.type == ObstacleType.STATIONARY_TRAIN ||
                        o.type == ObstacleType.MOVING_TRAIN)
                ) {
                    blockedLanes.add(o.laneIndex)
                }
            }
            assertTrue("All 3 lanes blocked at z=$checkZ", blockedLanes.size < 3)
        }
    }

    @Test
    fun powerUpManager_respectsUpgradeLevelsAndHoverboardShield() {
        val pm = PowerUpManager()
        pm.configureUpgradeLevels(
            mapOf(
                PowerUpKind.JETPACK to 6,
                PowerUpKind.MAGNET to 3
            )
        )
        pm.activate(PowerUpKind.JETPACK)
        assertEquals(PowerUpKind.JETPACK.durationForLevel(6), pm.jetpackRemaining, 0.01f)

        pm.activate(PowerUpKind.MAGNET)
        assertEquals(PowerUpKind.MAGNET.durationForLevel(3), pm.magnetRemaining, 0.01f)

        assertTrue(pm.activateHoverboard())
        assertTrue(pm.isHoverboardActive)
        assertTrue(pm.consumeHoverboardOnCrash())
        assertFalse(pm.isHoverboardActive)
    }
}
