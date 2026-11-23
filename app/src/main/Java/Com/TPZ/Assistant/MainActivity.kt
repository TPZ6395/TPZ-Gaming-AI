package com.tpz.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import java.security.MessageDigest
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var pm: MediaProjectionManager
    private var tts: TextToSpeech? = null
    private val quietHandler = Handler(Looper.getMainLooper())
    private val quietQs = listOf(
        "Hey, while it's quiet—what is your favorite color so I can customize your builds?",
        "Random question while we chill: what sport do you love watching or playing?",
        "Quick one—what is your favorite TV show or anime right now?",
        "Just curious so I know you better: what music gets you hyped for gaming?"
    )
    private val capLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            val i = Intent(this, TpzOverlayService::class.java).apply {
                putExtra("RESULT_CODE", res.resultCode)
                putExtra("DATA", res.data)
            }
            startForegroundService(i)
        }
    }

    private fun makeVipCode(tag: String): String {
        val clean = tag.trim().uppercase().ifEmpty { "PLAYER" }
        val h = MessageDigest.getInstance("SHA-256")
            .digest("TPZ_SECRET_6395_ZW:$clean".toByteArray())
            .joinToString("") { "%02X".format(it) }
        return "TPZ-${h.substring(0, 4)}-${h.substring(4, 8)}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }
        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale.UK }
        showSplashScreen()
    }

    private fun showSplashScreen() {
        val box = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#041230")) }
        box.addView(TpzSpinnerView(this), FrameLayout.LayoutParams(260, 260, Gravity.CENTER))
        setContentView(box)
        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("TPZ_CONFIG", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("SETUP_DONE", false)) showFirstTimeSetup()
            else showMainArena(greet = true)
        }, 10000L)
    }
    private fun showFirstTimeSetup() {
        val prefs = getSharedPreferences("TPZ_CONFIG", Context.MODE_PRIVATE)
        val gFont = try { ResourcesCompat.getFont(this, R.font.gamer_font) } catch (e: Exception) { Typeface.MONOSPACE }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#041230")); setPadding(48, 48, 48, 48)
        }
        val title = TextView(this).apply { text = "WELCOME TO TPZ"; textSize = 22f; typeface = gFont; setTextColor(Color.parseColor("#00E5FF")); gravity = Gravity.CENTER }
        val nameIn = EditText(this).apply { hint = "Name you want Tea Pee Zet to call you"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); typeface = gFont }
        val keyIn = EditText(this).apply { hint = "Google AI Studio API Key"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); typeface = gFont }
        val goBtn = Button(this).apply {
            text = "ENTER TPZ ARENA"; typeface = gFont; setBackgroundColor(Color.parseColor("#2962FF")); setTextColor(Color.WHITE)
            setOnClickListener {
                prefs.edit()
                    .putString("USER_NAME", nameIn.text.toString().trim().ifEmpty { "Player" })
                    .putString("AI_KEY", keyIn.text.toString().trim())
                    .putBoolean("SETUP_DONE", true).apply()
                showMainArena(greet = true)
            }
        }
        listOf(title, nameIn, keyIn, goBtn).forEach { root.addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 18, 0, 18) }) }
        setContentView(root)
    }

    private fun showMainArena(greet: Boolean) {
        val prefs = getSharedPreferences("TPZ_CONFIG", Context.MODE_PRIVATE)
        val gFont = try { ResourcesCompat.getFont(this, R.font.gamer_font) } catch (e: Exception) { Typeface.MONOSPACE }
        val uName = prefs.getString("USER_NAME", "Player") ?: "Player"
        val isOwner = prefs.getBoolean("IS_OWNER", false)

        val scroll = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#041230")) }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(40, 44, 40, 44) }

        val settingsBtn = Button(this).apply {
            text = "SETTINGS"; typeface = gFont; setBackgroundColor(Color.parseColor("#2962FF")); setTextColor(Color.WHITE)
            setOnClickListener { openSettingsDialog() }
        }
        root.addView(settingsBtn)

        val bubbleFrame = FrameLayout(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#0D2B6B")); setStroke(6, Color.parseColor("#00E5FF")) }
        }
        val logoView = ImageView(this).apply { setImageResource(R.drawable.tpz_logo); scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true; outlineProvider = android.view.ViewOutlineProvider.OVAL }
        bubbleFrame.addView(logoView)
        root.addView(bubbleFrame, LinearLayout.LayoutParams(280, 280).apply { setMargins(0, 36, 0, 24) })

        val greetTxt = TextView(this).apply {
            text = "Hello $uName!"
            textSize = 20f; typeface = gFont; setTextColor(Color.parseColor("#00E5FF")); gravity = Gravity.CENTER
        }
        root.addView(greetTxt)
        if (greet) {
            tts?.speak("Hello $uName! Tea Pee Zet is ready.", TextToSpeech.QUEUE_FLUSH, null, "greet")
        }
        quietHandler.removeCallbacksAndMessages(null)
        quietHandler.postDelayed({
            val q = quietQs.random()
            greetTxt.text = q
            tts?.speak(q, TextToSpeech.QUEUE_FLUSH, null, "quiet")
        }, 45000L)
        val launchBtn = Button(this).apply {
            text = "LAUNCH IN-GAME FLOATING BUBBLE"; typeface = gFont; setBackgroundColor(Color.parseColor("#00B0FF")); setTextColor(Color.BLACK)
            setOnClickListener {
                val rules = prefs.getString("AI_RULES", "Be kind, smart, and in quiet moments ask personal questions like favorite color, sport, or TV show to know the user better.")
                val sysPrompt = "You are TPZ (pronounced Tea Pee Zet) talking to $uName. $rules"
                prefs.edit().putString("SYSTEM_PROMPT", sysPrompt).apply()
                if (!Settings.canDrawOverlays(this@MainActivity)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                else capLauncher.launch(pm.createScreenCaptureIntent())
            }
        }
        root.addView(launchBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 24, 0, 20) })

        if (isOwner) {
            val godBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(28, 28, 28, 28)
                background = GradientDrawable().apply { setColor(Color.parseColor("#091D4A")); setStroke(4, Color.parseColor("#00E676")); cornerRadius = 24f }
            }
            godBox.addView(TextView(this).apply { text = "OWNER GOD-MODE DASHBOARD"; typeface = gFont; setTextColor(Color.parseColor("#00E676")); textSize = 15f })
            val rulesIn = EditText(this).apply { hint = "Update AI Personality / Rules"; setText(prefs.getString("AI_RULES", "")); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
            val buyerIn = EditText(this).apply { hint = "Buyer Gamer Tag for VIP Code"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
            val outTxt = TextView(this).apply { text = "1-Day Gift Code: TPZ-GIFT24"; setTextColor(Color.CYAN) }
            val applyBtn = Button(this).apply {
                text = "APPLY AI UPDATE & GENERATE CODE"; typeface = gFont; setBackgroundColor(Color.parseColor("#00E676")); setTextColor(Color.BLACK)
                setOnClickListener {
                    prefs.edit().putString("AI_RULES", rulesIn.text.toString().trim()).apply()
                    val b = buyerIn.text.toString().trim()
                    outTxt.text = if (b.isNotEmpty()) "AI Updated! $b VIP Code: ${makeVipCode(b)}" else "AI Update Applied Live!"
                }
            }
            listOf(rulesIn, buyerIn, applyBtn, outTxt).forEach { godBox.addView(it) }
            root.addView(godBox, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun openSettingsDialog() {
        val prefs = getSharedPreferences("TPZ_CONFIG", Context.MODE_PRIVATE)
        val uName = prefs.getString("USER_NAME", "Player") ?: "Player"
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(36, 24, 36, 24) }
        val codeIn = EditText(this).apply { hint = "Owner Passcode or VIP Code" }
        box.addView(codeIn)

        AlertDialog.Builder(this)
            .setTitle("TPZ SETTINGS")
            .setView(box)
            .setPositiveButton("SUBMIT CODE") { _, _ ->
                val c = codeIn.text.toString().trim().uppercase()
                val loadBox = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#041230")) }
                loadBox.addView(TpzSpinnerView(this), FrameLayout.LayoutParams(220, 220, Gravity.CENTER))
                setContentView(loadBox)
                Handler(Looper.getMainLooper()).postDelayed({
                    when {
                        c == "TPZ-OWNER-6395" -> prefs.edit().putBoolean("IS_OWNER", true).putBoolean("IS_VIP", true).apply()
                        c == "TPZ-GIFT24" && !prefs.getBoolean("USED_FREE_GIFT", false) -> prefs.edit().putBoolean("USED_FREE_GIFT", true).putLong("TRIAL_END", System.currentTimeMillis() + 86400000L).apply()
                        c == makeVipCode(uName) -> prefs.edit().putBoolean("IS_VIP", true).apply()
                    }
                    showMainArena(greet = false)
                }, 1500L)
            }
            .setNeutralButton("BUY VIP") { _, _ ->
                val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:yourname@gmail.com")).apply {
                    putExtra(Intent.EXTRA_SUBJECT, "TPZ VIP Request - $uName")
                }
                startActivity(Intent.createChooser(i, "Buy VIP"))
            }
            .setNegativeButton("FEATURES & AUTO-PLAY") { _, _ ->
                TpzAutoGamer.startLoop(this)
                Toast.makeText(this, "Eyes + Hands Auto-Play Started!", Toast.LENGTH_LONG).show()
            }
            .show()
    }
}

