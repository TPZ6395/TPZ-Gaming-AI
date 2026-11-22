package com.tpz.assistant

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {
    private lateinit var pm: MediaProjectionManager
    private val capLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            val i = Intent(this, TpzOverlayService::class.java).apply {
                putExtra("RESULT_CODE", res.resultCode)
                putExtra("DATA", res.data)
            }
            startForegroundService(i)
            Toast.makeText(this, "TPZ v3.0 HUD Online!", Toast.LENGTH_LONG).show()
        }
    }

    private fun makeVipCode(gamerTag: String): String {
        val clean = gamerTag.trim().uppercase().ifEmpty { "PLAYER" }
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("TPZ_SECRET_6395_ZW:$clean".toByteArray())
        val hex = bytes.joinToString("") { "%02X".format(it) }
        return "TPZ-${hex.substring(0, 4)}-${hex.substring(4, 8)}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            pm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            }
            val prefs = getSharedPreferences("TPZ_CONFIG", Context.MODE_PRIVATE)
            val gFont = try { ResourcesCompat.getFont(this, R.font.gamer_font) } catch (e: Exception) { Typeface.MONOSPACE }
            val scroll = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#060B19")) }
            val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(44, 56, 44, 56) }
            fun styleBox(v: TextView, hex: String = "#2962FF") {
                v.typeface = gFont; v.setTextColor(Color.WHITE); v.setHintTextColor(Color.parseColor("#7986CB"))
                v.background = GradientDrawable().apply { setColor(Color.parseColor("#0D162E")); setStroke(3, Color.parseColor(hex)); cornerRadius = 24f }
                v.setPadding(32, 24, 32, 24)
            }
            val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            topRow.addView(TpzSpinnerView(this), LinearLayout.LayoutParams(130, 130))
            topRow.addView(TextView(this).apply { text = "TPZ v3.0 // CO-PILOT"; textSize = 19f; typeface = gFont; setTextColor(Color.parseColor("#00E5FF")) })
            root.addView(topRow)

            val isOwner = prefs.getBoolean("IS_OWNER", false)
            val isVip = isOwner || prefs.getBoolean("IS_VIP", false) || System.currentTimeMillis() < prefs.getLong("TRIAL_END", 0L)
            val trust = prefs.getInt("TRUST_SCORE", 85)
            val hurt = prefs.getBoolean("IS_HURT", false)
            root.addView(TextView(this).apply {
                text = (if (isOwner) "OWNER GOD-MODE" else if (isVip) "VIP PRO" else "FREE TIER") +
                    " | BOND: $trust%" + (if (hurt) " (HURT - SAY SORRY)" else "")
                textSize = 13f; typeface = gFont; setTextColor(Color.parseColor(if (hurt) "#FF5252" else "#00E676")); setPadding(0, 16, 0, 20)
            })
            val nameIn = EditText(this).apply { hint = "Gamer Tag"; setText(prefs.getString("USER_NAME", "")); styleBox(this) }
            val colorIn = EditText(this).apply { hint = "Favorite Color"; setText(prefs.getString("FAV_COLOR", "Royal Blue")); styleBox(this) }
            val gameIn = EditText(this).apply { hint = "Game Mode (Minecraft / FC / CODM)"; setText(prefs.getString("FAV_GAME", "Minecraft")); styleBox(this) }
            val memIn = EditText(this).apply { hint = "TPZ Long-Term Memory (Friends/Betrayals)"; setText(prefs.getString("MEMORY_NOTES", "")); styleBox(this, "#00B0FF") }
            val keyIn = EditText(this).apply { hint = "Google AI Studio Key (AQ... or AIza...)"; setText(prefs.getString("AI_KEY", "")); styleBox(this, "#00E5FF") }
            val codeIn = EditText(this).apply { hint = "VIP Code or Owner Passcode"; styleBox(this, "#7C4DFF") }

            val redeemBtn = Button(this).apply {
                text = "UNLOCK CODE / OWNER ACCESS"; typeface = gFont; setBackgroundColor(Color.parseColor("#2962FF")); setTextColor(Color.WHITE)
                setOnClickListener {
                    val c = codeIn.text.toString().trim().uppercase()
                    val tag = nameIn.text.toString().trim()
                    when {
                        c == "TPZ-OWNER-6395" -> { prefs.edit().putBoolean("IS_OWNER", true).putBoolean("IS_VIP", true).apply(); Toast.makeText(this@MainActivity, "OWNER GOD-MODE UNLOCKED!", Toast.LENGTH_LONG).show(); recreate() }
                        c == "TPZ-GIFT24" -> {
                            if (prefs.getBoolean("USED_FREE_GIFT", false)) Toast.makeText(this@MainActivity, "24h VIP Gift already used!", Toast.LENGTH_LONG).show()
                            else { prefs.edit().putBoolean("USED_FREE_GIFT", true).putLong("TRIAL_END", System.currentTimeMillis() + 86400000L).apply(); Toast.makeText(this@MainActivity, "24h VIP Pass Activated!", Toast.LENGTH_LONG).show(); recreate() }
                        }
                        tag.isNotEmpty() && c == makeVipCode(tag) -> { prefs.edit().putBoolean("IS_VIP", true).apply(); Toast.makeText(this@MainActivity, "VIP PRO UNLOCKED FOR $tag!", Toast.LENGTH_LONG).show(); recreate() }
                        else -> Toast.makeText(this@MainActivity, "Invalid Code or Gamer Tag!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val buyVipBtn = Button(this).apply {
                text = "BUY VIP (EMAIL OWNER FOR CODE)"; typeface = gFont; setBackgroundColor(Color.parseColor("#00C853")); setTextColor(Color.BLACK)
                setOnClickListener {
                    val tag = nameIn.text.toString().trim().ifEmpty { "MyGamerTag" }
                    val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:tpzish1234567890@gmail.com").apply {
                        putExtra(Intent.EXTRA_SUBJECT, "TPZ VIP Code Request - $tag")
                        putExtra(Intent.EXTRA_TEXT, "Hi Owner! I want to buy TPZ VIP Pro (EcoCash / InnBucks). My exact Gamer Tag is: $tag")
                    }
                    startActivity(Intent.createChooser(i, "Send VIP Request")
                }
            }

            val autoPlaySw = Switch(this).apply {
                text = "Eyes + Hands Auto-Play For Me"; typeface = gFont; setTextColor(Color.WHITE); isChecked = TpzAutoGamer.autoPlayActive
                setOnCheckedChangeListener { _, on -> if (on) TpzAutoGamer.startLoop(this@MainActivity) else TpzAutoGamer.stopLoop() }
            }
            val afkSw = Switch(this).apply { text = "AFK Sentry & Full-Volume Alarm"; typeface = gFont; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("AFK_WATCH", false) }
            val boostSw = Switch(this).apply {
                text = "Low-RAM Speed Mode (Optional)"; typeface = gFont; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("A05_BOOST", false)
                setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean("A05_BOOST", on).apply() }
            }

            val launchBtn = Button(this).apply {
                text = "LAUNCH TPZ OVERLAY & VOICE"; typeface = gFont; setBackgroundColor(Color.parseColor("#00B0FF")); setTextColor(Color.BLACK)
                setOnClickListener {
                    val uName = nameIn.text.toString().trim().ifEmpty { "Player" }
                    val uCol = colorIn.text.toString().trim().ifEmpty { "Royal Blue" }
                    val uGame = gameIn.text.toString().trim().ifEmpty { "Minecraft" }
                    val uMem = memIn.text.toString().trim()
                    val sysPrompt = "You are TPZ, an ultra-smart, kind, generous futuristic gaming AI for $uName. Favorite color: $uCol. Game: $uGame. Memory notes: $uMem. Trust: $trust/100. Remember preferences (like $uCol banners). Analyze people for betrayal/traps. Ask about their day, check if AFK, and if Free tier and gift unused, offer code TPZ-GIFT24 for 1-day VIP."
                    prefs.edit().putString("USER_NAME", uName).putString("FAV_COLOR", uCol).putString("FAV_GAME", uGame).putString("MEMORY_NOTES", uMem).putString("AI_KEY", keyIn.text.toString().trim()).putString("SYSTEM_PROMPT", sysPrompt).putBoolean("AFK_WATCH", afkSw.isChecked).putBoolean("A05_BOOST", boostSw.isChecked).apply()
                    if (afkSw.isChecked) { val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager; am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0) }
                    if (!Settings.canDrawOverlays(this@MainActivity)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    else capLauncher.launch(pm.createScreenCaptureIntent())
                }
            }

            listOf(nameIn, colorIn, gameIn, memIn, keyIn, codeIn, redeemBtn, buyVipBtn, autoPlaySw, afkSw, boostSw, launchBtn).forEach {
                root.addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 12, 0, 12) })
            }

            if (isOwner) {
                val buyerIn = EditText(this).apply { hint = "Buyer's Exact Gamer Tag"; styleBox(this, "#00E676") }
                val genOut = TextView(this).apply { text = "OWNER VIP GENERATOR READY\n1-Day Gift Code: TPZ-GIFT24"; textSize = 13f; styleBox(this, "#00E676") }
                val genBtn = Button(this).apply {
                    text = "GENERATE UNGUESSABLE VIP CODE"; typeface = gFont; setBackgroundColor(Color.parseColor("#00E676")); setTextColor(Color.BLACK)
                    setOnClickListener {
                        val b = buyerIn.text.toString().trim()
                        if (b.isNotEmpty()) genOut.text = "BUYER: $b\nUNIQUE VIP CODE: ${makeVipCode(b)}\n(Locked to $b's Gamer Tag only!)"
                    }
                }
                listOf(buyerIn, genBtn, genOut).forEach {
                    root.addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 10, 0, 10) })
                }
            }
            scroll.addView(root); setContentView(scroll)
        } catch (e: Exception) {
            setContentView(TextView(this).apply { text = "TPZ Shield: ${e.message}"; setTextColor(Color.WHITE); setBackgroundColor(Color.BLACK); setPadding(32, 32, 32, 32) })
        }
    }
}

