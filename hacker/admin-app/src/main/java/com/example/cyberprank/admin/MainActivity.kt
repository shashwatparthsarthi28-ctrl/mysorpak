package com.example.cyberprank.admin

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : Activity() {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var codeInput: EditText
    private lateinit var message: TextView
    private var pairedDeviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        buildUi("Connecting to Firebase…")
        if (auth.currentUser != null) ready() else auth.signInAnonymously()
            .addOnSuccessListener { ready() }
            .addOnFailureListener { buildUi("Firebase sign-in failed: ${it.message}") }
    }

    private fun ready() = buildUi("Enter the pairing code shown in the Friend APK.")

    private fun buildUi(msg: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 36, 28, 28)
            setBackgroundColor(Color.rgb(16, 16, 18))
        }
        root.addView(TextView(this).apply {
            text = "CYBER PRANK ADMIN"; textSize = 26f; setTextColor(Color.RED)
            gravity = Gravity.CENTER; setPadding(0, 12, 0, 18)
        })
        message = TextView(this).apply {
            text = msg; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        }
        root.addView(message)
        codeInput = EditText(this).apply {
            hint = "6-character pairing code"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
            isSingleLine = true
        }
        root.addView(codeInput)
        root.addView(makeButton("PAIR DEVICE") { pair() })
        root.addView(makeButton("ACTIVATE PRANK") { send("ACTIVATE") })
        root.addView(makeButton("STOP PRANK") { send("STOP") })
        root.addView(TextView(this).apply {
            text = "Use only with a consenting test device. Android may block background screen launches."
            textSize = 13f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER; setPadding(0, 20, 0, 0)
        })
        setContentView(root)
    }

    private fun pair() {
        val code = codeInput.text.toString().trim().uppercase()
        if (code.length != 6) { message.text = "Enter a six-character code."; return }
        db.collection("devices").whereEqualTo("pairCode", code).limit(1).get()
            .addOnSuccessListener { result ->
                val doc = result.documents.firstOrNull()
                if (doc == null) {
                    message.text = "Code not found. Open Friend APK and check the code."
                } else {
                    pairedDeviceId = doc.id
                    message.text = "Paired with demo device ${doc.id.take(8)}…"
                }
            }.addOnFailureListener { message.text = "Pairing failed: ${it.message}" }
    }

    private fun send(command: String) {
        val id = pairedDeviceId
        if (id == null) { message.text = "Pair a device first."; return }
        db.collection("devices").document(id).update(
            mapOf("command" to command, "updatedAt" to System.currentTimeMillis())
        ).addOnSuccessListener {
            message.text = "$command request sent. Friend app must be running or opened to receive it."
        }.addOnFailureListener { message.text = "Command failed: ${it.message}" }
    }

    private fun makeButton(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(145, 0, 0))
        setOnClickListener { action() }
    }
}
