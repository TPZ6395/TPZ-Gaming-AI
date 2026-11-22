package com.tpz.assistant

import android.content.Context
import android.graphics.*
import android.view.View
import androidx.core.content.res.ResourcesCompat

class TpzSpinnerView(context: Context) : View(context) {
    private var angle = 0f
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2962FF")
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
    }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = try {
            ResourcesCompat.getFont(context, R.font.gamer_font)
        } catch (e: Exception) {
            Typeface.DEFAULT_BOLD
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = (minOf(width, height) / 2f) - 16f
        canvas.drawArc(cx - r, cy - r, cx + r, cy + r, angle, 270f, false, ring)
        canvas.save()
        canvas.rotate(-angle * 0.5f, cx, cy)
        canvas.drawText("TPZ", cx, cy + 12f, txt)
        canvas.restore()
        angle = (angle + 8f) % 360f
        postInvalidateOnAnimation()
    }
}
