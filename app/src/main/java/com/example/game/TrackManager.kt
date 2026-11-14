package com.example.game

import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class PooledChunk {
    var active: Boolean = false
    var chunkIndex: Int = 0
    var zStart: Float = 0f
    var hasTunnel: Boolean = false
    var hasBridge: Boolean = false
    var hasSupportPillars: Boolean = false
    var graffitiPalette: Int = 0
}

class PooledObstacle {
    var active: Boolean = false
    var type: ObstacleType = ObstacleType.LOW_HURDLE
    var laneIndex: Int = 0 // -1, 0, 1
    var customX: Float = 0f // Used for between-track pillars or spanning arches
    var zStart: Float = 0f
    var zEnd: Float = 0f
    var bottomY: Float = 0f
    var topY: Float = 1.15f
    var rampLength: Float = 0f
    var speedZ: Float = 0f // Negative = moves toward player
    var hornPlayed: Boolean = false
    var colorVariant: Int = 0
}

class PooledCollectible {
    var active: Boolean = false
    var kind: CollectibleKind = CollectibleKind.COIN
    var x: Float = 0f
    var y: Float = 0.8f
    var z: Float = 0f
    var laneIndex: Int = 0
    var spinPhase: Float = 0f
}

class ObstacleSpawner(private val random: Random = Random(System.currentTimeMillis())) {
    private var lastPowerUpChunk = -4

