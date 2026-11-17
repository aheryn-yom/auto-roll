package com.yom.autoroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.content.res.Configuration
import android.view.accessibility.AccessibilityEvent
import com.yom.autoroll.scroll.GestureExecutor
import com.yom.autoroll.scroll.ScrollController
import com.yom.autoroll.ui.FloatingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AutoScrollAccessibilityService : AccessibilityService() {
    private lateinit var scope: CoroutineScope
    private lateinit var controller: ScrollController
    private lateinit var floatingController: FloatingController

    override fun onServiceConnected() {
        super.onServiceConnected()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        controller = ScrollController(
            executor = GestureExecutor(this),
            scope = scope,
            screenSize = ::usableScreenSize,
            onStopped = { error -> floatingController.onScrollStopped(error) },
        )
        floatingController = FloatingController(this, controller)
        floatingController.show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        if (::controller.isInitialized) controller.stop()
        if (::floatingController.isInitialized) floatingController.onScrollStopped(null)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::floatingController.isInitialized) floatingController.adjustForCurrentScreen()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (::floatingController.isInitialized) floatingController.remove()
        if (::controller.isInitialized) controller.dispose()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (::floatingController.isInitialized) floatingController.remove()
        if (::controller.isInitialized) controller.dispose()
        super.onDestroy()
    }

    private fun usableScreenSize(): Pair<Int, Int> {
        val bounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getSystemService(android.view.WindowManager::class.java).currentWindowMetrics.bounds
        } else {
            @Suppress("DEPRECATION")
            Rect().also { getSystemService(android.view.WindowManager::class.java).defaultDisplay.getRectSize(it) }
        }
        return bounds.width() to bounds.height()
    }
}
