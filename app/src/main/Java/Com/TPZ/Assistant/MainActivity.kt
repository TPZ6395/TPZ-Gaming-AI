package com.tpz.assistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, TpzOverlayService::class.java).apply {
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("DATA", result.data)
            }
            startForegroundService(serviceIntent)
            Toast.makeText(this, "TPZ is active! Say 'TPZ, time to game'", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val prefs = getSharedPreferences("TPZ_CONFIG", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        val title = TextView(this).apply {
            text = "TPZ // GAMING AI MANAGER"
            textSize = 22f
        }

        val apiKeyInput = EditText(this).apply {
            hint = "Enter OpenRouter / Open-Source API Key"
            setText(prefs.getString("AI_KEY", ""))
        }

        val customPromptInput = EditText(this).apply {
            hint = "TPZ Personality & Game Rules"
            setText(prefs.getString("SYSTEM_PROMPT",
                "You are TPZ, an elite gaming co-pilot. Analyze the live game screen and give crisp, 1-to-2 sentence tactical tips. Never spoil story cutscenes."))
        }

        val activateBtn = Button(this).apply {
            text = "ACTIVATE TPZ OVERLAY & VOICE"
            setOnClickListener {
                prefs.edit()
                    .putString("AI_KEY", apiKeyInput.text.toString())
                    .putString("SYSTEM_PROMPT", customPromptInput.text.toString())
                    .apply()

                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
                }
            }
        }

        layout.addView(title)
        layout.addView(apiKeyInput)
        layout.addView(customPromptInput)
        layout.addView(activateBtn)
        setContentView(layout)
    }
}
