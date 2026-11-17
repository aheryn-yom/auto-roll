package com.yom.autoroll.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.yom.autoroll.service.AutoScrollAccessibilityService

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var enableButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(32))
            setBackgroundColor(Color.rgb(245, 246, 248))
        }
        content.addView(text("AutoRoll", 30, Color.rgb(28, 35, 50), bold = true), matchWrap(bottom = 6))
        content.addView(text("阅读时的轻量自动滑屏控制器", 16, Color.rgb(95, 103, 117)), matchWrap(bottom = 24))

        val permissionCard = card()
        permissionCard.addView(text("开启无障碍服务", 19, Color.rgb(35, 42, 56), bold = true), matchWrap(bottom = 10))
        status = text("正在检查服务状态…", 15, Color.rgb(83, 91, 105))
        permissionCard.addView(status, matchWrap(bottom = 14))
        permissionCard.addView(text(
            "该权限用于在你主动点击悬浮控制器后模拟上滑手势。应用不会读取屏幕内容，也不会收集或上传数据。启用后，悬浮小白点会显示在其他应用上方。",
            14,
            Color.rgb(95, 103, 117),
        ), matchWrap(bottom = 18))
        enableButton = Button(this).apply {
            text = "前往无障碍设置"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(49, 94, 251), dp(12))
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        permissionCard.addView(enableButton, matchWidth(dp(48)))
        content.addView(permissionCard, matchWrap(bottom = 16))

        val guideCard = card()
        guideCard.addView(text("使用方法", 19, Color.rgb(35, 42, 56), bold = true), matchWrap(bottom = 12))
        listOf(
            "1. 在系统设置中启用“自动滑屏控制”。",
            "2. 切回漫画、小说或网页阅读应用，点击右侧小白点。",
            "3. 选择间歇/连续模式，再选慢、中、快三档之一。",
            "4. 再次点击运行中的小白点即可停止；长按约半秒可拖动位置。",
        ).forEach { guide ->
            guideCard.addView(text(guide, 14, Color.rgb(83, 91, 105)), matchWrap(bottom = 9))
        }
        guideCard.addView(text(
            "连续模式通过短距离微滑动近似连续滚动，实际效果取决于阅读器的滚动与惯性实现。分页阅读器可能无法使用滑动滚动。",
            13,
            Color.rgb(112, 119, 132),
        ), matchWrap(top = 8))
        content.addView(guideCard, matchWrap())

        val page = ScrollView(this).apply {
            addView(content)
            setOnApplyWindowInsetsListener { view, insets ->
                var left: Int
                var top: Int
                var right: Int
                var bottom: Int
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                    left = bars.left
                    top = bars.top
                    right = bars.right
                    bottom = bars.bottom
                } else {
                    @Suppress("DEPRECATION")
                    left = insets.systemWindowInsetLeft
                    @Suppress("DEPRECATION")
                    top = insets.systemWindowInsetTop
                    @Suppress("DEPRECATION")
                    right = insets.systemWindowInsetRight
                    @Suppress("DEPRECATION")
                    bottom = insets.systemWindowInsetBottom
                }
                view.setPadding(left, top, right, bottom)
                insets
            }
        }
        setContentView(page)
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }

    private fun refreshStatus() {
        val enabled = getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                val serviceInfo = info.resolveInfo?.serviceInfo
                serviceInfo != null && ComponentName(serviceInfo.packageName, serviceInfo.name) ==
                    ComponentName(this, AutoScrollAccessibilityService::class.java)
            }
        status.text = if (enabled) {
            "服务已开启。返回阅读应用后，小白点会自动显示。"
        } else {
            "服务尚未开启。点击下方按钮，在系统无障碍服务列表中启用“自动滑屏控制”。"
        }
        enableButton.text = if (enabled) "打开系统无障碍设置" else "前往无障碍设置"
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(Color.WHITE, dp(20))
        elevation = dp(2).toFloat()
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        gravity = Gravity.START
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun matchWrap(bottom: Int = 0, top: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply {
        bottomMargin = dp(bottom)
        topMargin = dp(top)
    }
    private fun matchWidth(height: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        height,
    )
}