    fun populateChunk(
        chunk: PooledChunk,
        chunkIndex: Int,
        obstaclePool: Array<PooledObstacle>,
        collectiblePool: Array<PooledCollectible>,
        isJetpackActive: Boolean
    ) {
        val zBase = chunk.zStart
        chunk.graffitiPalette = chunkIndex % 4
        chunk.hasTunnel = chunkIndex > 3 && chunkIndex % 7 == 0
        chunk.hasBridge = !chunk.hasTunnel && chunkIndex > 1 && chunkIndex % 4 == 0
        chunk.hasSupportPillars = chunkIndex > 2 && (chunkIndex % 3 == 0)

        // Environmental Architecture: Tunnel Arch / Overhead Bridge / Support Pillars
        if (chunk.hasTunnel) {
            spawnObstacle(obstaclePool)?.apply {
                type = ObstacleType.TUNNEL_ARCH
                laneIndex = 0
                customX = 0f
                zStart = zBase + 2f
                zEnd = zBase + 22f
                bottomY = 4.6f // Overhead vault clear above train roofs
                topY = 7.4f
                colorVariant = chunkIndex % 3
            }
        } else if (chunk.hasBridge) {
            spawnObstacle(obstaclePool)?.apply {
                type = ObstacleType.OVERHEAD_BRIDGE
                laneIndex = 0
                customX = 0f
                zStart = zBase + 10f
                zEnd = zBase + 14.5f
                bottomY = 4.75f
                topY = 6.2f
                colorVariant = chunkIndex % 4
            }
        }

        if (chunk.hasSupportPillars && !chunk.hasTunnel) {
            // Narrow support pillars placed strictly BETWEEN lanes (-1.35f and +1.35f)
            val pillarX = if (chunkIndex % 2 == 0) -Lane.LANE_WIDTH * 0.5f else Lane.LANE_WIDTH * 0.5f
            spawnObstacle(obstaclePool)?.apply {
                type = ObstacleType.SUPPORT_PILLAR
                laneIndex = 99 // Between tracks, non-lane blocker
                customX = pillarX
                zStart = zBase + 11.5f
                zEnd = zBase + 12.4f
                bottomY = 0f
                topY = 5.2f
                colorVariant = chunkIndex % 3
            }
        }

        // First 2 chunks are gentle warm-up with coins only
        if (chunkIndex < 2) {
            spawnStraightCoins(collectiblePool, 0, zBase + 4f, 6, 0.8f)
            return
        }

        // If Jetpack is active, fill the sky with lush aerial coins across all lanes
        if (isJetpackActive) {
            val waveLane = (chunkIndex % 3) - 1
            spawnStraightCoins(collectiblePool, waveLane, zBase + 2f, 7, 6.2f)
            spawnStraightCoins(collectiblePool, ((waveLane + 2) % 3) - 1, zBase + 12f, 5, 6.2f)
        }

        // Choose a guaranteed-solvable pattern for this 24m chunk
        val pattern = when {
            chunkIndex < 4 -> random.nextInt(0, 3)
            else -> random.nextInt(0, 7)
        }

        when (pattern) {
            0 -> spawnHurdlesAndBarriersPattern(zBase, chunkIndex, obstaclePool, collectiblePool)
            1 -> spawnRampTrainComboPattern(zBase, chunkIndex, obstaclePool, collectiblePool)
            2 -> spawnMovingTrainLanePattern(zBase, chunkIndex, obstaclePool, collectiblePool)
            3 -> spawnTwinTrainCorridorPattern(zBase, chunkIndex, obstaclePool, collectiblePool)
            4 -> spawnSlalomBlockersPattern(zBase, chunkIndex, obstaclePool, collectiblePool)
            5 -> spawnRooftopLeapPattern(zBase, chunkIndex, obstaclePool, collectiblePool)
            else -> spawnMixedParkourPattern(zBase, chunkIndex, obstaclePool, collectiblePool)
        }

        // Spawn a Power-Up or rare Key/Hoverboard every 4-6 chunks in a safe reachable lane
        if (chunkIndex - lastPowerUpChunk >= 4 && random.nextFloat() < 0.65f) {
            lastPowerUpChunk = chunkIndex
            val safeLane = findSafeLaneAtZ(obstaclePool, zBase + 12f)
            if (safeLane != null) {
                val roll = random.nextFloat()
                val kind = when {
                    roll < 0.22f -> CollectibleKind.JETPACK
                    roll < 0.44f -> CollectibleKind.SUPER_SNEAKERS
                    roll < 0.68f -> CollectibleKind.COIN_MAGNET
                    roll < 0.88f -> CollectibleKind.DOUBLE_MULTIPLIER
                    roll < 0.95f -> CollectibleKind.HOVERBOARD_PICKUP
                    else -> CollectibleKind.KEY_PICKUP
                }
                spawnCollectible(collectiblePool)?.apply {
                    this.kind = kind
                    this.laneIndex = safeLane
                    this.x = Lane.fromIndex(safeLane).worldX()
                    this.y = 1.05f
                    this.z = zBase + 12f
                    this.spinPhase = random.nextFloat() * 6.28f
                }
            }
        }
    }

    private fun spawnHurdlesAndBarriersPattern(
        zBase: Float,
        chunkIndex: Int,
        obstacles: Array<PooledObstacle>,
        collectibles: Array<PooledCollectible>
    ) {
        val laneA = (chunkIndex % 3) - 1
        val laneB = ((laneA + 2) % 3) - 1
        val laneC = ((laneA + 3) % 3) - 1

        // Row 1: Low hurdle in laneA with jump coin arc, open in laneB & laneC
        spawnLowHurdle(obstacles, laneA, zBase + 4f, chunkIndex)
        spawnJumpArcCoins(collectibles, laneA, zBase + 4f, 0.8f)

        // Row 2: High barrier in laneB (roll under) + Full blocker in laneC, laneA open
        spawnHighBarrier(obstacles, laneB, zBase + 12.5f, chunkIndex)
        spawnFullBlocker(obstacles, laneC, zBase + 12.5f, chunkIndex)
        spawnStraightCoins(collectibles, laneB, zBase + 10.5f, 3, 0.55f)

        // Diagonal trail toward open laneA at end of chunk
        spawnDiagonalCoins(collectibles, laneB, laneA, zBase + 15.5f, 4)
    }

