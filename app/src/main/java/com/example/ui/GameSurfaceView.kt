package com.example.ui

import android.opengl.GLSurfaceView
import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.engine.StreetSurferRenderer
import com.example.game.GamePhase
import com.example.game.InputAction
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import kotlin.math.abs

private class MsaaConfigChooser : GLSurfaceView.EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        val msaaSpec = intArrayOf(
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16,
            EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT
            EGL10.EGL_SAMPLE_BUFFERS, 1,
            EGL10.EGL_SAMPLES, 4,
            EGL10.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfig = IntArray(1)
        if (egl.eglChooseConfig(display, msaaSpec, configs, 1, numConfig) && numConfig[0] > 0 && configs[0] != null) {
            return configs[0]!!
        }
        val fallbackSpec = intArrayOf(
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16,
            EGL10.EGL_RENDERABLE_TYPE, 4,
            EGL10.EGL_NONE
        )
        egl.eglChooseConfig(display, fallbackSpec, configs, 1, numConfig)
        return configs[0]!!
    }
}

@Composable
fun GameSurfaceView(
    renderer: StreetSurferRenderer,
    phase: GamePhase,
    onInput: (InputAction) -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusRequester = remember { FocusRequester() }
    val swipeThresholdPx = with(LocalDensity.current) { 24.dp.toPx() }
    var lastTapMs by remember { mutableLongStateOf(0L) }
    var glViewRef: GLSurfaceView? = remember { null }

    LaunchedEffect(phase) {
        focusRequester.requestFocus()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> glViewRef?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown || keyEvent.nativeKeyEvent.repeatCount > 0) {
                    return@onPreviewKeyEvent false
                }
                when (keyEvent.key) {
                    Key.A, Key.DirectionLeft -> {
                        onInput(InputAction.SwipeLeft); true
                    }
                    Key.D, Key.DirectionRight -> {
                        onInput(InputAction.SwipeRight); true
                    }
                    Key.W, Key.DirectionUp -> {
                        onInput(InputAction.SwipeUp); true
                    }
                    Key.S, Key.DirectionDown -> {
                        onInput(InputAction.SwipeDown); true
                    }
                    Key.Spacebar -> {
                        onInput(InputAction.DoubleTap); true
                    }
                    Key.P, Key.Escape -> {
                        if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_P ||
                            keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ESCAPE
                        ) {
                            onTogglePause(); true
                        } else false
                    }
                    else -> false
                }
            }
            .pointerInput(phase, swipeThresholdPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalDx = 0f
                    var totalDy = 0f
                    var swipeFired = false
                    var lastPos = down.position

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (!swipeFired) {
                                val now = System.currentTimeMillis()
                                if (now - lastTapMs <= 280L) {
                                    onInput(InputAction.DoubleTap)
                                    lastTapMs = 0L
                                } else {
                                    lastTapMs = now
                                }
                            }
                            break
                        }

                        val delta = change.position - lastPos
                        lastPos = change.position
                        totalDx += delta.x
                        totalDy += delta.y

                        if (!swipeFired) {
                            if (abs(totalDx) >= swipeThresholdPx || abs(totalDy) >= swipeThresholdPx) {
                                swipeFired = true
                                if (abs(totalDx) > abs(totalDy)) {
                                    onInput(if (totalDx > 0f) InputAction.SwipeRight else InputAction.SwipeLeft)
                                } else {
                                    onInput(if (totalDy < 0f) InputAction.SwipeUp else InputAction.SwipeDown)
                                }
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(3)
                    setEGLConfigChooser(MsaaConfigChooser())
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    glViewRef = this
                }
            }
        )
    }
}
