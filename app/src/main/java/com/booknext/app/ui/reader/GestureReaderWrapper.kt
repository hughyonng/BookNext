package com.booknext.app.ui.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun Modifier.readerGestures(
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleUI: () -> Unit,
): Modifier {
    return this.pointerInput(Unit) {
        detectTapGestures { offset ->
            val w = size.width.toFloat()
            when {
                offset.x < w * 0.25f -> onPrevPage()
                offset.x > w * 0.75f -> onNextPage()
                else -> onToggleUI()
            }
        }
    }
}