    private fun spawnRampTrainComboPattern(
        zBase: Float,
        chunkIndex: Int,
        obstacles: Array<PooledObstacle>,
        collectibles: Array<PooledCollectible>
    ) {
        val rampLane = random.nextInt(-1, 2)
        val blockedLane = if (rampLane == 0) 1 else 0
        val openLane = (-1..1).first { it != rampLane && it != blockedLane }

        // Ramp train in rampLane (walkable up to roof!)
        spawnRampTrain(obstacles, rampLane, zBase + 2f, chunkIndex)
        spawnRoofCoins(collectibles, rampLane, zBase + 3.5f, 5)

        // Stationary train in blockedLane, open lane stays completely clear with coins
        spawnStationaryTrain(obstacles, blockedLane, zBase + 4f, chunkIndex)
        spawnStraightCoins(collectibles, openLane, zBase + 4f, 6, 0.8f)
    }

    private fun spawnMovingTrainLanePattern(
        zBase: Float,
        chunkIndex: Int,
        obstacles: Array<PooledObstacle>,
        collectibles: Array<PooledCollectible>
    ) {
        val trainLane = random.nextInt(-1, 2)
        val hurdleLane = if (trainLane == -1) 0 else trainLane - 1
        val freeLane = (-1..1).first { it != trainLane && it != hurdleLane }

        // Approaching moving train in trainLane
        spawnMovingTrain(obstacles, trainLane, zBase + 16f, chunkIndex)

        // Low hurdle in hurdleLane, freeLane open
        spawnLowHurdle(obstacles, hurdleLane, zBase + 9f, chunkIndex)
        spawnStraightCoins(collectibles, freeLane, zBase + 5f, 6, 0.8f)
    }

    private fun spawnTwinTrainCorridorPattern(
        zBase: Float,
        chunkIndex: Int,
        obstacles: Array<PooledObstacle>,
        collectibles: Array<PooledCollectible>
    ) {
        // 2 stationary trains leaving 1 clear corridor (Solvability Guarantee: never block all 3!)
        val openLane = random.nextInt(-1, 2)
        for (lane in -1..1) {
            if (lane != openLane) {
                if (lane == (openLane + 1).coerceAtMost(1) && random.nextFloat() < 0.5f) {
                    spawnRampTrain(obstacles, lane, zBase + 2f, chunkIndex)
                    spawnRoofCoins(collectibles, lane, zBase + 4f, 4)
                } else {
                    spawnStationaryTrain(obstacles, lane, zBase + 3f, chunkIndex)
                }
            }
        }
        spawnLowHurdle(obstacles, openLane, zBase + 12f, chunkIndex)
        spawnJumpArcCoins(collectibles, openLane, zBase + 12f, 0.8f)
    }

    private fun spawnSlalomBlockersPattern(
        zBase: Float,
        chunkIndex: Int,
        obstacles: Array<PooledObstacle>,
        collectibles: Array<PooledCollectible>
    ) {
        val safe1 = -1
        val safe2 = 1
        // Row 1: Center & Right blocked -> Left open
        spawnFullBlocker(obstacles, 0, zBase + 4f, chunkIndex)
        spawnHighBarrier(obstacles, 1, zBase + 4f, chunkIndex)
        // Row 2: Left & Center blocked -> Right open
        spawnFullBlocker(obstacles, -1, zBase + 14f, chunkIndex)
        spawnLowHurdle(obstacles, 0, zBase + 14f, chunkIndex)

        spawnDiagonalCoins(collectibles, safe1, safe2, zBase + 6f, 5)
    }

    private fun spawnRooftopLeapPattern(
        zBase: Float,
        chunkIndex: Int,
        obstacles: Array<PooledObstacle>,
        collectibles: Array<PooledCollectible>
    ) {
        val laneA = -1
        val laneB = 0
        // Ramp train on left followed by stationary train in center so player can jump roof-to-roof!
        spawnRampTrain(obstacles, laneA, zBase + 1f, chunkIndex)
        spawnStationaryTrain(obstacles, laneB, zBase + 10f, chunkIndex)
        spawnRoofCoins(collectibles, laneA, zBase + 3f, 3)
        spawnRoofCoins(collectibles, laneB, zBase + 12f, 4)
        // Right lane remains passable on ground
        spawnStraightCoins(collectibles, 1, zBase + 4f, 6, 0.8f)
    }

