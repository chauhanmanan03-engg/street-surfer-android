package com.example.engine

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MeshBuffer(vertices: FloatArray) {
    val vertexCount: Int = vertices.size / 6
    private val vboId: Int

    init {
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        vboId = ids[0]

        val buf: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        buf.position(0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buf, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun bind(posLoc: Int, normalLoc: Int) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glEnableVertexAttribArray(posLoc)
        GLES30.glVertexAttribPointer(posLoc, 3, GLES30.GL_FLOAT, false, 24, 0)
        GLES30.glEnableVertexAttribArray(normalLoc)
        GLES30.glVertexAttribPointer(normalLoc, 3, GLES30.GL_FLOAT, false, 24, 12)
    }

    fun draw() {
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
    }
}

object MeshBuilder {
    fun createUnitCube(): MeshBuffer {
        val v = floatArrayOf(
            // Front (+Z)
            -0.5f, -0.5f, 0.5f, 0f, 0f, 1f,
            0.5f, -0.5f, 0.5f, 0f, 0f, 1f,
            0.5f, 0.5f, 0.5f, 0f, 0f, 1f,
            -0.5f, -0.5f, 0.5f, 0f, 0f, 1f,
            0.5f, 0.5f, 0.5f, 0f, 0f, 1f,
            -0.5f, 0.5f, 0.5f, 0f, 0f, 1f,
            // Back (-Z)
            0.5f, -0.5f, -0.5f, 0f, 0f, -1f,
            -0.5f, -0.5f, -0.5f, 0f, 0f, -1f,
            -0.5f, 0.5f, -0.5f, 0f, 0f, -1f,
            0.5f, -0.5f, -0.5f, 0f, 0f, -1f,
            -0.5f, 0.5f, -0.5f, 0f, 0f, -1f,
            0.5f, 0.5f, -0.5f, 0f, 0f, -1f,
            // Left (-X)
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,
            -0.5f, -0.5f, 0.5f, -1f, 0f, 0f,
            -0.5f, 0.5f, 0.5f, -1f, 0f, 0f,
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,
            -0.5f, 0.5f, 0.5f, -1f, 0f, 0f,
            -0.5f, 0.5f, -0.5f, -1f, 0f, 0f,
            // Right (+X)
            0.5f, -0.5f, 0.5f, 1f, 0f, 0f,
            0.5f, -0.5f, -0.5f, 1f, 0f, 0f,
            0.5f, 0.5f, -0.5f, 1f, 0f, 0f,
            0.5f, -0.5f, 0.5f, 1f, 0f, 0f,
            0.5f, 0.5f, -0.5f, 1f, 0f, 0f,
            0.5f, 0.5f, 0.5f, 1f, 0f, 0f,
            // Top (+Y)
            -0.5f, 0.5f, 0.5f, 0f, 1f, 0f,
            0.5f, 0.5f, 0.5f, 0f, 1f, 0f,
            0.5f, 0.5f, -0.5f, 0f, 1f, 0f,
            -0.5f, 0.5f, 0.5f, 0f, 1f, 0f,
            0.5f, 0.5f, -0.5f, 0f, 1f, 0f,
            -0.5f, 0.5f, -0.5f, 0f, 1f, 0f,
            // Bottom (-Y)
            -0.5f, -0.5f, -0.5f, 0f, -1f, 0f,
            0.5f, -0.5f, -0.5f, 0f, -1f, 0f,
            0.5f, -0.5f, 0.5f, 0f, -1f, 0f,
            -0.5f, -0.5f, -0.5f, 0f, -1f, 0f,
            0.5f, -0.5f, 0.5f, 0f, -1f, 0f,
            -0.5f, -0.5f, 0.5f, 0f, -1f, 0f
        )
        return MeshBuffer(v)
    }

    /**
     * Smooth UV Sphere (radius 0.5) with continuous vertex normals for heads, headlights, foliage, and joints.
     */
    fun createSmoothSphere(stacks: Int = 10, slices: Int = 16): MeshBuffer {
        val list = ArrayList<Float>(stacks * slices * 6 * 6)
        val r = 0.5f
        for (i in 0 until stacks) {
            val phi0 = (PI * i / stacks).toFloat()
            val phi1 = (PI * (i + 1) / stacks).toFloat()
            for (j in 0 until slices) {
                val theta0 = (2.0 * PI * j / slices).toFloat()
                val theta1 = (2.0 * PI * (j + 1) / slices).toFloat()

                val p00 = spherePt(r, phi0, theta0)
                val p10 = spherePt(r, phi1, theta0)
                val p11 = spherePt(r, phi1, theta1)
                val p01 = spherePt(r, phi0, theta1)

                addSphereVert(list, p00)
                addSphereVert(list, p10)
                addSphereVert(list, p11)

                addSphereVert(list, p00)
                addSphereVert(list, p11)
                addSphereVert(list, p01)
            }
        }
        return MeshBuffer(list.toFloatArray())
    }

    /**
     * Smooth vertical cylinder (height 1.0 along Y, radius 0.5 in XZ) for limbs, pillars, wheels, jetpack tanks.
     */
    fun createSmoothCylinder(segments: Int = 16): MeshBuffer {
        val list = ArrayList<Float>(segments * 12 * 6)
        val r = 0.5f
        val hy = 0.5f
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val x0 = cos(a0) * r
            val z0 = sin(a0) * r
            val x1 = cos(a1) * r
            val z1 = sin(a1) * r
            val nx0 = cos(a0); val nz0 = sin(a0)
            val nx1 = cos(a1); val nz1 = sin(a1)

            // Top cap (+Y)
            addVert(list, 0f, hy, 0f, 0f, 1f, 0f)
            addVert(list, x1, hy, z1, 0f, 1f, 0f)
            addVert(list, x0, hy, z0, 0f, 1f, 0f)

            // Bottom cap (-Y)
            addVert(list, 0f, -hy, 0f, 0f, -1f, 0f)
            addVert(list, x0, -hy, z0, 0f, -1f, 0f)
            addVert(list, x1, -hy, z1, 0f, -1f, 0f)

            // Smooth side wall
            addVert(list, x0, -hy, z0, nx0, 0f, nz0)
            addVert(list, x0, hy, z0, nx0, 0f, nz0)
            addVert(list, x1, hy, z1, nx1, 0f, nz1)

            addVert(list, x0, -hy, z0, nx0, 0f, nz0)
            addVert(list, x1, hy, z1, nx1, 0f, nz1)
            addVert(list, x1, -hy, z1, nx1, 0f, nz1)
        }
        return MeshBuffer(list.toFloatArray())
    }

    fun createRampWedge(): MeshBuffer {
        val slopeNy = 0.7071f
        val slopeNz = -0.7071f
        val v = floatArrayOf(
            // Sloped top
            -0.5f, -0.5f, -0.5f, 0f, slopeNy, slopeNz,
            0.5f, -0.5f, -0.5f, 0f, slopeNy, slopeNz,
            0.5f, 0.5f, 0.5f, 0f, slopeNy, slopeNz,
            -0.5f, -0.5f, -0.5f, 0f, slopeNy, slopeNz,
            0.5f, 0.5f, 0.5f, 0f, slopeNy, slopeNz,
            -0.5f, 0.5f, 0.5f, 0f, slopeNy, slopeNz,
            // Back vertical face (+Z)
            -0.5f, -0.5f, 0.5f, 0f, 0f, 1f,
            0.5f, -0.5f, 0.5f, 0f, 0f, 1f,
            0.5f, 0.5f, 0.5f, 0f, 0f, 1f,
            -0.5f, -0.5f, 0.5f, 0f, 0f, 1f,
            0.5f, 0.5f, 0.5f, 0f, 0f, 1f,
            -0.5f, 0.5f, 0.5f, 0f, 0f, 1f,
            // Left triangle (-X)
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,
            -0.5f, -0.5f, 0.5f, -1f, 0f, 0f,
            -0.5f, 0.5f, 0.5f, -1f, 0f, 0f,
            // Right triangle (+X)
            0.5f, -0.5f, 0.5f, 1f, 0f, 0f,
            0.5f, -0.5f, -0.5f, 1f, 0f, 0f,
            0.5f, 0.5f, 0.5f, 1f, 0f, 0f
        )
        return MeshBuffer(v)
    }

    fun createCoinDisc(segments: Int = 24): MeshBuffer {
        val list = ArrayList<Float>(segments * 12 * 6)
        val r = 0.5f
        val hz = 0.14f
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val x0 = cos(a0) * r
            val y0 = sin(a0) * r
            val x1 = cos(a1) * r
            val y1 = sin(a1) * r
            val nx0 = cos(a0); val ny0 = sin(a0)
            val nx1 = cos(a1); val ny1 = sin(a1)

            // Front cap (+Z)
            addVert(list, 0f, 0f, hz, 0f, 0f, 1f)
            addVert(list, x0, y0, hz, 0f, 0f, 1f)
            addVert(list, x1, y1, hz, 0f, 0f, 1f)

            // Back cap (-Z)
            addVert(list, 0f, 0f, -hz, 0f, 0f, -1f)
            addVert(list, x1, y1, -hz, 0f, 0f, -1f)
            addVert(list, x0, y0, -hz, 0f, 0f, -1f)

            // Smooth side rim
            addVert(list, x0, y0, -hz, nx0, ny0, 0f)
            addVert(list, x1, y1, -hz, nx1, ny1, 0f)
            addVert(list, x1, y1, hz, nx1, ny1, 0f)

            addVert(list, x0, y0, -hz, nx0, ny0, 0f)
            addVert(list, x1, y1, hz, nx1, ny1, 0f)
            addVert(list, x0, y0, hz, nx0, ny0, 0f)
        }
        return MeshBuffer(list.toFloatArray())
    }

    fun createDiamondCrystal(): MeshBuffer {
        val list = ArrayList<Float>(8 * 3 * 6)
        val pts = arrayOf(
            floatArrayOf(0.5f, 0f, 0f),
            floatArrayOf(0f, 0f, 0.5f),
            floatArrayOf(-0.5f, 0f, 0f),
            floatArrayOf(0f, 0f, -0.5f)
        )
        for (i in 0 until 4) {
            val p0 = pts[i]
            val p1 = pts[(i + 1) % 4]
            val nx = (p0[0] + p1[0])
            val nz = (p0[2] + p1[2])
            addVert(list, 0f, 0.65f, 0f, nx, 0.5f, nz)
            addVert(list, p0[0], 0f, p0[2], nx, 0.5f, nz)
            addVert(list, p1[0], 0f, p1[2], nx, 0.5f, nz)

            addVert(list, 0f, -0.65f, 0f, nx, -0.5f, nz)
            addVert(list, p1[0], 0f, p1[2], nx, -0.5f, nz)
            addVert(list, p0[0], 0f, p0[2], nx, -0.5f, nz)
        }
        return MeshBuffer(list.toFloatArray())
    }

    private fun spherePt(r: Float, phi: Float, theta: Float): FloatArray {
        val sinPhi = sin(phi)
        return floatArrayOf(
            r * sinPhi * cos(theta),
            r * cos(phi),
            r * sinPhi * sin(theta)
        )
    }

    private fun addSphereVert(list: MutableList<Float>, pt: FloatArray) {
        val len = 0.5f
        addVert(list, pt[0], pt[1], pt[2], pt[0] / len, pt[1] / len, pt[2] / len)
    }

    private fun addVert(list: MutableList<Float>, x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) {
        list.add(x); list.add(y); list.add(z)
        list.add(nx); list.add(ny); list.add(nz)
    }
}
