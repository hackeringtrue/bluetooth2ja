# Bluetooth Jammer Framework v2.0

A powerful Android Bluetooth penetration testing framework for security researchers and penetration testers. Test Bluetooth device resilience against various attack vectors including connection flooding, pairing spam, and protocol-level attacks.

##  Disclaimer

**FOR EDUCATIONAL AND AUTHORIZED TESTING ONLY**

This tool is designed for:
- Security researchers
- Penetration testers with proper authorization
- Bluetooth security auditing
- Testing device resilience in controlled environments

**Unauthorized jamming of Bluetooth devices may be illegal in your jurisdiction. Use responsibly and only on devices you own or have explicit permission to test.**

---

##  Features & Limitations

###  IMPORTANT: What This App CAN and CANNOT Do

####  What Works (Non-Root):
- **Block NEW connection attempts** - Prevents devices from connecting initially
- **Interfere with pairing process** - Disrupts device pairing
- **Connection spam attacks** - Makes discovery difficult
- **DoS during connection establishment** - Overwhelms connection logic

####  What DOESN'T Work Without Root:
- **Jamming active music playback** - Android OS prevents this
- **Disconnecting already-connected headphones** - Requires HCI access
- **Disrupting established audio streams** - OS-level protection
- **Forcing disconnection of paired devices** - Need root privileges

**BOTTOM LINE**: Non-root modes only work on devices trying to connect, NOT devices already playing audio.

####  What Works With Root:
- **Force disconnect active connections** - Via HCI deauth commands
- **Disrupt audio streams in real-time** - Bluetooth stack manipulation
- **Inject malformed packets** - Protocol-level attacks
- **Bypass Android security restrictions** - Direct hardware access

---

##  Requirements

### Non-Root Mode
- **Android Version**: 10 (API 29) or higher
- **Permissions**: 
  - Bluetooth
  - Bluetooth Admin
  - Location (for BLE scanning)
- **Limitations**: Only affects NEW connections

### Root Mode (For Active Connection Jamming)
- **All non-root requirements**
- **Root access** (Magisk recommended)
- **hcitool binary** (usually included in ROM)
- **SELinux permissive** (may be required on some devices)

---

##  Installation

### Method 1: Install Pre-built APK
1. Download the latest APK from releases
2. Enable "Install from Unknown Sources" on your device
3. Install the APK
4. Grant required permissions
5. **(Optional)** Root your device with Magisk for active connection attacks

### Method 2: Build from Source
\\\gradle
# Clone the repository
git clone https://github.com/yourusername/bluetooth2jam.git
cd bluetooth2jam

# Build with Gradle
.\gradlew.bat assembleDebug

# APK location: app/build/outputs/apk/debug/app-debug.apk
\\\

---

##  Usage

### Basic Attack Flow
1. **Scan for Devices**: Tap "SEARCH DEVICES" to discover nearby Bluetooth devices
2. **Select Target**: Choose a device from the list
3. **Choose Attack Mode**: 
   - **Non-Root Modes** (6 modes): Work on NEW connections only
   - **Root Modes** (2 modes): Can disrupt ACTIVE connections
4. **Configure Threads**: Set number of concurrent attack threads (default: 8)
5. **Start Attack**: Tap "START ATTACK" button
6. **Monitor Logs**: View real-time attack progress in the log window
7. **Stop Attack**: Tap "STOP ATTACK" when finished

### Attack Mode Details

#### Non-Root Modes (Affect NEW Connections Only)

##### Multi-Vector (Recommended)
Combines multiple attack strategies:
- SDP service discovery spam (every 5th attempt)
- Pairing requests (every 10th attempt)  
- Rapid RFCOMM connections on channels 1-30
- L2CAP channel flooding
- 990-byte junk data transmission

** Works for**: Blocking NEW connection attempts  
** Won't affect**: Already-connected devices playing audio

##### Connection Bombardment
- Creates rapid connect/disconnect cycles
- Uses reflection to access hidden RFCOMM sockets
- Floods with 990-byte packets
- Attempts 30 channels per cycle

** Works for**: Overwhelming connection logic  
** Won't affect**: Established connections

##### RFCOMM Flood
- Scans RFCOMM channels 1-30
- Maintains persistent connections
- Continuous data flooding (10ms intervals)
- Keeps sockets alive until stopped

** Works for**: Channel exhaustion on NEW connections  
** Won't affect**: Active audio streams

##### L2CAP Attack
- Targets L2CAP protocol layer
- Tests PSM (Protocol/Service Multiplexer) 1-50
- Rapid channel creation and destruction

** Works for**: Protocol-level resilience testing  
** Won't affect**: Established audio connections

