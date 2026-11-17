package com.yom.autoroll.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.yom.autoroll.scroll.ScrollConfig
import com.yom.autoroll.scroll.ScrollController
import com.yom.autoroll.scroll.ScrollMode
import com.yom.autoroll.scroll.ScrollSpeed
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale

class FloatingController(
    private val service: AccessibilityService,
    private val scrollController: ScrollController,
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val preferences = service.getSharedPreferences("controller", 0)
    private var mode = enumValueOrDefault(preferences.getString(KEY_MODE, null), ScrollMode.STEP)
    private var rate = loadRate()
    private var expanded = false
    private var attached = false
    private lateinit var speedLabels: List<TextView>
    private lateinit var rateValueLabel: TextView
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.END
        y = dp(preferences.getInt(KEY_Y_DP, 260))
    }
    private val root = LinearLayout(service).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val menu = createMenu()
    private val dot = createDot()

    fun show() {
        if (attached) return
        adjustForCurrentScreen()
        render()
        windowManager.addView(root, params)
        attached = true
    }

    fun remove() {
        if (!attached) return
        scrollController.stop()
        windowManager.removeView(root)
        attached = false
    }

    fun adjustForCurrentScreen() {
        val height = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            service.resources.displayMetrics.heightPixels
        }
        params.y = params.y.coerceIn(dp(24), (height - dp(72)).coerceAtLeast(dp(24)))
        if (attached) windowManager.updateViewLayout(root, params)
    }

    fun onScrollStopped(error: String?) {
        renderDot()
        if (error != null) Toast.makeText(service, error, Toast.LENGTH_SHORT).show()
    }

    private fun createMenu(): LinearLayout = LinearLayout(service).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background = rounded(Color.WHITE, dp(18))
        elevation = dp(8).toFloat()

        addView(label("自动滑屏", 14, Color.DKGRAY), rowParams(bottom = 6))
        val speedHeader = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        speedHeader.addView(label("滑动速率", 12, Color.rgb(95, 103, 117)), LinearLayout.LayoutParams(0, dp(22), 1f))
        rateValueLabel = label("", 12, Color.rgb(73, 79, 91)).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        speedHeader.addView(rateValueLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(22)))
        addView(speedHeader, rowParams(bottom = 2))

        val speedLabelsRow = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL }
        speedLabels = ScrollSpeed.entries.map { option ->
            label("${option.label}速", 12, Color.rgb(127, 133, 143)).apply {
                gravity = Gravity.CENTER
            }.also { speedLabelsRow.addView(it, LinearLayout.LayoutParams(0, dp(24), 1f)) }
        }
        addView(speedLabelsRow, rowParams())

        addView(SeekBar(service).apply {
            max = MAX_RATE_PROGRESS
            progress = (rate * MAX_RATE_PROGRESS).roundToInt()
            contentDescription = "滑动速率，可连续调节，慢速到快速"
            progressTintList = ColorStateList.valueOf(Color.rgb(72, 79, 91))
            thumbTintList = ColorStateList.valueOf(Color.rgb(72, 79, 91))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        rate = progress / MAX_RATE_PROGRESS.toFloat()
                        renderRateValue()
                        renderSpeedLabels()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    saveSelection()
                }
            })
        }, rowParams(bottom = 8))
        renderRateValue()
        renderSpeedLabels()

        val modes = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL }
        ScrollMode.entries.forEach { option ->
            modes.addView(chip(option.label, mode == option) {
                mode = option
                saveSelection()
                expanded = false
                scrollController.start(ScrollConfig.forRate(mode, rate))
                render()
            }, LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                if (option == ScrollMode.STEP) marginEnd = dp(6)
            })
        }
        addView(modes, rowParams())
    }

    private fun createDot(): TextView = AccessibleDot(service).apply {
        text = "●"
        textSize = 7.5f
        gravity = Gravity.CENTER
        setTextColor(Color.argb(150, 70, 70, 70))
        background = null
        alpha = FLOATING_ALPHA
        contentDescription = "自动滑屏控制；点击打开菜单或停止，长按拖动"
        setOnClickListener { onDotClicked() }
        setOnTouchListener(::handleDotTouch)
    }

    private fun handleDotTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                startX = params.x
                startY = params.y
                dragging = false
                longPressTriggered = false
                view.postDelayed(longPress, LONG_PRESS_MS)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (!dragging && (abs(dx) > touchSlop() || abs(dy) > touchSlop())) {
                    view.removeCallbacks(longPress)
                }
                if (dragging) {
                    params.x = (startX - dx.toInt()).coerceAtLeast(0)
                    params.y = (startY + dy.toInt()).coerceAtLeast(dp(24))
                    if (attached) windowManager.updateViewLayout(root, params)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.removeCallbacks(longPress)
                if (dragging) {
                    preferences.edit().putInt(
                        KEY_Y_DP,
                        (params.y / service.resources.displayMetrics.density).toInt(),
                    ).apply()
                } else if (!longPressTriggered && event.actionMasked == MotionEvent.ACTION_UP) {
                    view.performClick()
                }
                return true
            }
        }
        return true
    }

    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false
    private var longPressTriggered = false
    private val longPress = Runnable {
        dragging = true
        longPressTriggered = true
    }

    private class AccessibleDot(context: Context) : TextView(context) {
        override fun performClick(): Boolean {
            super.performClick()
            return true
        }
    }

    private fun onDotClicked() {
        if (scrollController.isRunning) {
            scrollController.stop()
            expanded = false
            render()
        } else {
            expanded = !expanded
            render()
        }
    }

    private fun render() {
        root.removeAllViews()
        if (expanded && !scrollController.isRunning) {
            renderMenu()
            root.addView(menu, LinearLayout.LayoutParams(dp(214), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(8)
            })
        }
        renderDot()
        root.addView(dot, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.requestLayout()
    }

    private fun renderMenu() {
        menu.visibility = View.VISIBLE
        (menu.getChildAt(4) as LinearLayout).let { row ->
            ScrollMode.entries.forEachIndexed { index, option ->
                (row.getChildAt(index) as TextView).apply {
                    isSelected = option == mode
                    styleChip(this, option.label, option == mode)
                }
            }
        }
        renderSpeedLabels()
    }

    private fun renderDot() {
        dot.text = if (scrollController.isRunning) "■" else "●"
        dot.contentDescription = if (scrollController.isRunning) "停止自动滑屏" else "打开自动滑屏菜单"
        dot.setTextColor(Color.argb(150, 70, 70, 70))
        dot.background = null
        if (root.childCount > 0) {
            val visibleMenu = expanded && !scrollController.isRunning
            if (visibleMenu && root.getChildAt(0) !== menu) render()
            else if (!visibleMenu && root.childCount > 1) render()
        }
    }

    private fun chip(text: String, selected: Boolean, action: () -> Unit) =
        TextView(service).apply {
            gravity = Gravity.CENTER
            textSize = 13f
            setOnClickListener { action() }
            styleChip(this, text, selected)
        }

    private fun renderSpeedLabels() {
        val closestSpeed = ScrollSpeed.entries.minByOrNull { abs(it.rate - rate) }
        ScrollSpeed.entries.forEachIndexed { index, option ->
            speedLabels.getOrNull(index)?.apply {
                setTextColor(if (option == closestSpeed) Color.rgb(49, 57, 71) else Color.rgb(127, 133, 143))
                isSelected = option == closestSpeed
            }
        }
    }

    private fun renderRateValue() {
        rateValueLabel.text = String.format(Locale.getDefault(), "%.1f%%", rate * 100f)
    }

    private fun styleChip(view: TextView, text: String, selected: Boolean) {
        view.text = text
        view.setTextColor(if (selected) Color.WHITE else Color.rgb(73, 79, 91))
        view.background = rounded(if (selected) Color.rgb(49, 94, 251) else Color.rgb(242, 244, 248), dp(12))
    }

    private fun label(text: String, size: Int, color: Int) = TextView(service).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(color)
    }

    private fun rowParams(bottom: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { bottomMargin = dp(bottom) }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun saveSelection() {
        preferences.edit().putString(KEY_MODE, mode.name).putFloat(KEY_RATE, rate).apply()
    }

    private fun loadRate(): Float {
        if (preferences.contains(KEY_RATE)) return preferences.getFloat(KEY_RATE, DEFAULT_RATE).coerceIn(0f, 1f)
        return enumValueOrDefault(preferences.getString(KEY_SPEED, null), ScrollSpeed.MEDIUM).rate
    }

    private fun dp(value: Int) = (value * service.resources.displayMetrics.density).toInt()
    private fun touchSlop() = dp(8).toFloat()

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    companion object {
        private const val KEY_MODE = "mode"
        private const val KEY_SPEED = "speed"
        private const val KEY_RATE = "rate"
        private const val KEY_Y_DP = "overlay_y_dp"
        private const val LONG_PRESS_MS = 450L
        private const val FLOATING_ALPHA = 0.72f
        private const val DEFAULT_RATE = 0.5f
        private const val MAX_RATE_PROGRESS = 1_000
    }
}
