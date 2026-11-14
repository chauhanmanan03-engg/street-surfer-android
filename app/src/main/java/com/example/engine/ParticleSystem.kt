package com.example.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PooledParticle {
    var active: Boolean = false
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var vz: Float = 0f
    var life: Float = 0f
    var maxLife: Float = 0.5f
    var size: Float = 0.14f
    var r: Float = 1f
    var g: Float = 1f
    var b: Float = 1f
}

class ParticleSystem {
    val particles = Array(220) { PooledParticle() }
    private val random = Random(99)

    fun emitDust(x: Float, y: Float, z: Float) {
        for (i in 0 until 4) {
            spawn()?.apply {
                this.x = x + (random.nextFloat() - 0.5f) * 0.4f
                this.y = y + random.nextFloat() * 0.15f
                this.z = z
                vx = (random.nextFloat() - 0.5f) * 1.6f
                vy = 1.2f + random.nextFloat() * 1.4f
                vz = -1.5f - random.nextFloat() * 1.5f
                maxLife = 0.34f
                life = maxLife
                size = 0.16f
                r = 0.90f; g = 0.86f; b = 0.78f
            }
        }
    }

    fun emitCoinSparkle(x: Float, y: Float, z: Float) {
        for (i in 0 until 7) {
            val angle = i * (6.283f / 7f)
            spawn()?.apply {
                this.x = x
                this.y = y
                this.z = z
                vx = cos(angle) * 3.5f
                vy = 2.0f + random.nextFloat() * 2.5f
                vz = sin(angle) * 3.5f
                maxLife = 0.40f
                life = maxLife
                size = 0.13f
                r = 1.0f; g = 0.88f; b = 0.12f
            }
        }
    }

    fun emitPowerUpBurst(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float) {
        for (i in 0 until 14) {
            val angle = i * (6.283f / 14f)
            spawn()?.apply {
                this.x = x
                this.y = y
                this.z = z
                vx = cos(angle) * 5.2f
                vy = (random.nextFloat() - 0.2f) * 4.5f
                vz = sin(angle) * 5.2f
                maxLife = 0.55f
                life = maxLife
                size = 0.18f
                this.r = r; this.g = g; this.b = b
            }
        }
    }

    fun emitHoverboardShockwave(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float) {
        for (i in 0 until 28) {
            val angle = i * (6.283f / 28f)
            spawn()?.apply {
                this.x = x
                this.y = y
                this.z = z
                vx = cos(angle) * 11.5f
                vy = 2.0f + random.nextFloat() * 5.0f
                vz = sin(angle) * 11.5f
                maxLife = 0.65f
                life = maxLife
                size = 0.28f
                this.r = r; this.g = g; this.b = b
            }
        }
    }

    fun emitTrail(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float) {
        spawn()?.apply {
            this.x = x + (random.nextFloat() - 0.5f) * 0.35f
            this.y = y + (random.nextFloat() - 0.5f) * 0.12f
            this.z = z
            vx = (random.nextFloat() - 0.5f) * 0.8f
            vy = 0.4f + random.nextFloat() * 0.8f
            vz = -4.0f
            maxLife = 0.28f
            life = maxLife
            size = 0.15f
            this.r = r; this.g = g; this.b = b
        }
    }

    fun update(dt: Float) {
        for (p in particles) {
            if (!p.active) continue
            p.life -= dt
            if (p.life <= 0f) {
                p.active = false
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            p.vy -= 5.0f * dt
        }
    }

    private fun spawn(): PooledParticle? {
        for (p in particles) {
            if (!p.active) {
                p.active = true
                return p
            }
        }
        return null
    }
}
