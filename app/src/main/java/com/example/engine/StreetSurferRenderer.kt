package com.example.engine

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.CollectibleKind
import com.example.game.GameManager
import com.example.game.GamePhase
import com.example.game.Lane
import com.example.game.ObstacleType
import com.example.game.ParticleBurstKind
import com.example.game.PooledObstacle
import com.example.game.TrackManager
import com.example.progression.CharacterSkin
import com.example.progression.HoverboardStyle
import com.example.progression.StreetCatalog
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class StreetSurferRenderer(
    private val gameManager: GameManager
) : GLSurfaceView.Renderer {

    private lateinit var shader: ShaderProgram
    private lateinit var cubeMesh: MeshBuffer
    private lateinit var sphereMesh: MeshBuffer
    private lateinit var cylinderMesh: MeshBuffer
    private lateinit var rampMesh: MeshBuffer
    private lateinit var coinMesh: MeshBuffer
    private lateinit var crystalMesh: MeshBuffer

    private val particleSystem = ParticleSystem()

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var viewportWidth = 1080
    private var viewportHeight = 1920
    private var lastFrameNs = System.nanoTime()

    // Smooth Dynamic Camera State
    private var camX = 0f
    private var camY = 3.2f
    private var currentFov = 54f
    private var menuOrbitTime = 0f
    private var playBlend = 0f // 0f = Menu hero framing, 1f = Gameplay chase framing
    private var trailEmitAccum = 0f

    private var eyeX = 0f
    private var eyeY = 3.2f
    private var eyeZ = -5.6f

    @Volatile
    var equippedCharacter: CharacterSkin = StreetCatalog.characters.first()

    @Volatile
    var equippedBoard: HoverboardStyle = StreetCatalog.hoverboards.first()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.42f, 0.80f, 0.99f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        shader = ShaderProgram()
        cubeMesh = MeshBuilder.createUnitCube()
        sphereMesh = MeshBuilder.createSmoothSphere(12, 18)
        cylinderMesh = MeshBuilder.createSmoothCylinder(18)
        rampMesh = MeshBuilder.createRampWedge()
        coinMesh = MeshBuilder.createCoinDisc(24)
        crystalMesh = MeshBuilder.createDiamondCrystal()
        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastFrameNs) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
        lastFrameNs = nowNs

        gameManager.update(dt)

        while (true) {
            val ev = gameManager.particleEvents.poll() ?: break
            when (ev.kind) {
                ParticleBurstKind.DUST_PUFF -> particleSystem.emitDust(ev.x, ev.y, ev.z)
                ParticleBurstKind.COIN_SPARKLE -> particleSystem.emitCoinSparkle(ev.x, ev.y, ev.z)
                ParticleBurstKind.POWERUP_BURST -> particleSystem.emitPowerUpBurst(ev.x, ev.y, ev.z, ev.r, ev.g, ev.b)
                ParticleBurstKind.HOVERBOARD_EXPLOSION -> particleSystem.emitHoverboardShockwave(ev.x, ev.y, ev.z, ev.r, ev.g, ev.b)
            }
        }

        if (gameManager.phase == GamePhase.PLAYING) {
            trailEmitAccum += dt
            if (trailEmitAccum > 0.025f) {
                trailEmitAccum = 0f
                val p = gameManager.player
                if (gameManager.powerUpManager.isJetpackActive) {
                    particleSystem.emitTrail(p.worldX - 0.16f, p.worldY + 0.7f, p.worldZ - 0.35f, 1.0f, 0.48f, 0.05f)
                    particleSystem.emitTrail(p.worldX + 0.16f, p.worldY + 0.7f, p.worldZ - 0.35f, 1.0f, 0.86f, 0.14f)
                } else if (gameManager.powerUpManager.isHoverboardActive) {
                    val g = equippedBoard.glowRgb
                    particleSystem.emitTrail(p.worldX, p.worldY + 0.14f, p.worldZ - 0.75f, g[0], g[1], g[2])
                }
            }
        }
        particleSystem.update(dt)

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        shader.use()

        updateCamera(dt)
        GLES30.glUniform3f(shader.uViewPosLoc, eyeX, eyeY, eyeZ)

        // Sky Backdrop & Sun
        drawSkyBackdrop()

        // Track Chunks & Urban Skyline
        for (chunk in gameManager.trackManager.chunks) {
            if (!chunk.active) continue
            drawTrackChunk(chunk.zStart, chunk.graffitiPalette)
        }

        // Obstacles & Trains
        for (o in gameManager.trackManager.obstacles) {
            if (!o.active) continue
            drawObstacle(o)
        }

        // Runner + Ground Shadow + Hoverboard/Jetpack
        drawRunner()

        // Pursuing Guard & Dog
        if (gameManager.player.chaserZOffset > -13f) {
            drawGuardAndDog()
        }

        // Collectibles
        drawCollectibles()

        // Pooled 3D Particle Spheres
        sphereMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        for (pt in particleSystem.particles) {
            if (!pt.active) continue
            val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
            val s = pt.size * (0.55f + alpha * 0.65f)
            drawMesh(sphereMesh, pt.x, pt.y, pt.z, s, s, s, 0f, 0f, pt.r, pt.g, pt.b, alpha, 0.45f, 24f, ShaderProgram.MAT_STANDARD)
        }
    }

    private fun updateCamera(dt: Float) {
        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
        val player = gameManager.player

        if (gameManager.phase == GamePhase.MENU) {
            menuOrbitTime += dt
            playBlend = (playBlend - dt * 2.5f).coerceAtLeast(0f)
        } else {
            playBlend = (playBlend + dt * 1.8f).coerceAtMost(1f)
        }

        // Smooth cubic ease-out blend between Menu Hero Orbit and Chase Camera
        val t = playBlend
        val smoothBlend = t * t * (3f - 2f * t)

        val menuEyeX = sin(menuOrbitTime * 0.42f) * 2.1f
        val menuEyeY = 2.65f
        val menuEyeZ = -4.6f + cos(menuOrbitTime * 0.35f) * 0.55f
        val menuLookX = 0f
        val menuLookY = 1.35f
        val menuLookZ = 6.5f

        val speedNorm = ((gameManager.speed - GameManager.BASE_SPEED) / (GameManager.MAX_SPEED - GameManager.BASE_SPEED)).coerceIn(0f, 1.2f)
        val jetpackBoost = if (gameManager.powerUpManager.isJetpackActive) 7.5f else 0f
        val targetFov = 54f + smoothBlend * (4f + speedNorm * 9f + jetpackBoost)
        currentFov += (targetFov - currentFov) * (dt * 4.5f).coerceAtMost(1f)
        Matrix.perspectiveM(projMatrix, 0, currentFov, aspect, 0.2f, 220f)

        camX += (player.worldX * 0.80f - camX) * (dt * 9.5f).coerceAtMost(1f)
        val targetCamY = 4.35f + player.worldY * 0.62f + (if (gameManager.powerUpManager.isJetpackActive) 1.15f else 0f)
        camY += (targetCamY - camY) * (dt * 7.0f).coerceAtMost(1f)

        var shakeX = 0f
        var shakeY = 0f
        if (gameManager.cameraShakeTimer > 0f) {
            val mag = gameManager.cameraShakeIntensity * (gameManager.cameraShakeTimer / 0.4f).coerceAtMost(1f)
            shakeX = sin(gameManager.cameraShakeTimer * 78f) * mag
            shakeY = cos(gameManager.cameraShakeTimer * 64f) * mag * 0.7f
        }

        val chaseEyeX = camX + shakeX
        val chaseEyeY = camY + shakeY
        val chaseEyeZ = player.worldZ - 7.6f
        val chaseLookX = camX * 0.88f
        val chaseLookY = player.worldY + 1.50f
        val chaseLookZ = player.worldZ + 13.0f

        eyeX = menuEyeX + (chaseEyeX - menuEyeX) * smoothBlend
        eyeY = menuEyeY + (chaseEyeY - menuEyeY) * smoothBlend
        eyeZ = menuEyeZ + (chaseEyeZ - menuEyeZ) * smoothBlend
        val lookX = menuLookX + (chaseLookX - menuLookX) * smoothBlend
        val lookY = menuLookY + (chaseLookY - menuLookY) * smoothBlend
        val lookZ = menuLookZ + (chaseLookZ - menuLookZ) * smoothBlend

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            lookX, lookY, lookZ,
            player.laneSwitchTilt * 0.045f * smoothBlend, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
    }

    private fun drawSkyBackdrop() {
        val pz = gameManager.player.worldZ
        // Distant sunlit sky dome backdrop plane
        GLES30.glDepthMask(false)
        cubeMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(cubeMesh, 0f, 34f, pz + 170f, 280f, 120f, 2f, 0f, 0f, 0.5f, 0.8f, 1.0f, 1f, 0f, 8f, ShaderProgram.MAT_SKY_BACKDROP)
        GLES30.glDepthMask(true)

        // Glowing sunlit disc & soft clouds
        sphereMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(sphereMesh, -28f, 38f, pz + 145f, 11f, 11f, 4f, 0f, 0f, 1.0f, 0.95f, 0.65f, 1f, 0.95f, 8f, ShaderProgram.MAT_STANDARD)
        drawMesh(sphereMesh, -28f, 38f, pz + 146f, 18f, 18f, 2f, 0f, 0f, 1.0f, 0.94f, 0.72f, 0.25f, 0.85f, 8f, ShaderProgram.MAT_STANDARD)
    }

    private fun drawTrackChunk(zStart: Float, paletteIdx: Int) {
        val len = TrackManager.CHUNK_LENGTH
        val cz = zStart + len * 0.5f

        cubeMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        // Procedural gravel/ballast rail-yard bed
        drawMesh(cubeMesh, 0f, -0.25f, cz, 11.0f, 0.5f, len, 0f, 0f, 0.27f, 0.30f, 0.36f, 1f, 0f, 8f, ShaderProgram.MAT_GROUND_BALLAST)
        // Outer boulevard concrete platforms
        drawMesh(cubeMesh, -8.5f, -0.10f, cz, 6.2f, 0.65f, len, 0f, 0f, 0.68f, 0.70f, 0.74f, 1f, 0f, 12f, ShaderProgram.MAT_GROUND_BALLAST)
        drawMesh(cubeMesh, 8.5f, -0.10f, cz, 6.2f, 0.65f, len, 0f, 0f, 0.68f, 0.70f, 0.74f, 1f, 0f, 12f, ShaderProgram.MAT_GROUND_BALLAST)

        // 3 Parallel Tracks (metallic rails + wooden sleepers)
        for (lane in -1..1) {
            val lx = Lane.fromIndex(lane).worldX()
            drawMesh(cubeMesh, lx - 0.62f, 0.05f, cz, 0.11f, 0.10f, len, 0f, 0f, 0.74f, 0.79f, 0.86f, 1f, 0.04f, 64f, ShaderProgram.MAT_METALLIC)
            drawMesh(cubeMesh, lx + 0.62f, 0.05f, cz, 0.11f, 0.10f, len, 0f, 0f, 0.74f, 0.79f, 0.86f, 1f, 0.04f, 64f, ShaderProgram.MAT_METALLIC)
            for (step in 0 until 8) {
                val tz = zStart + step * 3f + 1.5f
                drawMesh(cubeMesh, lx, 0.01f, tz, 1.68f, 0.05f, 0.34f, 0f, 0f, 0.38f, 0.25f, 0.16f, 1f, 0f, 10f, ShaderProgram.MAT_STANDARD)
            }
        }

        // Retaining walls with colorful street-art murals
        val wallColor = when (paletteIdx) {
            0 -> floatArrayOf(0.98f, 0.30f, 0.38f)
            1 -> floatArrayOf(0.04f, 0.72f, 0.96f)
            2 -> floatArrayOf(0.99f, 0.82f, 0.10f)
            else -> floatArrayOf(0.05f, 0.86f, 0.50f)
        }
        drawMesh(cubeMesh, -6.2f, 1.45f, cz, 0.45f, 2.9f, len, 0f, 0f, 0.86f, 0.84f, 0.78f, 1f, 0f, 14f, ShaderProgram.MAT_GROUND_BALLAST)
        drawMesh(cubeMesh, 6.2f, 1.45f, cz, 0.45f, 2.9f, len, 0f, 0f, 0.86f, 0.84f, 0.78f, 1f, 0f, 14f, ShaderProgram.MAT_GROUND_BALLAST)
        drawMesh(cubeMesh, -5.94f, 1.55f, cz, 0.08f, 1.65f, len * 0.74f, 0f, 0f, wallColor[0], wallColor[1], wallColor[2], 1f, 0.08f, 24f, ShaderProgram.MAT_STANDARD)
        drawMesh(cubeMesh, 5.94f, 1.55f, cz, 0.08f, 1.65f, len * 0.74f, 0f, 0f, wallColor[0], wallColor[1], wallColor[2], 1f, 0.08f, 24f, ShaderProgram.MAT_STANDARD)

        // Urban Windowed Skyline Buildings
        val bHeightL = 11f + (paletteIdx * 3.5f)
        val bHeightR = 13f + ((3 - paletteIdx) * 3.0f)
        drawMesh(cubeMesh, -12.8f, bHeightL * 0.5f, zStart + 7f, 7.2f, bHeightL, 11.5f, 0f, 0f, 0.86f, 0.74f, 0.62f, 1f, 0f, 32f, ShaderProgram.MAT_BUILDING_WINDOWS)
        drawMesh(cubeMesh, -12.4f, bHeightL * 0.42f, zStart + 19f, 6.4f, bHeightL * 0.84f, 9.5f, 0f, 0f, 0.62f, 0.72f, 0.82f, 1f, 0f, 32f, ShaderProgram.MAT_BUILDING_WINDOWS)
        drawMesh(cubeMesh, 12.8f, bHeightR * 0.5f, zStart + 9f, 7.2f, bHeightR, 11.5f, 0f, 0f, 0.88f, 0.66f, 0.54f, 1f, 0f, 32f, ShaderProgram.MAT_BUILDING_WINDOWS)
        drawMesh(cubeMesh, 12.4f, bHeightR * 0.44f, zStart + 20f, 6.4f, bHeightR * 0.88f, 9.5f, 0f, 0f, 0.54f, 0.74f, 0.76f, 1f, 0f, 32f, ShaderProgram.MAT_BUILDING_WINDOWS)

        // Roadside Trees & Streetlamps for vibrant realistic depth
        cylinderMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(cylinderMesh, -7.6f, 1.3f, zStart + 12f, 0.32f, 2.4f, 0.32f, 0f, 0f, 0.42f, 0.28f, 0.18f, 1f, 0f, 12f, ShaderProgram.MAT_STANDARD)
        drawMesh(cylinderMesh, 7.6f, 1.3f, zStart + 6f, 0.32f, 2.4f, 0.32f, 0f, 0f, 0.42f, 0.28f, 0.18f, 1f, 0f, 12f, ShaderProgram.MAT_STANDARD)
        sphereMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(sphereMesh, -7.6f, 3.1f, zStart + 12f, 2.3f, 2.1f, 2.3f, 0f, 0f, 0.16f, 0.68f, 0.32f, 1f, 0.04f, 12f, ShaderProgram.MAT_STANDARD)
        drawMesh(sphereMesh, 7.6f, 3.1f, zStart + 6f, 2.2f, 2.0f, 2.2f, 0f, 0f, 0.20f, 0.74f, 0.34f, 1f, 0.04f, 12f, ShaderProgram.MAT_STANDARD)
    }

    private fun drawObstacle(o: PooledObstacle) {
        val cz = (o.zStart + o.zEnd) * 0.5f
        val len = (o.zEnd - o.zStart).coerceAtLeast(0.4f)
        when (o.type) {
            ObstacleType.LOW_HURDLE -> {
                cylinderMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                drawMesh(cylinderMesh, o.customX - 0.85f, 0.55f, cz, 0.14f, 1.1f, 0.14f, 0f, 0f, 0.94f, 0.94f, 0.96f, 1f, 0f, 32f, ShaderProgram.MAT_METALLIC)
                drawMesh(cylinderMesh, o.customX + 0.85f, 0.55f, cz, 0.14f, 1.1f, 0.14f, 0f, 0f, 0.94f, 0.94f, 0.96f, 1f, 0f, 32f, ShaderProgram.MAT_METALLIC)
                cubeMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                drawMesh(cubeMesh, o.customX, 0.92f, cz, 2.02f, 0.42f, 0.22f, 0f, 0f, 1.0f, 0.32f, 0.22f, 1f, 0.08f, 24f, ShaderProgram.MAT_STANDARD)
                drawMesh(cubeMesh, o.customX, 0.92f, cz - 0.02f, 0.95f, 0.44f, 0.24f, 0f, 0f, 1.0f, 0.86f, 0.08f, 1f, 0.10f, 24f, ShaderProgram.MAT_STANDARD)
            }
            ObstacleType.HIGH_BARRIER -> {
                cylinderMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                drawMesh(cylinderMesh, o.customX - 0.95f, 1.35f, cz, 0.16f, 2.7f, 0.16f, 0f, 0f, 0.28f, 0.34f, 0.42f, 1f, 0f, 48f, ShaderProgram.MAT_METALLIC)
                drawMesh(cylinderMesh, o.customX + 0.95f, 1.35f, cz, 0.16f, 2.7f, 0.16f, 0f, 0f, 0.28f, 0.34f, 0.42f, 1f, 0f, 48f, ShaderProgram.MAT_METALLIC)
                cubeMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                drawMesh(cubeMesh, o.customX, 1.88f, cz, 2.16f, 1.32f, 0.34f, 0f, 0f, 0.02f, 0.70f, 0.98f, 1f, 0.06f, 24f, ShaderProgram.MAT_STANDARD)
                drawMesh(cubeMesh, o.customX, 1.30f, cz, 2.20f, 0.20f, 0.38f, 0f, 0f, 1.0f, 0.84f, 0.0f, 1f, 0.15f, 24f, ShaderProgram.MAT_STANDARD)
            }
            ObstacleType.FULL_BLOCKER -> {
                cubeMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                drawMesh(cubeMesh, o.customX, 1.32f, cz, 2.06f, 2.64f, len, 0f, 0f, 0.92f, 0.26f, 0.35f, 1f, 0.04f, 18f, ShaderProgram.MAT_GROUND_BALLAST)
                drawMesh(cubeMesh, o.customX, 2.38f, cz, 2.12f, 0.32f, len + 0.05f, 0f, 0f, 1.0f, 0.84f, 0.0f, 1f, 0.12f, 24f, ShaderProgram.MAT_STANDARD)
            }
            ObstacleType.STATIONARY_TRAIN, ObstacleType.RAMP_TRAIN, ObstacleType.MOVING_TRAIN -> {
                val bodyZStart = if (o.type == ObstacleType.RAMP_TRAIN) o.zStart + o.rampLength else o.zStart
                val bodyLen = (o.zEnd - bodyZStart).coerceAtLeast(2f)
                val bodyCz = (bodyZStart + o.zEnd) * 0.5f

                if (o.type == ObstacleType.RAMP_TRAIN && o.rampLength > 0f) {
                    val rampCz = o.zStart + o.rampLength * 0.5f
                    rampMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                    drawMesh(rampMesh, o.customX, o.topY * 0.5f, rampCz, 2.02f, o.topY, o.rampLength, 0f, 0f, 1.0f, 0.82f, 0.06f, 1f, 0.10f, 36f, ShaderProgram.MAT_METALLIC)
                }

                val bodyColor = when {
                    o.type == ObstacleType.MOVING_TRAIN -> floatArrayOf(0.92f, 0.20f, 0.26f)
                    o.colorVariant == 0 -> floatArrayOf(0.06f, 0.62f, 0.90f)
                    o.colorVariant == 1 -> floatArrayOf(0.96f, 0.50f, 0.12f)
                    o.colorVariant == 2 -> floatArrayOf(0.10f, 0.76f, 0.52f)
                    else -> floatArrayOf(0.56f, 0.32f, 0.92f)
                }
                cubeMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                // Sleek metallic train body
                drawMesh(cubeMesh, o.customX, 1.74f, bodyCz, 2.06f, 2.74f, bodyLen, 0f, 0f, bodyColor[0], bodyColor[1], bodyColor[2], 1f, 0.04f, 48f, ShaderProgram.MAT_METALLIC)
                // Brushed roof crown + HVAC vent pods
                drawMesh(cubeMesh, o.customX, 3.16f, bodyCz, 1.98f, 0.22f, bodyLen + 0.04f, 0f, 0f, 0.88f, 0.91f, 0.95f, 1f, 0.06f, 64f, ShaderProgram.MAT_METALLIC)
                drawMesh(cubeMesh, o.customX, 3.30f, bodyCz - bodyLen * 0.25f, 1.1f, 0.12f, 2.2f, 0f, 0f, 0.62f, 0.66f, 0.72f, 1f, 0f, 48f, ShaderProgram.MAT_METALLIC)
                drawMesh(cubeMesh, o.customX, 3.30f, bodyCz + bodyLen * 0.25f, 1.1f, 0.12f, 2.2f, 0f, 0f, 0.62f, 0.66f, 0.72f, 1f, 0f, 48f, ShaderProgram.MAT_METALLIC)
                // Dark reflective window band & silver trim
                drawMesh(cubeMesh, o.customX, 2.25f, bodyCz, 2.10f, 0.66f, bodyLen * 0.88f, 0f, 0f, 0.12f, 0.18f, 0.26f, 1f, 0f, 96f, ShaderProgram.MAT_METALLIC)
                drawMesh(cubeMesh, o.customX, 1.48f, bodyCz, 2.10f, 0.14f, bodyLen * 0.96f, 0f, 0f, 0.95f, 0.96f, 0.98f, 1f, 0.06f, 64f, ShaderProgram.MAT_METALLIC)
                // Front windshield
                drawMesh(cubeMesh, o.customX, 2.26f, bodyZStart - 0.02f, 1.56f, 0.74f, 0.08f, 0f, 0f, 0.10f, 0.18f, 0.28f, 1f, 0f, 96f, ShaderProgram.MAT_METALLIC)
                // Bogie wheels
                cylinderMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                for (wz in listOf(bodyZStart + 1.8f, bodyZStart + 3.2f, o.zEnd - 3.2f, o.zEnd - 1.8f)) {
                    drawMesh(cylinderMesh, o.customX - 0.78f, 0.28f, wz, 0.22f, 0.52f, 0.52f, 0f, 90f, 0.18f, 0.20f, 0.24f, 1f, 0f, 48f, ShaderProgram.MAT_METALLIC)
                    drawMesh(cylinderMesh, o.customX + 0.78f, 0.28f, wz, 0.22f, 0.52f, 0.52f, 0f, 90f, 0.18f, 0.20f, 0.24f, 1f, 0f, 48f, ShaderProgram.MAT_METALLIC)
                }

                // Glowing spherical headlights on moving trains
                if (o.type == ObstacleType.MOVING_TRAIN) {
                    sphereMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                    drawMesh(sphereMesh, o.customX - 0.60f, 1.08f, bodyZStart - 0.05f, 0.36f, 0.36f, 0.24f, 0f, 0f, 1.0f, 0.98f, 0.72f, 1f, 0.95f, 64f, ShaderProgram.MAT_STANDARD)
                    drawMesh(sphereMesh, o.customX + 0.60f, 1.08f, bodyZStart - 0.05f, 0.36f, 0.36f, 0.24f, 0f, 0f, 1.0f, 0.98f, 0.72f, 1f, 0.95f, 64f, ShaderProgram.MAT_STANDARD)
                    cubeMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                    drawMesh(cubeMesh, o.customX, 2.94f, bodyZStart - 0.04f, 0.85f, 0.20f, 0.10f, 0f, 0f, 1.0f, 0.82f, 0.12f, 1f, 0.88f, 16f, ShaderProgram.MAT_STANDARD)
                }
            }
            ObstacleType.TUNNEL_ARCH -> {
                cubeMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                drawMesh(cubeMesh, -5.2f, 3.6f, cz, 1.0f, 7.2f, len, 0f, 0f, 0.32f, 0.36f, 0.44f, 1f, 0f, 12f, ShaderProgram.MAT_GROUND_BALLAST)
                drawMesh(cubeMesh, 5.2f, 3.6f, cz, 1.0f, 7.2f, len, 0f, 0f, 0.32f, 0.36f, 0.44f, 1f, 0f, 12f, ShaderProgram.MAT_GROUND_BALLAST)
                drawMesh(cubeMesh, 0f, 6.2f, cz, 11.4f, 1.8f, len, 0f, 0f, 0.28f, 0.32f, 0.38f, 1f, 0f, 12f, ShaderProgram.MAT_GROUND_BALLAST)
                drawMesh(cubeMesh, 0f, 5.25f, cz, 2.2f, 0.14f, len * 0.8f, 0f, 0f, 1.0f, 0.92f, 0.55f, 1f, 0.85f, 16f, ShaderProgram.MAT_STANDARD)
            }
            ObstacleType.OVERHEAD_BRIDGE -> {
                cylinderMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                drawMesh(cylinderMesh, -5.1f, 3.1f, cz, 0.58f, 6.2f, 0.58f, 0f, 0f, 0.95f, 0.66f, 0.12f, 1f, 0f, 48f, ShaderProgram.MAT_METALLIC)
                drawMesh(cylinderMesh, 5.1f, 3.1f, cz, 0.58f, 6.2f, 0.58f, 0f, 0f, 0.95f, 0.66f, 0.12f, 1f, 0f, 48f, ShaderProgram.MAT_METALLIC)
                cubeMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                drawMesh(cubeMesh, 0f, 5.5f, cz, 11.2f, 1.3f, len, 0f, 0f, 0.22f, 0.28f, 0.38f, 1f, 0f, 48f, ShaderProgram.MAT_METALLIC)
                drawMesh(cubeMesh, 0f, 5.5f, o.zStart, 10.6f, 0.32f, 0.15f, 0f, 0f, 0.0f, 0.85f, 1.0f, 1f, 0.28f, 24f, ShaderProgram.MAT_STANDARD)
            }
            ObstacleType.SUPPORT_PILLAR -> {
                cylinderMesh.bind(shader.aPosLoc, shader.aNormalLoc)
                drawMesh(cylinderMesh, o.customX, 2.6f, cz, 0.30f, 5.2f, 0.30f, 0f, 0f, 0.38f, 0.44f, 0.54f, 1f, 0f, 48f, ShaderProgram.MAT_METALLIC)
                drawMesh(cylinderMesh, o.customX, 0.8f, cz, 0.36f, 0.7f, 0.36f, 0f, 0f, 1.0f, 0.84f, 0.0f, 1f, 0.12f, 24f, ShaderProgram.MAT_STANDARD)
            }
        }
    }

    private fun drawRunner() {
        val p = gameManager.player
        val skin = equippedCharacter
        val board = equippedBoard

        if (p.invulnerableTimer > 0f && ((p.invulnerableTimer * 16f).toInt() % 2 == 1)) {
            return
        }

        // Smooth circular ground shadow
        val shadowScale = (1f - (p.worldY - p.currentGroundY) * 0.12f).coerceIn(0.35f, 1f)
        cylinderMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(cylinderMesh, p.worldX, p.currentGroundY + 0.03f, p.worldZ, 1.05f * shadowScale, 0.03f, 1.05f * shadowScale, 0f, 0f, 0.06f, 0.08f, 0.12f, 0.50f, 0f, 8f, ShaderProgram.MAT_STANDARD)

        val x = p.worldX
        val y = p.worldY
        val z = p.worldZ

        val isJet = gameManager.powerUpManager.isJetpackActive
        val isBoard = gameManager.powerUpManager.isHoverboardActive
        val isSneakers = gameManager.powerUpManager.isSuperSneakersActive

        if (isBoard) {
            val bob = sin(p.runAnimTime * 5f) * 0.05f
            cubeMesh.bind(shader.aPosLoc, shader.aNormalLoc)
            drawMesh(cubeMesh, x, y + 0.15f + bob, z, 0.56f, 0.09f, 1.50f, 0f, 0f, board.deckRgb[0], board.deckRgb[1], board.deckRgb[2], 1f, 0.25f, 64f, ShaderProgram.MAT_METALLIC)
            sphereMesh.bind(shader.aPosLoc, shader.aNormalLoc)
            drawMesh(sphereMesh, x, y + 0.15f + bob, z + 0.72f, 0.56f, 0.09f, 0.34f, 0f, 0f, board.deckRgb[0], board.deckRgb[1], board.deckRgb[2], 1f, 0.25f, 64f, ShaderProgram.MAT_METALLIC)
            drawMesh(sphereMesh, x, y + 0.10f + bob, z - 0.65f, 0.42f, 0.14f, 0.26f, 0f, 0f, board.glowRgb[0], board.glowRgb[1], board.glowRgb[2], 0.95f, 0.90f, 16f, ShaderProgram.MAT_STANDARD)
        }

        if (p.isRolling) {
            sphereMesh.bind(shader.aPosLoc, shader.aNormalLoc)
            drawMesh(sphereMesh, x, y + 0.36f, z, 0.62f, 0.56f, 0.74f, 0f, 0f, skin.hoodieRgb[0], skin.hoodieRgb[1], skin.hoodieRgb[2], 1f, 0.05f, 24f, ShaderProgram.MAT_STANDARD)
            drawMesh(sphereMesh, x, y + 0.66f, z + 0.20f, 0.42f, 0.42f, 0.42f, 0f, 0f, 0.96f, 0.80f, 0.68f, 1f, 0f, 18f, ShaderProgram.MAT_STANDARD)
            drawMesh(sphereMesh, x, y + 0.82f, z + 0.22f, 0.45f, 0.18f, 0.46f, 0f, 0f, skin.accentRgb[0], skin.accentRgb[1], skin.accentRgb[2], 1f, 0.1f, 24f, ShaderProgram.MAT_STANDARD)
            return
        }

        val yOffset = if (isBoard) 0.22f else 0f
        val swing = if (gameManager.phase == GamePhase.MENU) {
            sin(menuOrbitTime * 2.2f) * 0.05f
        } else if (isJet || !p.isGrounded || isBoard) {
            0.10f
        } else {
            sin(p.runAnimTime * 11f) * 0.40f
        }

        val shoeR = if (isSneakers) 0.05f else skin.shoeRgb[0]
        val shoeG = if (isSneakers) 0.95f else skin.shoeRgb[1]
        val shoeB = if (isSneakers) 0.45f else skin.shoeRgb[2]
        val shoeEmissive = if (isSneakers) 0.6f else 0.05f

        // Smooth cylindrical legs + rounded sneakers
        cylinderMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(cylinderMesh, x - 0.16f, y + yOffset + 0.44f, z + swing * 0.45f, 0.22f, 0.66f, 0.22f, 0f, 0f, skin.pantsRgb[0], skin.pantsRgb[1], skin.pantsRgb[2], 1f, 0f, 14f, ShaderProgram.MAT_STANDARD)
        drawMesh(cylinderMesh, x + 0.16f, y + yOffset + 0.44f, z - swing * 0.45f, 0.22f, 0.66f, 0.22f, 0f, 0f, skin.pantsRgb[0], skin.pantsRgb[1], skin.pantsRgb[2], 1f, 0f, 14f, ShaderProgram.MAT_STANDARD)
        sphereMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(sphereMesh, x - 0.16f, y + yOffset + 0.12f, z + swing * 0.45f + 0.07f, 0.25f, 0.20f, 0.38f, 0f, 0f, shoeR, shoeG, shoeB, 1f, shoeEmissive, 24f, ShaderProgram.MAT_STANDARD)
        drawMesh(sphereMesh, x + 0.16f, y + yOffset + 0.12f, z - swing * 0.45f + 0.07f, 0.25f, 0.20f, 0.38f, 0f, 0f, shoeR, shoeG, shoeB, 1f, shoeEmissive, 24f, ShaderProgram.MAT_STANDARD)

        // Smooth cylindrical hoodie torso + shoulders
        cylinderMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(cylinderMesh, x, y + yOffset + 1.05f, z, 0.56f, 0.68f, 0.36f, 0f, 0f, skin.hoodieRgb[0], skin.hoodieRgb[1], skin.hoodieRgb[2], 1f, 0.05f, 18f, ShaderProgram.MAT_STANDARD)
        if (isJet) {
            drawMesh(cylinderMesh, x - 0.16f, y + yOffset + 1.04f, z - 0.26f, 0.22f, 0.62f, 0.22f, 0f, 0f, 0.98f, 0.42f, 0.08f, 1f, 0.22f, 64f, ShaderProgram.MAT_METALLIC)
            drawMesh(cylinderMesh, x + 0.16f, y + yOffset + 1.04f, z - 0.26f, 0.22f, 0.62f, 0.22f, 0f, 0f, 0.98f, 0.42f, 0.08f, 1f, 0.22f, 64f, ShaderProgram.MAT_METALLIC)
        } else {
            cubeMesh.bind(shader.aPosLoc, shader.aNormalLoc)
            drawMesh(cubeMesh, x, y + yOffset + 1.07f, z - 0.23f, 0.38f, 0.44f, 0.18f, 0f, 0f, skin.accentRgb[0], skin.accentRgb[1], skin.accentRgb[2], 1f, 0.04f, 16f, ShaderProgram.MAT_STANDARD)
        }

        // Rounded Arms + Hands
        cylinderMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(cylinderMesh, x - 0.37f, y + yOffset + 1.07f, z - swing * 0.42f, 0.18f, 0.54f, 0.18f, 0f, 0f, skin.hoodieRgb[0], skin.hoodieRgb[1], skin.hoodieRgb[2], 1f, 0.04f, 16f, ShaderProgram.MAT_STANDARD)
        drawMesh(cylinderMesh, x + 0.37f, y + yOffset + 1.07f, z + swing * 0.42f, 0.18f, 0.54f, 0.18f, 0f, 0f, skin.hoodieRgb[0], skin.hoodieRgb[1], skin.hoodieRgb[2], 1f, 0.04f, 16f, ShaderProgram.MAT_STANDARD)

        // Smooth Spherical Head & Contoured Cap
        sphereMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(sphereMesh, x, y + yOffset + 1.58f, z, 0.42f, 0.44f, 0.42f, 0f, 0f, 0.96f, 0.80f, 0.68f, 1f, 0f, 18f, ShaderProgram.MAT_STANDARD)
        drawMesh(sphereMesh, x, y + yOffset + 1.72f, z, 0.45f, 0.26f, 0.46f, 0f, 0f, skin.accentRgb[0], skin.accentRgb[1], skin.accentRgb[2], 1f, 0.08f, 24f, ShaderProgram.MAT_STANDARD)
        cubeMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(cubeMesh, x, y + yOffset + 1.69f, z + 0.25f, 0.36f, 0.05f, 0.22f, 0f, 0f, skin.accentRgb[0], skin.accentRgb[1], skin.accentRgb[2], 1f, 0.08f, 24f, ShaderProgram.MAT_STANDARD)
    }

    private fun drawGuardAndDog() {
        val p = gameManager.player
        val gz = p.worldZ + p.chaserZOffset
        val gx = p.worldX * 0.85f
        val swing = sin(p.runAnimTime * 11.5f) * 0.35f

        cylinderMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(cylinderMesh, gx, 0.03f, gz, 0.95f, 0.03f, 0.95f, 0f, 0f, 0.06f, 0.08f, 0.12f, 0.45f, 0f, 8f, ShaderProgram.MAT_STANDARD)
        drawMesh(cylinderMesh, gx - 0.18f, 0.45f, gz + swing * 0.4f, 0.24f, 0.72f, 0.24f, 0f, 0f, 0.14f, 0.18f, 0.32f, 1f, 0f, 12f, ShaderProgram.MAT_STANDARD)
        drawMesh(cylinderMesh, gx + 0.18f, 0.45f, gz - swing * 0.4f, 0.24f, 0.72f, 0.24f, 0f, 0f, 0.14f, 0.18f, 0.32f, 1f, 0f, 12f, ShaderProgram.MAT_STANDARD)
        drawMesh(cylinderMesh, gx, 1.15f, gz, 0.68f, 0.76f, 0.44f, 0f, 0f, 0.18f, 0.26f, 0.48f, 1f, 0f, 16f, ShaderProgram.MAT_STANDARD)
        drawMesh(cylinderMesh, gx, 1.18f, gz + 0.03f, 0.70f, 0.34f, 0.46f, 0f, 0f, 0.88f, 0.96f, 0.15f, 1f, 0.16f, 24f, ShaderProgram.MAT_STANDARD)
        sphereMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(sphereMesh, gx, 1.72f, gz, 0.44f, 0.44f, 0.44f, 0f, 0f, 0.95f, 0.76f, 0.65f, 1f, 0f, 16f, ShaderProgram.MAT_STANDARD)
        drawMesh(sphereMesh, gx, 1.88f, gz, 0.48f, 0.22f, 0.48f, 0f, 0f, 0.12f, 0.16f, 0.28f, 1f, 0f, 18f, ShaderProgram.MAT_STANDARD)

        // Pursuing Dog
        val dx = gx - 1.05f
        val dz = gz + 0.4f
        val hop = abs(sin(p.runAnimTime * 13f)) * 0.22f
        cylinderMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(cylinderMesh, dx, 0.03f, dz, 0.70f, 0.03f, 0.85f, 0f, 0f, 0.06f, 0.08f, 0.12f, 0.45f, 0f, 8f, ShaderProgram.MAT_STANDARD)
        drawMesh(cylinderMesh, dx - 0.13f, 0.18f + hop, dz + 0.26f, 0.11f, 0.34f, 0.11f, 0f, 0f, 0.52f, 0.32f, 0.16f, 1f, 0f, 10f, ShaderProgram.MAT_STANDARD)
        drawMesh(cylinderMesh, dx + 0.13f, 0.18f + hop, dz - 0.26f, 0.11f, 0.34f, 0.11f, 0f, 0f, 0.52f, 0.32f, 0.16f, 1f, 0f, 10f, ShaderProgram.MAT_STANDARD)
        sphereMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        drawMesh(sphereMesh, dx, 0.45f + hop, dz, 0.42f, 0.38f, 0.78f, 0f, 0f, 0.66f, 0.42f, 0.22f, 1f, 0f, 14f, ShaderProgram.MAT_STANDARD)
        drawMesh(sphereMesh, dx, 0.65f + hop, dz + 0.40f, 0.32f, 0.30f, 0.34f, 0f, 0f, 0.56f, 0.34f, 0.18f, 1f, 0f, 14f, ShaderProgram.MAT_STANDARD)
    }

    private fun drawCollectibles() {
        coinMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        for (col in gameManager.trackManager.collectibles) {
            if (!col.active || col.kind != CollectibleKind.COIN) continue
            val bob = sin(col.spinPhase) * 0.08f
            val angleDeg = (col.spinPhase * 57.3f) % 360f
            drawMesh(coinMesh, col.x, col.y + bob, col.z, 0.68f, 0.68f, 0.68f, angleDeg, 0f, 1.0f, 0.84f, 0.10f, 1f, 0.25f, 84f, ShaderProgram.MAT_METALLIC)
        }

        crystalMesh.bind(shader.aPosLoc, shader.aNormalLoc)
        for (col in gameManager.trackManager.collectibles) {
            if (!col.active || col.kind == CollectibleKind.COIN) continue
            val bob = sin(col.spinPhase * 1.4f) * 0.15f
            val angleDeg = (col.spinPhase * 75f) % 360f
            val (r, g, b) = when (col.kind) {
                CollectibleKind.JETPACK -> Triple(1.0f, 0.45f, 0.05f)
                CollectibleKind.SUPER_SNEAKERS -> Triple(0.05f, 0.95f, 0.45f)
                CollectibleKind.COIN_MAGNET -> Triple(1.0f, 0.22f, 0.34f)
                CollectibleKind.DOUBLE_MULTIPLIER -> Triple(1.0f, 0.88f, 0.08f)
                CollectibleKind.HOVERBOARD_PICKUP -> Triple(0.0f, 0.78f, 1.0f)
                CollectibleKind.KEY_PICKUP -> Triple(0.95f, 0.98f, 1.0f)
                else -> Triple(1f, 1f, 1f)
            }
            drawMesh(crystalMesh, col.x, col.y + 0.15f + bob, col.z, 0.85f, 0.95f, 0.85f, angleDeg, 0f, r, g, b, 1f, 0.55f, 64f, ShaderProgram.MAT_METALLIC)
        }
    }

    private fun drawMesh(
        mesh: MeshBuffer,
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        rotYDeg: Float,
        rotZDeg: Float,
        r: Float, g: Float, b: Float, a: Float,
        emissive: Float,
        shininess: Float,
        materialType: Int
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (rotYDeg != 0f) Matrix.rotateM(modelMatrix, 0, rotYDeg, 0f, 1f, 0f)
        if (rotZDeg != 0f) Matrix.rotateM(modelMatrix, 0, rotZDeg, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES30.glUniformMatrix4fv(shader.uMvpMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, modelMatrix, 0)
        GLES30.glUniform4f(shader.uColorLoc, r, g, b, a)
        GLES30.glUniform1f(shader.uEmissiveLoc, emissive)
        GLES30.glUniform1f(shader.uShininessLoc, shininess)
        GLES30.glUniform1i(shader.uMaterialTypeLoc, materialType)
        mesh.draw()
    }
}
