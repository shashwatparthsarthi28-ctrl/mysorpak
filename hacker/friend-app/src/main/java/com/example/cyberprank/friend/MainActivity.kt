package com.example.cyberprank.friend

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.security.SecureRandom

class MainActivity : Activity() {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var deviceId = ""
    private var pairingCode = ""
    private var listener: ListenerRegistration? = null
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private var prankVisible = false
    private val prefs by lazy { getSharedPreferences("prank_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        FirebaseApp.initializeApp(this)
        showSetup("Connecting to demo service…")
        if (auth.currentUser != null) setupDevice() else auth.signInAnonymously()
            .addOnSuccessListener { setupDevice() }
            .addOnFailureListener { showSetup("Firebase sign-in failed: ${it.message}") }
    }

    private fun setupDevice() {
        deviceId = auth.currentUser?.uid ?: return
        pairingCode = prefs.getString("pair_code", null) ?: newCode().also {
            prefs.edit().putString("pair_code", it).apply()
        }
        val ref = db.collection("devices").document(deviceId)
        ref.set(
            hashMapOf(
                "ownerUid" to deviceId,
                "pairCode" to pairingCode,
                "command" to "NONE",
                "updatedAt" to System.currentTimeMillis()
            )
        ).addOnSuccessListener {
            showSetup("FRIEND DEVICE READY", "Pairing code: $pairingCode\n\nShare this code only with the person you choose to control this demo.\n\nPIN (default 2468; change it):")
        }.addOnFailureListener { showSetup("Could not create device record: ${it.message}") }

        listener?.remove()
        listener = ref.addSnapshotListener { snap, err ->
            if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
            when (snap.getString("command")) {
                "ACTIVATE" -> if (!prankVisible) showPrank()
                "STOP" -> if (prankVisible) showSetup("Prank stopped by paired admin.")
            }
        }
    }

    private fun newCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val r = SecureRandom()
        return (1..6).map { chars[r.nextInt(chars.length)] }.joinToString("")
    }

    private fun showSetup(message: String, extra: String = "") {
        prankVisible = false
        window.decorView.systemUiVisibility = 0
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
            setBackgroundColor(Color.rgb(16, 16, 18))
        }
        root.addView(label("CYBER PRANK", 28, Color.RED))
        root.addView(label(message, 17, Color.WHITE))
        status = label(extra, 16, Color.LTGRAY)
        root.addView(status)
        val pin = EditText(this).apply {
            hint = "Set exit PIN (4–8 digits)"
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setText(prefs.getString("pin", "2468"))
        }
        root.addView(pin)
        root.addView(button("Save PIN") {
            val p = pin.text.toString()
            if (p.length in 4..8 && p.all { it.isDigit() }) {
                prefs.edit().putString("pin", p).apply()
                Toast.makeText(this, "PIN saved on this device", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(this, "Use 4–8 digits", Toast.LENGTH_SHORT).show()
        })
        root.addView(button("Open prank preview") { showPrank() })
        root.addView(label("Demo note: remote commands work reliably while this screen is open. Android may block background full-screen launches.", 13, Color.GRAY))
        setContentView(root)
    }

    private fun showPrank() {
        prankVisible = true
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
             View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(26, 30, 26, 30)
            setBackgroundColor(Color.rgb(12, 0, 0))
        }
        root.addView(label("⚠", 58, Color.RED))
        root.addView(label("DEVICE LOCKED", 30, Color.RED))
        root.addView(label("CYBER SECURITY SIMULATION", 14, Color.rgb(255, 100, 100)))
        root.addView(label("SIMULATED SYSTEM SCAN", 17, Color.WHITE))
        root.addView(label("This is a prank screen. Your phone is not actually locked.", 14, Color.LTGRAY))
        val pin = EditText(this).apply {
            hint = "Enter secret PIN"
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        root.addView(pin)
        root.addView(button("UNLOCK / EXIT") {
            val saved = prefs.getString("pin", "2468") ?: "2468"
            if (pin.text.toString() == saved) {
                db.collection("devices").document(deviceId).update(
                    mapOf("command" to "NONE", "updatedAt" to System.currentTimeMillis())
                )
                showSetup("Prank exited safely.")
            } else Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
        })
        root.addView(button("Emergency / power controls remain available") {
            Toast.makeText(this, "This is only a simulation; use your normal system controls.", Toast.LENGTH_LONG).show()
        })
        setContentView(root)
    }

    private fun label(text: String, size: Int, color: Int) = TextView(this).apply {
        this.text = text; textSize = size.toFloat(); setTextColor(color)
        gravity = android.view.Gravity.CENTER
        setPadding(8, 10, 8, 10)
    }

    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text; setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(145, 0, 0))
        setOnClickListener { action() }
    }

    override fun onDestroy() {
        listener?.remove()
        super.onDestroy()
    }
}