    private fun spawnMixedParkourPattern(
        zBase: Float,
        chunkIndex: Int,
        obstacles: Array<PooledObstacle>,
        collectibles: Array<PooledCollectible>
    ) {
        val movingLane = (chunkIndex % 3) - 1
        val barrierLane = if (movingLane == 0) -1 else 0
        val openLane = (-1..1).first { it != movingLane && it != barrierLane }

        spawnMovingTrain(obstacles, movingLane, zBase + 18f, chunkIndex)
        spawnHighBarrier(obstacles, barrierLane, zBase + 8f, chunkIndex)
        spawnLowHurdle(obstacles, openLane, zBase + 14f, chunkIndex)
        spawnJumpArcCoins(collectibles, openLane, zBase + 14f, 0.8f)
    }

    private fun findSafeLaneAtZ(obstacles: Array<PooledObstacle>, z: Float): Int? {
        val candidates = listOf(0, -1, 1).shuffled(random)
        for (lane in candidates) {
            var blocked = false
            for (o in obstacles) {
                if (!o.active) continue
                if (o.laneIndex == lane && z >= o.zStart - 2.5f && z <= o.zEnd + 2.5f) {
                    blocked = true
                    break
                }
            }
            if (!blocked) return lane
        }
        return null
    }

    private fun spawnLowHurdle(pool: Array<PooledObstacle>, lane: Int, z: Float, variant: Int) {
        spawnObstacle(pool)?.apply {
            type = ObstacleType.LOW_HURDLE
            laneIndex = lane
            customX = Lane.fromIndex(lane).worldX()
            zStart = z
            zEnd = z + 0.8f
            bottomY = 0f
            topY = 1.15f
            colorVariant = variant % 4
        }
    }

    private fun spawnHighBarrier(pool: Array<PooledObstacle>, lane: Int, z: Float, variant: Int) {
        spawnObstacle(pool)?.apply {
            type = ObstacleType.HIGH_BARRIER
            laneIndex = lane
            customX = Lane.fromIndex(lane).worldX()
            zStart = z
            zEnd = z + 0.85f
            bottomY = 1.15f
            topY = 2.6f
            colorVariant = variant % 4
        }
    }

    private fun spawnFullBlocker(pool: Array<PooledObstacle>, lane: Int, z: Float, variant: Int) {
        spawnObstacle(pool)?.apply {
            type = ObstacleType.FULL_BLOCKER
            laneIndex = lane
            customX = Lane.fromIndex(lane).worldX()
            zStart = z
            zEnd = z + 1.1f
            bottomY = 0f
            topY = 2.65f
            colorVariant = variant % 4
        }
    }

    private fun spawnStationaryTrain(pool: Array<PooledObstacle>, lane: Int, z: Float, variant: Int) {
        spawnObstacle(pool)?.apply {
            type = ObstacleType.STATIONARY_TRAIN
            laneIndex = lane
            customX = Lane.fromIndex(lane).worldX()
            zStart = z
            zEnd = z + 13.5f
            bottomY = 0f
            topY = 3.25f
            rampLength = 0f
            speedZ = 0f
            colorVariant = variant % 4
        }
    }

    private fun spawnRampTrain(pool: Array<PooledObstacle>, lane: Int, z: Float, variant: Int) {
        spawnObstacle(pool)?.apply {
            type = ObstacleType.RAMP_TRAIN
            laneIndex = lane
            customX = Lane.fromIndex(lane).worldX()
            zStart = z
            zEnd = z + 16.5f
            bottomY = 0f
            topY = 3.25f
            rampLength = 5.0f
            speedZ = 0f
            colorVariant = variant % 4
        }
    }

