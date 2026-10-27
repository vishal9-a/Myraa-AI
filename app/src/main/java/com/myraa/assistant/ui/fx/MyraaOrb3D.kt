package com.myraa.assistant.ui.fx

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myraa.assistant.ui.components.NovaOrb
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.LocalNovaReducedMotion
import com.myraa.assistant.voice.NovaOrbState

private const val AGSL_SPHERE_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iAmplitude;
    uniform float2 iTilt;
    uniform float4 iColorCore;
    uniform float4 iColorRim;
    uniform float4 iColorHalo;

    // Simple pseudo-random hash
    float hash(float2 p) {
        return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
    }

    // 2D noise
    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return lerp(lerp(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
                    lerp(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
    }

    // FBM
    float fbm(float2 p) {
        float v = 0.0;
        float a = 0.5;
        for (int i = 0; i < 3; i++) {
            v += a * noise(p);
            p = p * 2.0;
            a *= 0.5;
        }
        return v;
    }

    half4 main(float2 fragCoord) {
        float2 uv = (fragCoord - 0.5 * iResolution) / min(iResolution.x, iResolution.y);
        float r = length(uv);

        // Displace radius with voice amplitude
        float sphereRadius = 0.36 + (iAmplitude * 0.06);

        // Outside sphere: soft atmospheric halo
        if (r > sphereRadius) {
            float dist = r - sphereRadius;
            float halo = exp(-dist * 18.0) * (0.8 + iAmplitude * 0.8);
            return half4(iColorHalo.rgb * halo, halo * iColorHalo.a);
        }

        // Inside sphere: 3D normal calculation
        float z = sqrt(max(0.0, sphereRadius * sphereRadius - uv.x * uv.x - uv.y * uv.y));
        float3 N = normalize(float3(uv.x, uv.y, z));

        // Light direction tilting with phone accelerometer
        float3 L = normalize(float3(0.3 + iTilt.x * 0.5, -0.5 - iTilt.y * 0.5, 0.8));

        // Diffuse and specular lighting
        float diff = max(0.0, dot(N, L));
        float3 V = float3(0.0, 0.0, 1.0);
        float3 H = normalize(L + V);
        float spec = pow(max(0.0, dot(N, H)), 24.0);

        // Fresnel rim light
        float fresnel = pow(1.0 - max(0.0, dot(N, V)), 2.8);

        // Internal energy field rotation
        float2 rotUV = float2(
            uv.x * cos(iTime * 0.4) - uv.y * sin(iTime * 0.4),
            uv.x * sin(iTime * 0.4) + uv.y * cos(iTime * 0.4)
        );
        float energy = fbm(rotUV * 6.0 + iTime * 0.6);

        // Color blending
        float3 col = lerp(iColorCore.rgb, iColorRim.rgb, fresnel * 0.8 + energy * 0.3);
        col = col * (0.4 + 0.6 * diff) + (spec * 0.9);
        col += iColorRim.rgb * fresnel * 0.9;
        col += float3(iAmplitude * 0.3);

        return half4(col, 1.0);
    }
"""

@Composable
fun MyraaOrb3D(
    state: NovaOrbState,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    amplitude: Float = 0f,
    assistantName: String = "Myraa"
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // Fallback for Android < 13
        NovaOrb(
            state = state,
            modifier = modifier,
            size = size,
            amplitude = amplitude,
            assistantName = assistantName
        )
        return
    }

    val context = LocalContext.current
    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current

    // Tilt sensor listener
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && !reducedMotion) {
                    val ax = event.values[0] / 9.8f
                    val ay = event.values[1] / 9.8f
                    tiltX = tiltX * 0.85f + (-ax) * 0.15f
                    tiltY = tiltY * 0.85f + ay * 0.15f
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        accel?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose {
            sm?.unregisterListener(listener)
        }
    }

    val transition = rememberInfiniteTransition(label = "ShaderTime")
    val animTime by transition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(100_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShaderClock"
    )

    // Palette per state
    val (coreCol, rimCol, haloCol) = when (state) {
        NovaOrbState.IDLE -> Triple(colors.lotus, colors.lagoon, Color(0x66FF6FAE))
        NovaOrbState.LISTENING -> Triple(colors.lagoon, Color(0xFF67E8F9), Color(0x993FE0D0))
        NovaOrbState.THINKING -> Triple(colors.accent, colors.lotus, Color(0x99FFB547))
        NovaOrbState.SPEAKING -> Triple(colors.accent, colors.accentSecondary, Color(0xAAFF8A3D))
        NovaOrbState.CONNECTING -> Triple(Color(0xFFFBBF24), colors.accent, Color(0x80FBBF24))
        NovaOrbState.ERROR -> Triple(colors.error, Color(0xFFB91C1C), Color(0x66FF5C6C))
    }

    val shader = remember {
        try {
            RuntimeShader(AGSL_SPHERE_SHADER)
        } catch (_: Exception) {
            null
        }
    }

    if (shader == null) {
        NovaOrb(
            state = state,
            modifier = modifier,
            size = size,
            amplitude = amplitude,
            assistantName = assistantName
        )
        return
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val time = if (reducedMotion) 0f else animTime
                val px = this.size.width
                val py = this.size.height

                shader.setFloatUniform("iResolution", px, py)
                shader.setFloatUniform("iTime", time)
                shader.setFloatUniform("iAmplitude", amplitude)
                shader.setFloatUniform("iTilt", if (reducedMotion) 0f else tiltX, if (reducedMotion) 0f else tiltY)

                shader.setColorUniform("iColorCore", coreCol.value.toLong())
                shader.setColorUniform("iColorRim", rimCol.value.toLong())
                shader.setColorUniform("iColorHalo", haloCol.value.toLong())

                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "dummy").asComposeRenderEffect()
            }
    ) {
        drawRect(Color.White)
    }
}
