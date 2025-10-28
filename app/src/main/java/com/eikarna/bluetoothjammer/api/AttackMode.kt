package api

enum class AttackMode(val displayName: String, val description: String, val requiresRoot: Boolean = false) {
    CONNECTION_BOMBARDMENT(
        "Connection Bombardment",
        "Rapidly connect/disconnect to exhaust resources"
    ),
    PAIRING_SPAM(
        "Pairing Spam",
        "Flood with pairing requests"
    ),
    SDP_FLOODING(
        "SDP Flooding",
        "Service Discovery Protocol spam"
    ),
    RFCOMM_FLOOD(
        "RFCOMM Flood",
        "Channel scanning and data flooding"
    ),
    L2CAP_ATTACK(
        "L2CAP Attack",
        "L2CAP channel flooding"
    ),
    MULTI_VECTOR(
        "Multi-Vector",
        "Combined attack using all methods"
    ),
    ROOT_DEAUTH(
        "Root: Deauth Attack",
        "Force disconnect using root access (REQUIRES ROOT)",
        requiresRoot = true
    ),
    ROOT_STACK_POISON(
        "Root: Stack Poisoning",
        "Inject malformed packets (REQUIRES ROOT)",
        requiresRoot = true
    )
}
