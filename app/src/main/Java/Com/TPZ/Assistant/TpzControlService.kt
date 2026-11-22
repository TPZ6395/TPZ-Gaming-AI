package com.tpz.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class TpzControlService : AccessibilityService() {
    companion object {
        var instance: TpzControlService? = null
        var inBigFight: Boolean = false

        fun isVipOrOwner(): Boolean {
            val prefs = instance?.getSharedPreferences("TPZ_CONFIG", Context.MODE_PRIVATE) ?: return false
            return prefs.getBoolean("IS_OWNER", false) ||
                prefs.getBoolean("IS_VIP", false) ||
                System.currentTimeMillis() < prefs.getLong("TRIAL_END", 0L)
        }

        fun tap(x: Float, y: Float) {
            if (inBigFight && !isVipOrOwner()) return
            val p = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(p, 0, 75)
            instance?.dispatchGesture(
                GestureDescription.Builder().addStroke(stroke).build(),
                null, null
            )
        }

        fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long = 320) {
            if (inBigFight && !isVipOrOwner()) return
            val p = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
            val stroke = GestureDescription.StrokeDescription(p, 0, ms)
            instance?.dispatchGesture(
                GestureDescription.Builder().addStroke(stroke).build(),
                null, null
            )
        }

        fun comboAttack(x: Float, y: Float) {
            if (!isVipOrOwner()) return
            repeat(3) { idx ->
                val p = Path().apply { moveTo(x, y) }
                val stroke = GestureDescription.StrokeDescription(p, idx * 140L, 60)
                instance?.dispatchGesture(
                    GestureDescription.Builder().addStroke(stroke).build(),
                    null, null
                )
            }
        }

        fun goHome() = instance?.performGlobalAction(GLOBAL_ACTION_HOME)
        fun goBack() = instance?.performGlobalAction(GLOBAL_ACTION_BACK)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
