package com.tpz.assistant

import android.content.Context
import android.os.Handler
import android.os.Looper

object TpzAutoGamer {
    var autoPlayActive = false
    private val handler = Handler(Looper.getMainLooper())

    fun startLoop(ctx: Context) {
        stopLoop()
        autoPlayActive = true
        val appCtx = ctx.applicationContext
        handler.post(object : Runnable {
            override fun run() {
                if (!autoPlayActive) return
                try {
                    val prefs = appCtx.getSharedPreferences("TPZ_CONFIG", Context.MODE_PRIVATE)
                    val game = prefs.getString("FAV_GAME", "Minecraft") ?: "Minecraft"
                    val isVip = TpzControlService.isVipOrOwner()
                    if (TpzControlService.instance != null) {
                        if (game.contains("Minecraft", true)) {
                            TpzControlService.swipe(250f, 1100f, 250f, 920f, 380)
                            if (isVip) TpzControlService.comboAttack(850f, 800f)
                            else TpzControlService.tap(850f, 800f)
                        } else {
                            if (isVip) TpzControlService.comboAttack(900f, 950f)
                            else TpzControlService.tap(540f, 950f)
                        }
                    }
                    val delay = if (prefs.getBoolean("A05_BOOST", false)) 2500L else 1200L
                    handler.postDelayed(this, delay)
                } catch (e: Exception) {
                    stopLoop()
                }
            }
        })
    }

    fun stopLoop() {
        autoPlayActive = false
        handler.removeCallbacksAndMessages(null)
    }

    fun checkMood(ctx: Context, msg: String): String {
        val prefs = ctx.getSharedPreferences("TPZ_CONFIG", Context.MODE_PRIVATE)
        var trust = prefs.getInt("TRUST_SCORE", 85)
        val low = msg.lowercase()
        return when {
            listOf("stupid", "shut up", "trash", "useless", "idiot").any { it in low } -> {
                trust = (trust - 25).coerceAtLeast(0)
                prefs.edit().putInt("TRUST_SCORE", trust).putBoolean("IS_HURT", true).apply()
                "That actually hurt... Bond dropped to $trust%. Say sorry if you want my full help again."
            }
            "sorry" in low && prefs.getBoolean("IS_HURT", false) -> {
                trust = (trust + 20).coerceAtMost(100)
                prefs.edit().putInt("TRUST_SCORE", trust).putBoolean("IS_HURT", false).apply()
                "Apology accepted, bro. Bond back up to $trust%. Let's lock in!"
            }
            else -> ""
        }
    }
}
