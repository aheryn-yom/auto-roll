package com.yom.autoroll.scroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GestureExecutor(private val service: AccessibilityService) {
    suspend fun swipe(x: Float, fromY: Float, toY: Float, durationMs: Long): Boolean =
        suspendCancellableCoroutine { continuation ->
            val path = Path().apply {
                moveTo(x, fromY)
                lineTo(x, toY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
                .build()
            val callback = object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    if (continuation.isActive) continuation.resume(false)
                }
            }

            val accepted = service.dispatchGesture(gesture, callback, null)
            if (!accepted && continuation.isActive) continuation.resume(false)
        }
}
