package com.example.engine

import android.opengl.GLES30

class ShaderProgram {
    val programId: Int
    val aPosLoc: Int
    val aNormalLoc: Int
    val uMvpMatrixLoc: Int
    val uModelMatrixLoc: Int
    val uColorLoc: Int
    val uEmissiveLoc: Int
    val uViewPosLoc: Int
    val uShininessLoc: Int
    val uMaterialTypeLoc: Int

    init {
        val vsId = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fsId = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vsId)
        GLES30.glAttachShader(programId, fsId)
        GLES30.glLinkProgram(programId)
        GLES30.glDeleteShader(vsId)
        GLES30.glDeleteShader(fsId)

        aPosLoc = GLES30.glGetAttribLocation(programId, "aPos")
        aNormalLoc = GLES30.glGetAttribLocation(programId, "aNormal")
        uMvpMatrixLoc = GLES30.glGetUniformLocation(programId, "uMvpMatrix")
        uModelMatrixLoc = GLES30.glGetUniformLocation(programId, "uModelMatrix")
        uColorLoc = GLES30.glGetUniformLocation(programId, "uColor")
        uEmissiveLoc = GLES30.glGetUniformLocation(programId, "uEmissive")
        uViewPosLoc = GLES30.glGetUniformLocation(programId, "uViewPos")
        uShininessLoc = GLES30.glGetUniformLocation(programId, "uShininess")
        uMaterialTypeLoc = GLES30.glGetUniformLocation(programId, "uMaterialType")
    }

    fun use() {
        GLES30.glUseProgram(programId)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        return shader
    }

    companion object {
        const val MAT_STANDARD = 0
        const val MAT_GROUND_BALLAST = 1
        const val MAT_BUILDING_WINDOWS = 2
        const val MAT_METALLIC = 3
        const val MAT_SKY_BACKDROP = 4

        private const val VERTEX_SHADER = """#version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aNormal;

            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;

            out vec3 vNormal;
            out vec3 vWorldPos;
            out float vViewDist;

            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = mat3(uModelMatrix) * aNormal;
                vec4 clipPos = uMvpMatrix * vec4(aPos, 1.0);
                vViewDist = clipPos.w;
                gl_Position = clipPos;
            }
        """

        private const val FRAGMENT_SHADER = """#version 300 es
            precision highp float;

            in vec3 vNormal;
            in vec3 vWorldPos;
            in float vViewDist;

            uniform vec4 uColor;
            uniform float uEmissive;
            uniform vec3 uViewPos;
            uniform float uShininess;
            uniform int uMaterialType;

            out vec4 fragColor;

            float hash21(vec2 p) {
                p = fract(p * vec2(123.34, 456.21));
                p += dot(p, p + 45.32);
                return fract(p.x * p.y);
            }

            void main() {
                if (uMaterialType == 4) {
                    // Sunlit sky dome gradient
                    float h = clamp((vWorldPos.y + 4.0) / 55.0, 0.0, 1.0);
                    vec3 horizonCol = vec3(0.72, 0.91, 1.00);
                    vec3 midSkyCol = vec3(0.32, 0.72, 0.98);
                    vec3 zenithCol = vec3(0.11, 0.44, 0.86);
                    vec3 sky = mix(horizonCol, midSkyCol, smoothstep(0.0, 0.42, h));
                    sky = mix(sky, zenithCol, smoothstep(0.42, 1.0, h));
                    fragColor = vec4(sky, 1.0);
                    return;
                }

                vec3 n = normalize(vNormal);
                vec3 v = normalize(uViewPos - vWorldPos);
                vec3 sunDir = normalize(vec3(-0.42, 0.86, -0.32));
                vec3 halfVec = normalize(sunDir + v);

                vec3 baseColor = uColor.rgb;
                float specStrength = 0.22;
                float extraEmissive = uEmissive;

                if (uMaterialType == 1) {
                    // Procedural gravel/ballast micro-detail on rail-yard ground
                    float grain = hash21(floor(vWorldPos.xz * 4.5));
                    baseColor *= (0.86 + grain * 0.26);
                    specStrength = 0.06;
                } else if (uMaterialType == 2) {
                    // Procedural architectural sunlit windows on city buildings
                    if (abs(n.y) < 0.5) {
                        float wx = fract((vWorldPos.x + vWorldPos.z) * 0.55);
                        float wy = fract(vWorldPos.y * 0.48);
                        if (wx > 0.18 && wx < 0.82 && wy > 0.22 && wy < 0.80 && vWorldPos.y > 1.2) {
                            float glassTint = hash21(floor(vec2(vWorldPos.x + vWorldPos.z, vWorldPos.y * 0.5)));
                            baseColor = mix(vec3(0.16, 0.34, 0.52), vec3(0.70, 0.88, 0.98), glassTint * 0.65);
                            specStrength = 0.85;
                        }
                    }
                } else if (uMaterialType == 3) {
                    // Metallic rails, coins, & train trim
                    specStrength = 0.90;
                }

                float sunDiff = max(dot(n, sunDir), 0.0);
                float fillDiff = max(dot(n, normalize(vec3(0.6, 0.3, 0.7))), 0.0) * 0.28;

                // Hemisphere sky + warm asphalt bounce
                float hemi = n.y * 0.5 + 0.5;
                vec3 skyAmbient = vec3(0.50, 0.66, 0.84);
                vec3 groundAmbient = vec3(0.30, 0.27, 0.25);
                vec3 ambient = mix(groundAmbient, skyAmbient, hemi);

                // Blinn-Phong specular highlight
                float spec = pow(max(dot(n, halfVec), 0.0), max(uShininess, 8.0)) * specStrength;

                // Subtle sunlit Fresnel rim light for crisp 3D silhouette separation
                float rim = pow(1.0 - max(dot(n, v), 0.0), 3.0) * 0.16;

                vec3 lit = baseColor * (ambient * 0.68 + vec3(1.0, 0.96, 0.88) * (sunDiff * 0.84 + fillDiff))
                         + vec3(1.0, 0.97, 0.90) * spec
                         + vec3(0.65, 0.88, 1.0) * rim
                         + baseColor * extraEmissive;

                // Smooth distance atmospheric sunlit haze
                float fogFactor = clamp((vViewDist - 85.0) / 95.0, 0.0, 1.0);
                vec3 fogColor = vec3(0.70, 0.90, 0.99);
                vec3 finalRgb = mix(lit, fogColor, fogFactor);

                fragColor = vec4(finalRgb, uColor.a);
            }
        """
    }
}
