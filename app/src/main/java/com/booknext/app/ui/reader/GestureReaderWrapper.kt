package com.booknext.app.ui.reader

import android.content.Context
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.abs

/**
 * 应用9区域手势模型到阅读器
 * - 左侧 25% 点击: 上一页
 * - 右侧 25% 点击: 下一页
 * - 中间 50% 点击: 切换 UI 可见性
 * - 左边缘上下滑: 亮度调节
 * - 右边缘上下滑: 音量调节
 */
@Composable
fun Modifier.readerGestures(
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleUI: () -> Unit,
    edgeWidth: Dp = androidx.compose.ui.unit.Dp(40f),
): Modifier {
    val context = LocalContext.current
    val density = LocalDensity.current
    val edgePx = with(density) { edgeWidth.toPx() }

    return this.pointerInput(Unit) {
        detectTapGestures(
            onTap = { offset ->
                val w = size.width.toFloat()
                when {
                    offset.x < w * 0.25f -> onPrevPage()
                    offset.x > w * 0.75f -> onNextPage()
                    else -> onToggleUI()
                }
            }
        )
    }
    .pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragStart = { offset ->
                val w = size.width.toFloat()
                // Only activate edge drags
                if (offset.x < edgePx) {
                    _leftEdgeDrag = true
                } else if (offset.x > w - edgePx) {
                    _rightEdgeDrag = true
                }
            },
            onVerticalDrag = { _, dragAmount ->
                val w = size.width.toFloat()
                when {
                    _leftEdgeDrag -> adjustBrightness(context, dragAmount / size.height)
                    _rightEdgeDrag -> adjustVolume(context, dragAmount / size.height)
                }
            },
            onDragEnd = { _leftEdgeDrag = false; _rightEdgeDrag = false },
            onDragCancel = { _leftEdgeDrag = false; _rightEdgeDrag = false },
        )
    }
    .pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragEnd = {},
            onDragCancel = {},
            onHorizontalDrag = { _, _ -> },
            onDragStart = {},
        )
    }
}

private var _leftEdgeDrag by mutableStateOf(false)
private var _rightEdgeDrag by mutableStateOf(false)

fun adjustBrightness(context: Context, fraction: Float) {
    try {
        val window = (context as? android.app.Activity)?.window ?: return
        val attrs = window.attributes
        attrs.screenBrightness = (attrs.screenBrightness - fraction * 0.05f).coerceIn(0.01f, 1.0f)
        window.attributes = attrs
    } catch (_: Exception) {}
}

fun adjustVolume(context: Context, fraction: Float) {
    try {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val current = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val target = (current - (fraction * max * 0.05f).toInt()).coerceIn(0, max)
        am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, target, 0)
    } catch (_: Exception) {}
}