    private fun spawnMovingTrain(pool: Array<PooledObstacle>, lane: Int, z: Float, variant: Int) {
        spawnObstacle(pool)?.apply {
            type = ObstacleType.MOVING_TRAIN
            laneIndex = lane
            customX = Lane.fromIndex(lane).worldX()
            zStart = z
            zEnd = z + 14.0f
            bottomY = 0f
            topY = 3.25f
            rampLength = 0f
            speedZ = -13.5f // Moves toward player
            hornPlayed = false
            colorVariant = variant % 4
        }
    }

    private fun spawnStraightCoins(
        pool: Array<PooledCollectible>,
        lane: Int,
        startZ: Float,
        count: Int,
        yHeight: Float
    ) {
        val laneX = Lane.fromIndex(lane).worldX()
        for (i in 0 until count) {
            spawnCollectible(pool)?.apply {
                kind = CollectibleKind.COIN
                laneIndex = lane
                x = laneX
                y = yHeight
                z = startZ + i * 2.4f
                spinPhase = i * 0.45f
            }
        }
    }

    private fun spawnRoofCoins(pool: Array<PooledCollectible>, lane: Int, startZ: Float, count: Int) {
        val laneX = Lane.fromIndex(lane).worldX()
        for (i in 0 until count) {
            spawnCollectible(pool)?.apply {
                kind = CollectibleKind.COIN
                laneIndex = lane
                x = laneX
                y = 4.05f
                z = startZ + 3.0f + i * 2.3f
                spinPhase = i * 0.5f
            }
        }
    }

    private fun spawnJumpArcCoins(pool: Array<PooledCollectible>, lane: Int, centerZ: Float, baseHeight: Float) {
        val laneX = Lane.fromIndex(lane).worldX()
        for (i in -2..2) {
            val t = (i + 2) / 4f
            val arcY = baseHeight + sin(t * Math.PI).toFloat() * 1.95f
            spawnCollectible(pool)?.apply {
                kind = CollectibleKind.COIN
                laneIndex = lane
                x = laneX
                y = arcY
                z = centerZ + i * 1.6f
                spinPhase = (i + 2) * 0.4f
            }
        }
    }

    private fun spawnDiagonalCoins(
        pool: Array<PooledCollectible>,
        fromLane: Int,
        toLane: Int,
        startZ: Float,
        count: Int
    ) {
        val x0 = Lane.fromIndex(fromLane).worldX()
        val x1 = Lane.fromIndex(toLane).worldX()
        for (i in 0 until count) {
            val t = i.toFloat() / maxOf(1, count - 1)
            spawnCollectible(pool)?.apply {
                kind = CollectibleKind.COIN
                laneIndex = if (t < 0.5f) fromLane else toLane
                x = x0 + (x1 - x0) * t
                y = 0.85f
                z = startZ + i * 1.9f
                spinPhase = i * 0.5f
            }
        }
    }

    private fun spawnObstacle(pool: Array<PooledObstacle>): PooledObstacle? {
        for (item in pool) {
            if (!item.active) {
                item.active = true
                item.speedZ = 0f
                item.rampLength = 0f
                item.hornPlayed = false
                return item
            }
        }
        return null
    }

    private fun spawnCollectible(pool: Array<PooledCollectible>): PooledCollectible? {
        for (item in pool) {
            if (!item.active) {
                item.active = true
                return item
            }
        }
        return null
    }
}

class TrackManager {
    companion object {
        const val CHUNK_LENGTH = 24f
        const val CHUNK_POOL_SIZE = 14
        const val OBSTACLE_POOL_SIZE = 84
        const val COLLECTIBLE_POOL_SIZE = 260
    }

    val chunks = Array(CHUNK_POOL_SIZE) { PooledChunk() }
    val obstacles = Array(OBSTACLE_POOL_SIZE) { PooledObstacle() }
    val collectibles = Array(COLLECTIBLE_POOL_SIZE) { PooledCollectible() }