##### Pairing Spam
- Continuous pairing requests
- SDP UUID fetching
- Bond state manipulation

** Works for**: Disrupting pairing process  
** Won't affect**: Already-paired devices

##### SDP Flooding
- Service Discovery Protocol spam
- UUID fetching at 50ms intervals
- Overwhelming service discovery

** Works for**: SDP stack testing during discovery  
** Won't affect**: Connected devices

---

#### Root-Only Modes (Can Disrupt ACTIVE Connections)

##### Root Deauth Attack  (Root Required)
**THIS IS WHAT YOU NEED FOR ACTIVE AUDIO JAMMING**

- Force disconnect via \hcitool dc\
- HCI reset commands
- Bluetooth stack toggling
- ACL connection reset

** WILL disrupt**: Active music playback  
** WILL disconnect**: Already-connected headphones  
** WILL drop**: Established audio streams

**Requirements**:
- Rooted device (Magisk)
- \hcitool\ binary
- Root permission granted to app

##### Root Stack Poison  (Root Required)
- Injects malformed L2CAP packets
- Invalid channel IDs
- Oversized packet injection
- HCI command manipulation

** WILL crash**: Bluetooth stack (requires reboot)  
** WILL disrupt**: All active connections  
** WARNING**: May cause device instability

---

##  How to Root Your Device (For Active Jamming)

### Recommended Method: Magisk

1. **Unlock Bootloader**:
   \\\ash
   # Enable OEM Unlocking in Developer Options
   # Boot to fastboot: Power + Volume Down
   adb reboot bootloader
   fastboot oem unlock  # or: fastboot flashing unlock
   \\\

2. **Install Magisk**:
   - Download stock boot.img for your device
   - Install Magisk Manager APK
   - Patch boot.img in Magisk app
   - Flash patched boot: \astboot flash boot magisk_patched.img\

3. **Verify Root**:
   - Open Bluetooth Jammer app
   - Check logs: Should show "Root Status:  AVAILABLE"
   - Root attack modes should be enabled (not grayed out)

** Rooting Warnings**:
- Voids warranty
- May brick device if done incorrectly
- Banking apps may not work (Magisk Hide can bypass)
- Samsung devices trip Knox permanently

---

##  Technical Details

### Why Non-Root Modes Don't Work on Active Connections

Android OS protects established Bluetooth connections through:
1. **ACL (Active Connection Link)** - OS-managed, app-inaccessible
2. **A2DP Audio Stream** - Kernel-level audio routing
3. **Security restrictions** - Apps can't access HCI layer
4. **Socket isolation** - Each connection has protected socket

**The attacks target**:
- RFCOMM connection establishment (pre-connection phase)
- L2CAP channel setup (before audio starts)
- SDP service discovery (before pairing)

**The attacks DON'T reach**:
- Established ACL links
- Active A2DP streams
- Kernel-level audio routing

### Why Root Modes DO Work

Root access allows:
1. **HCI command injection** - Direct Bluetooth hardware control
2. **Bluetooth stack manipulation** - Service enable/disable
3. **Packet injection** - Malformed HCI commands
4. **Connection termination** - Force disconnect via \hcitool dc\

---

##  Known Issues & Troubleshooting

### "Attack doesn't stop my headphones' music"
**Cause**: You're using non-root modes on an already-connected device  
**Solution**: 
1. Root your device
2. Select "Root Deauth Attack" mode
3. Grant root permission when prompted
4. Start attack - music should disconnect

### "Root modes are grayed out"
**Cause**: Device is not rooted or root not detected  
**Solution**:
1. Verify root: Install Root Checker app
2. Ensure Magisk is properly installed
3. Grant root to Bluetooth Jammer when prompted
4. Check logs for "Root Status:  AVAILABLE"

### "Root modes don't work even when rooted"
**Cause**: \hcitool\ binary not found on device  
**Solution**:
1. Check if \hcitool\ exists: \db shell which hcitool\
2. Some custom ROMs don't include it
3. Install manually or use a ROM that includes it

### "Device becomes unstable after Root Stack Poison"
**Cause**: Malformed packets crash Bluetooth stack  
**Solution**: 
- Reboot device to recover
- Use "Root Deauth Attack" instead (safer)
- Stack Poison is intentionally destructive

---

##  Credits

- **ChatGPT-4o**: AI assistance and code optimization
- **Material Design 3**: Google's design system
- **Android Bluetooth API**: Core functionality
- **Magisk**: Root solution for testing root modes

---

##  License

MIT License - See LICENSE file for details

---

**Remember: Non-root modes only block NEW connections. For active audio jamming, you MUST root your device and use Root Deauth Attack mode.**