    private val spawner = ObstacleSpawner()
    private var nextChunkIndex = 0

    fun reset(isJetpackActive: Boolean = false) {
        for (c in chunks) c.active = false
        for (o in obstacles) o.active = false
        for (col in collectibles) col.active = false
        nextChunkIndex = -1
        for (i in chunks.indices) {
            activateChunk(chunks[i], nextChunkIndex++, isJetpackActive)
        }
    }

    fun update(
        dt: Float,
        playerZ: Float,
        isJetpackActive: Boolean,
        onTrainHorn: () -> Unit
    ) {
        // Advance moving trains and trigger horns when approaching
        for (o in obstacles) {
            if (!o.active) continue
            if (o.speedZ != 0f) {
                val delta = o.speedZ * dt
                o.zStart += delta
                o.zEnd += delta
                if (!o.hornPlayed && o.zStart - playerZ in 18f..68f) {
                    o.hornPlayed = true
                    onTrainHorn()
                }
            }
            if (o.zEnd < playerZ - 26f) {
                o.active = false
            }
        }

        // Recycle passed collectibles
        for (col in collectibles) {
            if (!col.active) continue
            col.spinPhase += dt * 4.2f
            if (col.z < playerZ - 14f) {
                col.active = false
            }
        }

        // Recycle passed track chunks seamlessly
        for (c in chunks) {
            if (!c.active) continue
            if (c.zStart + CHUNK_LENGTH < playerZ - 24f) {
                activateChunk(c, nextChunkIndex++, isJetpackActive)
            }
        }
    }

    fun clearObstaclesAhead(playerZ: Float, clearDistance: Float) {
        for (o in obstacles) {
            if (!o.active) continue
            if (o.type == ObstacleType.TUNNEL_ARCH ||
                o.type == ObstacleType.OVERHEAD_BRIDGE ||
                o.type == ObstacleType.SUPPORT_PILLAR
            ) continue
            if (o.zEnd >= playerZ - 3f && o.zStart <= playerZ + clearDistance) {
                o.active = false
            }
        }
    }

    fun evaluateSurfaceUnderPlayer(laneX: Float, playerZ: Float, playerY: Float): Pair<Float, Boolean> {
        var maxSurfaceY = 0f
        var onRoof = false
        for (o in obstacles) {
            if (!o.active) continue
            if (o.type != ObstacleType.STATIONARY_TRAIN &&
                o.type != ObstacleType.RAMP_TRAIN &&
                o.type != ObstacleType.MOVING_TRAIN
            ) continue
            if (abs(laneX - o.customX) > 1.15f) continue
            if (playerZ < o.zStart - 0.35f || playerZ > o.zEnd + 0.35f) continue

            if (o.type == ObstacleType.RAMP_TRAIN && o.rampLength > 0f && playerZ < o.zStart + o.rampLength) {
                val progress = ((playerZ - o.zStart) / o.rampLength).coerceIn(0f, 1f)
                val rampY = progress * o.topY
                if (playerY >= rampY - 0.85f && rampY > maxSurfaceY) {
                    maxSurfaceY = rampY
                    onRoof = progress > 0.7f
                }
            } else {
                if (playerY >= o.topY - 0.75f && o.topY > maxSurfaceY) {
                    maxSurfaceY = o.topY
                    onRoof = true
                }
            }
        }
        return maxSurfaceY to onRoof
    }

    private fun activateChunk(chunk: PooledChunk, chunkIndex: Int, isJetpackActive: Boolean) {
        chunk.active = true
        chunk.chunkIndex = chunkIndex
        chunk.zStart = chunkIndex * CHUNK_LENGTH
        if (chunkIndex >= 0) {
            spawner.populateChunk(chunk, chunkIndex, obstacles, collectibles, isJetpackActive)
        }
    }
}
